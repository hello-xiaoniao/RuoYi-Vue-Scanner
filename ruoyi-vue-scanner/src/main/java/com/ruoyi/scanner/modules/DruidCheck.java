/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.RuoYiResponse;
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

    /**
     * Druid \u5f3a\u7279\u5f81\u8bcd\u3002\u53ea\u6709\u547d\u4e2d\u81f3\u5c11 2 \u4e2a\u624d\u786e\u8ba4\u662f\u771f\u5b9e Druid \u9875\u9762\u3002
     * \u907f\u514d SPA \u524d\u7aef\u9875\u9762\u6216 RuoYi \u9519\u8bef\u54cd\u5e94\u4e2d\u7684 "druid" \u5b57\u7b26\u4e32\u8bef\u5339\u914d\u3002
     */
    private static final String[] DRUID_FEATURES = {
        "DruidVersion",
        "druidStatView",
        "DruidStatService",
        "druid.common",
        ".druid.monitor",
        "com.alibaba.druid",
        "submitLogin",
        "DruidSecondary",
        "druid.login.username"
    };

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

                // \u5148\u6392\u9664 RuoYi \u9519\u8bef\u5305\u88c5
                RuoYiResponse.Result rr = RuoYiResponse.parse(text);
                if (rr.status == RuoYiResponse.Status.DENIED
                        || rr.status == RuoYiResponse.Status.ERROR
                        || rr.status == RuoYiResponse.Status.SPA) {
                    ctx.log.log(path + " \u975eDruid: " + rr.status, LogConsole.Severity.INFO);
                    return;
                }

                // JavaClassPath \u662f Druid /basic.json \u72ec\u6709\u7684\u5f3a\u7279\u5f81
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

    /**
     * Druid \u7aef\u70b9\u68c0\u6d4b\uff0c\u4e09\u5c42\u5224\u5b9a\uff1a
     * 1. RuoYiResponse \u6392\u9664 RuoYi \u9519\u8bef\u5305\u88c5\u548c SPA \u9875\u9762
     * 2. \u5f3a\u7279\u5f81\u8bcd\u5339\u914d\uff08\u81f3\u5c11\u547d\u4e2d 2 \u4e2a\u624d\u786e\u8ba4\uff09
     * 3. \u907f\u514d SPA/\u524d\u7aef\u9875\u9762/\u9519\u8bef\u6d88\u606f\u4e2d\u7684 "druid" \u5b57\u7b26\u4e32\u8bef\u5339\u914d
     */
    private static void detectEndpoint(ScanContext ctx, HttpService http, ScanConfig cfg, String path) {
        String url = cfg.fullUrl(path);
        try (Response resp = http.get(url, cfg)) {
            String text = resp.body() != null ? resp.body().string() : "";

            // \u7b2c\u4e00\u5c42\uff1aRuoYiResponse \u7edf\u4e00\u5224\u5b9a
            RuoYiResponse.Result rr = RuoYiResponse.parse(text);
            if (rr.status == RuoYiResponse.Status.DENIED) {
                ctx.log.log(path + " \u9700\u8981\u8ba4\u8bc1\u6216\u4e0d\u5b58\u5728\uff08RuoYi\u9519\u8bef\u5305\u88c5\uff09", LogConsole.Severity.INFO);
                return;
            }
            if (rr.status == RuoYiResponse.Status.ERROR) {
                ctx.log.log(path + " \u670d\u52a1\u7aef\u9519\u8bef", LogConsole.Severity.WARNING);
                return;
            }
            if (rr.status == RuoYiResponse.Status.SPA) {
                ctx.log.log(path + " \u8fd4\u56de\u524d\u7aefSPA\u9875\u9762\uff0c\u975eDruid", LogConsole.Severity.INFO);
                return;
            }
            if (rr.status == RuoYiResponse.Status.UNKNOWN && resp.code() != 200) {
                ctx.log.log(path + " \u6f0f\u6d1e\u53ef\u80fd\u4e0d\u5b58\u5728", LogConsole.Severity.INFO);
                return;
            }

            // \u7b2c\u4e8c\u5c42\uff1a\u5f3a\u7279\u5f81\u8bcd\u5339\u914d\uff08\u9700\u547d\u4e2d\u81f3\u5c11 2 \u4e2a\uff09
            String low = text.toLowerCase();
            int hitCount = 0;
            for (String f : DRUID_FEATURES) {
                if (low.contains(f.toLowerCase())) {
                    hitCount++;
                    if (hitCount >= 2) break;
                }
            }

            if (hitCount >= 2) {
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

