/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import com.ruoyi.scanner.poc.PocData.DruidPoc.DruidCredential;
import com.ruoyi.scanner.poc.PocLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import okhttp3.FormBody;
import okhttp3.Response;

public class DruidCheck {

    public static void run(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        String[] rawPaths = PocLoader.get().druid().paths;
        List<String> paths = new ArrayList<>();
        for (String p : rawPaths) {
            paths.add(cfg.baseApi + p);
        }
        paths.addAll(Arrays.asList(rawPaths));

        ctx.log.log("\u5f00\u59cbDruid\u7efc\u5408\u68c0\u6d4b...", LogConsole.Severity.INFO);
        int idx = 0;
        for (String path : paths) {
            if (!ctx.scanning.get()) return;
            ctx.setProgressMax(idx, paths.size());
            if (path.endsWith("basic.json")) {
                detectUnauth(ctx, http, cfg, path);
            } else {
                detectEndpoint(ctx, http, cfg, path);
                if (path.contains("login.html")) {
                    bruteforce(ctx, http, cfg, path);
                }
            }
            ++idx;
        }
    }

    private static void detectUnauth(ScanContext ctx, HttpService http, ScanConfig cfg, String path) {
        String url = cfg.fullUrl(path);
        try (Response resp = http.get(url, cfg)) {
            if (resp.code() == 200) {
                String text = resp.body() != null ? resp.body().string() : "";
                if (text.contains("JavaClassPath")) {
                    ctx.log.log("[\u4e25\u91cd] Druid\u672a\u6388\u6743\u8bbf\u95ee\u6f0f\u6d1e: " + path + " (\u4fe1\u606f\u6cc4\u9732)", LogConsole.Severity.CRITICAL);
                } else {
                    ctx.log.log(path + " \u4e0d\u5b58\u5728Druid\u672a\u6388\u6743\u8bbf\u95ee", LogConsole.Severity.INFO);
                }
            } else if (resp.code() == 401) {
                ctx.log.log(path + " \u9700\u8981\u8eab\u4efd\u9a8c\u8bc1", LogConsole.Severity.INFO);
            } else {
                ctx.log.log(path + " \u8bbf\u95ee\u5931\u8d25\uff0c\u72b6\u6001\u7801: " + resp.code(), LogConsole.Severity.INFO);
            }
        } catch (Exception e) {
            ctx.log.log(path + " \u68c0\u6d4b\u5f02\u5e38: " + e.getMessage(), LogConsole.Severity.WARNING);
        }
    }

    private static void detectEndpoint(ScanContext ctx, HttpService http, ScanConfig cfg, String path) {
        String url = cfg.fullUrl(path);
        try (Response resp = http.get(url, cfg)) {
            String text = resp.body() != null ? resp.body().string() : "";
            if (resp.code() == 200 && text.toLowerCase().contains("druid")) {
                ctx.log.log(path + " \u5b58\u5728Druid\u76d1\u63a7\u9875\u9762", LogConsole.Severity.CRITICAL);
            } else {
                ctx.log.log(path + " \u6f0f\u6d1e\u53ef\u80fd\u4e0d\u5b58\u5728", LogConsole.Severity.INFO);
            }
        } catch (Exception e) {
            ctx.log.log(path + " \u8bf7\u6c42\u5931\u8d25: " + e.getMessage(), LogConsole.Severity.WARNING);
        }
    }

    private static void bruteforce(ScanContext ctx, HttpService http, ScanConfig cfg, String path) {
        String submitPath = path.replace("login.html", "submitLogin");
        String target = cfg.fullUrl(submitPath);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("Origin", cfg.url);
        headers.put("Referer", cfg.fullUrl(path));

        for (DruidCredential cred : PocLoader.get().druid().credentials) {
            if (!ctx.scanning.get()) return;
            try {
                LinkedHashMap<String, String> form = new LinkedHashMap<>();
                form.put("loginUsername", cred.username);
                form.put("loginPassword", cred.password);
                FormBody.Builder fb = new FormBody.Builder();
                form.forEach(fb::add);
                try (Response resp = http.post(target, cfg, fb.build(), headers)) {
                    String body = (resp.body() != null ? resp.body().string() : "").trim().toLowerCase();
                    if (resp.code() == 200 && "success".equals(body)) {
                        ctx.log.log("[\u4e25\u91cd] Druid\u5f31\u53e3\u4ee4\u7206\u7834\u6210\u529f: " + cred.username + ":" + cred.password + " @ " + submitPath, LogConsole.Severity.CRITICAL);
                        return;
                    }
                }
            } catch (Exception e) {
                ctx.log.log("\u8bf7\u6c42\u5931\u8d25: " + e.getMessage(), LogConsole.Severity.WARNING);
            }
        }
        ctx.log.log(submitPath + " \u672a\u53d1\u73b0\u6709\u6548\u51ed\u8bc1", LogConsole.Severity.INFO);
    }
}

