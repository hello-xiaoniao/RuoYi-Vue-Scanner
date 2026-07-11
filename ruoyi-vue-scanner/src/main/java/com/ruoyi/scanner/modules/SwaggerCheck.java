/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import com.ruoyi.scanner.poc.PocLoader;
import okhttp3.Response;

public class SwaggerCheck {

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

    private static void tryProbe(ScanContext ctx, ScanConfig cfg, HttpService http,
                                  String path, String[] features, String[] denyPatterns) {
        String url = cfg.fullUrl(path);
        try (Response resp = http.get(url, cfg)) {
            if (!ctx.scanning.get()) return;
            if (resp.code() == 200) {
                String body = resp.body() != null ? resp.body().string() : "";
                String compact = body.replaceAll("\\s+", "").toLowerCase();
                boolean hit = false;
                for (String f : features) {
                    if (compact.contains(f.toLowerCase())) { hit = true; break; }
                }
                if (hit) {
                    ctx.log.log("[+] \u53d1\u73b0Swagger\u63a5\u53e3: " + url, LogConsole.Severity.CRITICAL);
                    return;
                }
                boolean denied = false;
                for (String d : denyPatterns) {
                    if (compact.contains(d.toLowerCase())) { denied = true; break; }
                }
                if (denied) {
                    ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u9519\u8bef\u54cd\u5e94\uff08\u7591\u4f3c\u672a\u6388\u6743\uff09: " + url, LogConsole.Severity.INFO);
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
}

