/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiTester {
    private static final String[] SENSITIVE_KEYS = new String[]{"password", "token", "secret", "key", "credential", "auth", "pwd"};

    public static void run(ScanContext ctx, ScanConfig cfg, String method, String body) {
        if (ctx.discoveredApis.isEmpty()) {
            ctx.log.log("\u8bf7\u5148\u6267\u884c\u63a5\u53e3\u6536\u96c6\uff01", LogConsole.Severity.WARNING);
            return;
        }
        ctx.log.log("\n[+] \u5f00\u59cb\u63a5\u53e3\u6d4b\u8bd5...", LogConsole.Severity.CRITICAL, true);
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        ArrayList<String> apis = new ArrayList<String>(ctx.discoveredApis);
        Collections.sort(apis);
        int total = apis.size();
        AtomicInteger done = new AtomicInteger(0);
        AtomicInteger valid = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(total);
        for (String apiPath : apis) {
            pool.submit(() -> {
                try {
                    if (!ctx.scanning.get()) {
                        return;
                    }
                    String full = cfg.fullApiUrl((String)(apiPath.startsWith("/") ? apiPath : "/" + apiPath));
                    Response resp = ApiTester.sendRequest(http, cfg, full, method, body);
                    if (resp == null) {
                        return;
                    }
                    try (Response response = resp;){
                        String text = resp.body() != null ? resp.body().string() : "";
                        int code = resp.code();
                        String contentType = resp.header("Content-Type", "").toLowerCase();
                        if (code < 200) return;
                        if (code >= 300) {
                            return;
                        }
                        boolean sensitive = false;
                        if (contentType.contains("json")) {
                            try {
                                JsonElement el = new Gson().fromJson(text, JsonElement.class);
                                if (el != null && el.isJsonObject()) {
                                    sensitive = ApiTester.containsSensitive(el.getAsJsonObject());
                                }
                            }
                            catch (Exception el) {
                                // empty catch block
                            }
                        }
                        if (!sensitive && text.length() <= 50) {
                            return;
                        }
                        valid.incrementAndGet();
                        String preview = text.length() > 15 ? text.substring(0, 15) : text;
                        preview = preview.replace('\n', ' ').replace('\r', ' ');
                        LogConsole.Severity sev = sensitive ? LogConsole.Severity.CRITICAL : LogConsole.Severity.PATH;
                        ctx.log.log("[+] \u53d1\u73b0\u6709\u6548\u63a5\u53e3: " + full + " [\u72b6\u6001\u7801: " + code + "]", sev, true);
                        ctx.log.log("[\u9884\u89c8: " + preview + "]", LogConsole.Severity.PREVIEW, true);
                        if (!sensitive) return;
                        String snippet = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                        ctx.log.log("\u54cd\u5e94\u5185\u5bb9: " + snippet, LogConsole.Severity.RESPONSE_CONTENT, true);
                        return;
                    }
                }
                catch (Exception c) {
                    return;
                }
                finally {
                    int c = done.incrementAndGet();
                    ctx.setProgressMax(c, total);
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(20L, TimeUnit.MINUTES);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
        pool.shutdownNow();
        if (valid.get() > 0) {
            ctx.log.log("\n[+] \u63a5\u53e3\u6d4b\u8bd5\u5b8c\u6210\uff0c\u5171\u53d1\u73b0 " + valid.get() + " \u4e2a\u6709\u6548\u63a5\u53e3", LogConsole.Severity.CRITICAL, true);
        } else {
            ctx.log.log("\u63a5\u53e3\u6d4b\u8bd5\u5b8c\u6210\uff0c\u672a\u53d1\u73b0\u6709\u6548\u63a5\u53e3", LogConsole.Severity.INFO);
        }
    }

    private static Response sendRequest(HttpService http, ScanConfig cfg, String url, String method, String body) throws Exception {
        switch (method) {
            case "GET": {
                return http.get(url, cfg);
            }
            case "POST": {
                HashMap<String, String> h = new HashMap<String, String>();
                h.put("Content-Type", "application/x-www-form-urlencoded");
                return http.post(url, cfg, RequestBody.create(body == null ? "" : body, HttpService.FORM), h);
            }
            case "POST-JSON": {
                return http.postJson(url, cfg, body == null ? "{}" : body);
            }
        }
        return null;
    }

    private static boolean containsSensitive(JsonObject obj) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String k = e.getKey().toLowerCase();
            for (String s : SENSITIVE_KEYS) {
                if (!k.contains(s)) continue;
                return true;
            }
            JsonElement v = e.getValue();
            if (v.isJsonObject() && ApiTester.containsSensitive(v.getAsJsonObject())) {
                return true;
            }
            if (!v.isJsonArray()) continue;
            for (JsonElement it : v.getAsJsonArray()) {
                if (!it.isJsonObject() || !ApiTester.containsSensitive(it.getAsJsonObject())) continue;
                return true;
            }
        }
        return false;
    }
}

