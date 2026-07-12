/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.RuoYiResponse;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import com.ruoyi.scanner.poc.PocLoader;
import okhttp3.Response;

public class SystemApiCheck {

    /**
     * \u654f\u611f\u5b57\u6bb5\u68c0\u6d4b\u6b63\u5219\uff1a\u624b\u673a\u53f7\u3001\u8eab\u4efd\u8bc1\u53f7\u3001\u5b66\u53f7\u3002
     * \u547d\u4e2d\u8fd9\u4e9b\u5b57\u6bb5\u65f6\u624d\u5c06"\u63a5\u53e3\u5b58\u5728"\u63d0\u7ea7\u4e3a CRITICAL\u3002
     */
    private static final String SENSITIVE_PATTERN =
            "(1[3-9]\\d{9})"                                     // \u624b\u673a\u53f7
            + "|(\\d{17}[\\dXx])"                                // \u8eab\u4efd\u8bc1\u53f7
            + "|(\"studentId\"\\s*:)"                            // \u5b66\u53f7\u5b57\u6bb5
            + "|(\"phone\"\\s*:)|(\"mobile\"\\s*:)"              // \u7535\u8bdd\u5b57\u6bb5
            + "|(\"idCard\"\\s*:)|(\"idNumber\"\\s*:)"           // \u8eab\u4efd\u8bc1\u5b57\u6bb5
            + "|(\"realName\"\\s*:)|(\"name\"\\s*:\\s*\"[\\u4e00-\\u9fa5]{2,4}\")"; // \u4e2d\u6587\u59d3\u540d

    public static void run(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        ctx.log.log("\n[+] \u5f00\u59cb\u7cfb\u7edf\u63a5\u53e3\u6d4b\u8bd5...", LogConsole.Severity.INFO, true);
        String[] apis = PocLoader.get().systemApi().paths;
        for (int i = 0; i < apis.length && ctx.scanning.get(); ++i) {
            String path = apis[i];
            String url = cfg.fullApiUrl(path);
            ctx.setStatus("\u6b63\u5728\u6d4b\u8bd5: " + path);
            ctx.setProgressMax(i + 1, apis.length);
            try (Response resp = http.get(url, cfg)) {
                String body = resp.body() != null ? resp.body().string() : "";
                RuoYiResponse.Result rr = RuoYiResponse.parse(body);

                if (rr.status == RuoYiResponse.Status.SUCCESS) {
                    // \u63a5\u53e3\u53ef\u8fbe\u4e14\u6709\u4e1a\u52a1\u6570\u636e\u2014\u2014\u68c0\u67e5\u662f\u5426\u5305\u542b\u654f\u611f\u5b57\u6bb5
                    boolean hasSensitive = containsSensitiveData(body);
                    if (hasSensitive) {
                        ctx.log.log("[!] \u654f\u611f\u6570\u636e\u6cc4\u9732\u98ce\u9669: " + path + "\uff08\u672a\u6388\u6743\u8fd4\u56de\u624b\u673a\u53f7/\u8eab\u4efd\u8bc1/\u59d3\u540d\u7b49\u654f\u611f\u5b57\u6bb5\uff09",
                                LogConsole.Severity.CRITICAL, true);
                    } else {
                        ctx.log.log("[+] \u63a5\u53e3\u5b58\u5728\u4e14\u6709\u6570\u636e: " + path, LogConsole.Severity.INFO, true);
                    }
                    String preview = body.length() > 200 ? body.substring(0, 200) + "..." : body;
                    ctx.log.log("\u54cd\u5e94\u5185\u5bb9: " + preview, LogConsole.Severity.RESPONSE_CONTENT, true);

                } else if (rr.status == RuoYiResponse.Status.SUCCESS_EMPTY) {
                    // \u63a5\u53e3\u53ef\u8fbe\u4f46\u65e0\u4e1a\u52a1\u6570\u636e\uff08\u7a7a\u6570\u7ec4/\u7a7a\u5bf9\u8c61\uff09
                    ctx.log.log("[+] \u63a5\u53e3\u5b58\u5728\uff08\u7a7a\u6570\u636e\uff09: " + path, LogConsole.Severity.INFO, true);

                } else if (rr.status == RuoYiResponse.Status.DENIED) {
                    // \u63a5\u53e3\u5b58\u5728\u4f46\u9700\u8981\u8ba4\u8bc1\u2014\u2014\u6b63\u5e38\u60c5\u51b5\uff0c\u4e0d\u62a5
                    ctx.log.log("[-] \u63a5\u53e3\u5b58\u5728\u4f46\u9700\u8ba4\u8bc1: " + path, LogConsole.Severity.INFO, true);

                } else if (rr.status == RuoYiResponse.Status.ERROR) {
                    ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u9519\u8bef: " + path, LogConsole.Severity.WARNING, true);

                } else if (rr.status == RuoYiResponse.Status.SPA) {
                    ctx.log.log("[-] \u8fd4\u56de\u524d\u7aefSPA\u9875\u9762: " + path, LogConsole.Severity.INFO, true);

                } else {
                    // UNKNOWN \u2014 \u975eJSON\uff0c\u6309\u539f\u59cb\u903b\u8f91\u515c\u5e95
                    if (resp.code() == 200) {
                        ctx.log.log("[-] \u63a5\u53e3\u8fd4\u56de\u975eJSON: " + path, LogConsole.Severity.WARNING, true);
                    } else {
                        ctx.log.log("[-] \u63a5\u53e3\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650: " + path + " (\u72b6\u6001\u7801: " + resp.code() + ")", LogConsole.Severity.WARNING, true);
                    }
                }
            } catch (Exception e) {
                ctx.log.log("[-] \u8bf7\u6c42\u5f02\u5e38: " + path + " - " + e.getMessage(), LogConsole.Severity.WARNING, true);
            }
        }
        ctx.log.log("\n[+] \u7cfb\u7edf\u63a5\u53e3\u6d4b\u8bd5\u5b8c\u6210", LogConsole.Severity.INFO, true);
    }

    /**
     * \u68c0\u67e5\u54cd\u5e94\u4f53\u662f\u5426\u5305\u542b\u654f\u611f\u5b57\u6bb5\uff08\u624b\u673a\u53f7/\u8eab\u4efd\u8bc1/\u5b66\u53f7/\u4e2d\u6587\u59d3\u540d\uff09\u3002
     */
    private static boolean containsSensitiveData(String body) {
        if (body == null || body.isEmpty()) return false;
        return body.matches(".*" + SENSITIVE_PATTERN + ".*");
    }
}

