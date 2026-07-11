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
import com.ruoyi.scanner.poc.PocLoader;
import okhttp3.Response;

public class SystemApiCheck {

    public static void run(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        ctx.log.log("\n[+] \u5f00\u59cb\u7cfb\u7edf\u63a5\u53e3\u6d4b\u8bd5...", LogConsole.Severity.CRITICAL, true);
        Gson gson = new Gson();
        String[] apis = PocLoader.get().systemApi().paths;
        for (int i = 0; i < apis.length && ctx.scanning.get(); ++i) {
            String path = apis[i];
            String url = cfg.fullApiUrl(path);
            ctx.setStatus("\u6b63\u5728\u6d4b\u8bd5: " + path);
            ctx.setProgressMax(i + 1, apis.length);
            try (Response resp = http.get(url, cfg)) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (resp.code() == 200) {
                    JsonElement el;
                    try {
                        el = gson.fromJson(body, JsonElement.class);
                    } catch (Exception ex) {
                        el = null;
                    }
                    if (el != null && el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("code") && obj.get("code").isJsonPrimitive()
                                && obj.get("code").getAsJsonPrimitive().isNumber()
                                && obj.get("code").getAsInt() == 200) {
                            ctx.log.log("[+] \u63a5\u53e3\u5b58\u5728: " + path, LogConsole.Severity.CRITICAL, true);
                            String preview = body.length() > 200 ? body.substring(0, 200) + "..." : body;
                            ctx.log.log("\u54cd\u5e94\u5185\u5bb9: " + preview, LogConsole.Severity.RESPONSE_CONTENT, true);
                            continue;
                        }
                        ctx.log.log("[-] \u63a5\u53e3\u54cd\u5e94\u5f02\u5e38: " + path, LogConsole.Severity.WARNING, true);
                        continue;
                    }
                    if (body.toLowerCase().contains("success") || body.contains("\u6210\u529f")) {
                        ctx.log.log("[+] \u63a5\u53e3\u5b58\u5728: " + path, LogConsole.Severity.CRITICAL, true);
                        continue;
                    }
                    ctx.log.log("[-] \u63a5\u53e3\u54cd\u5e94\u975eJSON: " + path, LogConsole.Severity.WARNING, true);
                    continue;
                }
                ctx.log.log("[-] \u63a5\u53e3\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650: " + path + " (\u72b6\u6001\u7801: " + resp.code() + ")", LogConsole.Severity.WARNING, true);
            } catch (Exception e) {
                ctx.log.log("[-] \u8bf7\u6c42\u5f02\u5e38: " + path + " - " + e.getMessage(), LogConsole.Severity.WARNING, true);
            }
        }
        ctx.log.log("\n[+] \u7cfb\u7edf\u63a5\u53e3\u6d4b\u8bd5\u5b8c\u6210", LogConsole.Severity.INFO, true);
    }
}

