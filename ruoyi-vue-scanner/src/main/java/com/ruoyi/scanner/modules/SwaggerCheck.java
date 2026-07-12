/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.RuoYiResponse;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import com.ruoyi.scanner.poc.PocLoader;
import okhttp3.Response;

public class SwaggerCheck {

    private static final Gson GSON = new Gson();

    public static void run(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        String[] paths = PocLoader.get().swagger().paths;
        String[] features = PocLoader.get().swagger().features;
        String[] denyPatterns = PocLoader.get().swagger().deny_patterns;

        for (String path : paths) {
            if (!ctx.scanning.get()) return;
            // try raw + baseApi prefix
            tryProbe(ctx, cfg, http, path, features, denyPatterns);
            if (path.startsWith("/")) {
                tryProbe(ctx, cfg, http, cfg.baseApi + path, features, denyPatterns);
            }
        }
    }

    /**
     * Swagger \u68c0\u6d4b\uff0c\u4e09\u5c42\u5224\u5b9a\uff1a
     * 1. RuoYiResponse \u6392\u9664 RuoYi \u9519\u8bef\u5305\u88c5\uff08{"code":401/403/404/500,...}\uff09\u548c SPA \u9875\u9762
     * 2. deny_patterns \u6392\u9664\u9519\u8bef\u54cd\u5e94\u4e2d\u7684\u7279\u5f81\u8bcd
     * 3. features \u547d\u4e2d\u540e\uff0cisGenuineSwagger \u4e8c\u6b21\u9a8c\u8bc1\u786e\u4fdd\u662f\u771f\u5b9e Swagger \u6587\u6863/UI
     */
    private static void tryProbe(ScanContext ctx, ScanConfig cfg, HttpService http,
                                  String path, String[] features, String[] denyPatterns) {
        String url = cfg.fullUrl(path);
        try (Response resp = http.get(url, cfg)) {
            if (!ctx.scanning.get()) return;
            if (resp.code() == 200) {
                String body = resp.body() != null ? resp.body().string() : "";

                // \u2550\u2550\u2550 \u7b2c\u4e00\u5c42\uff1aRuoYiResponse \u7edf\u4e00\u5224\u5b9a \u2550\u2550\u2550
                RuoYiResponse.Result rr = RuoYiResponse.parse(body);
                if (rr.status == RuoYiResponse.Status.DENIED) {
                    ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u8ba4\u8bc1/404\u9519\u8bef\uff08RuoYi\u5305\u88c5\uff09: " + url, LogConsole.Severity.INFO);
                    return;
                }
                if (rr.status == RuoYiResponse.Status.ERROR) {
                    ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u670d\u52a1\u7aef\u9519\u8bef: " + url, LogConsole.Severity.WARNING);
                    return;
                }
                if (rr.status == RuoYiResponse.Status.SPA) {
                    ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u524d\u7aefSPA\u9875\u9762\uff0c\u975eSwagger: " + url, LogConsole.Severity.INFO);
                    return;
                }
                // SUCCESS / SUCCESS_EMPTY / UNKNOWN \u2192 \u7ee7\u7eed\u5f80\u4e0b\u9a8c\u8bc1

                // \u2550\u2550\u2550 \u7b2c\u4e8c\u5c42\uff1adeny_patterns \u5339\u914d \u2550\u2550\u2550
                String compact = body.replaceAll("\\s+", "").toLowerCase();
                boolean denied = false;
                for (String d : denyPatterns) {
                    if (compact.contains(d.toLowerCase())) { denied = true; break; }
                }
                if (denied) {
                    ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u9519\u8bef\u54cd\u5e94\uff08\u547d\u4e2ddeny pattern\uff09: " + url, LogConsole.Severity.INFO);
                    return;
                }

                // \u2550\u2550\u2550 \u7b2c\u4e09\u5c42\uff1afeatures \u5339\u914d + \u4e8c\u6b21\u9a8c\u8bc1 \u2550\u2550\u2550
                boolean hit = false;
                for (String f : features) {
                    if (compact.contains(f.toLowerCase())) { hit = true; break; }
                }
                if (hit) {
                    // \u4e8c\u6b21\u9a8c\u8bc1\uff1a\u786e\u8ba4\u54cd\u5e94\u4f53\u662f\u771f\u5b9e Swagger/OpenAPI \u6587\u6863\u6216 Swagger UI
                    if (isGenuineSwagger(body)) {
                        ctx.log.log("[+] \u53d1\u73b0Swagger\u63a5\u53e3: " + url, LogConsole.Severity.CRITICAL);
                    } else {
                        ctx.log.log("[-] \u7279\u5f81\u547d\u4e2d\u4f46\u975e\u771f\u5b9eSwagger\u6587\u6863\uff08\u7591\u4f3c\u8bef\u62a5\u5df2\u8fc7\u6ee4\uff09: " + url, LogConsole.Severity.WARNING);
                    }
                    return;
                }

                ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de200\u4f46\u672a\u5305\u542bSwagger\u7279\u5f81: " + url, LogConsole.Severity.INFO);
            } else {
                ctx.log.log("[-] Swagger\u63a5\u53e3\u4e0d\u5b58\u5728: " + url, LogConsole.Severity.INFO);
            }
        } catch (Exception e) {
            ctx.log.log("[-] \u68c0\u6d4bSwagger\u63a5\u53e3\u5f02\u5e38: " + url + ", " + e.getMessage(), LogConsole.Severity.WARNING);
        }
    }

    /**
     * \u4e8c\u6b21\u9a8c\u8bc1\uff1a\u786e\u8ba4\u54cd\u5e94\u4f53\u662f\u771f\u5b9e\u7684 Swagger/OpenAPI \u6587\u6863\u6216 Swagger UI \u9875\u9762\u3002
     *
     * \u7279\u5f81\u547d\u4e2d\uff08\u5982 compact \u542b "api-docs"\uff09\u4ec5\u8bf4\u660e URL \u6216\u5185\u5bb9\u4e2d\u51fa\u73b0\u76f8\u5173\u5b57\u7b26\u4e32\uff0c
     * \u4e0d\u4e00\u5b9a\u662f\u771f\u5b9e Swagger \u6587\u6863\u3002\u672c\u65b9\u6cd5\u89e3\u6790 JSON \u7ed3\u6784\u505a\u786e\u8ba4\u3002
     */
    private static boolean isGenuineSwagger(String body) {
        if (body == null || body.isEmpty()) return false;

        // Swagger UI HTML \u9875\u9762\u7279\u5f81
        if (body.contains("swagger-ui") || body.contains("SwaggerUIBundle")
                || body.contains("swagger-initializer") || body.contains("swagger-ui.css")) {
            return true;
        }

        // JSON \u7ed3\u6784\u9a8c\u8bc1
        try {
            JsonElement el = GSON.fromJson(body.trim(), JsonElement.class);
            if (el != null && el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();

                // Swagger 2.0: {"swagger":"2.0", "info":{...}, "paths":{...}}
                // OpenAPI 3.x: {"openapi":"3.x", "info":{...}, "paths":{...}}
                // Swagger Resource: {"swaggerVersion":"2.0", "urls":[...]}
                boolean hasSwaggerVersion = obj.has("swagger") || obj.has("openapi") || obj.has("swaggerVersion");
                boolean hasInfoOrPaths = obj.has("info") || obj.has("paths");
                boolean hasDefinitions = obj.has("definitions") || obj.has("components") || obj.has("schemas");

                if (hasSwaggerVersion && (hasInfoOrPaths || hasDefinitions)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // \u975e JSON\uff0c\u4e0d\u547d\u4e2d
        }
        return false;
    }
}

