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

public class BasicVulnChecks {

    public static void runFileDownload(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        String[][] payloads = PocLoader.get().basic().file_download;
        for (String[] p : payloads) {
            if (!ctx.scanning.get()) return;
            detectByKeyword(ctx, http, cfg, p[0], p[1]);
        }
    }

    public static void runSqlInjection(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        String[] paths = PocLoader.get().basic().sql_injection_paths;
        String inject = PocLoader.get().basic().sql_injection_inject;
        String[] errorKeywords = PocLoader.get().basic().sql_injection_error_keywords;

        for (String path : paths) {
            if (!ctx.scanning.get()) return;

            // Step 1: \u5148\u53d1\u6b63\u5e38\u8bf7\u6c42\u786e\u8ba4\u63a5\u53e3\u662f\u5426\u53ef\u8fbe\uff08\u672a\u8ba4\u8bc1/\u5df2\u7981\u6b62\u7684\u63a5\u53e3\u4e0d\u6d4b\u6ce8\u5165\uff09
            String normalUrl = cfg.fullApiUrl(path + "?pageNum=1&pageSize=10");
            try (Response normalResp = http.get(normalUrl, cfg)) {
                String normalText = normalResp.body() != null ? normalResp.body().string() : "";
                RuoYiResponse.Result rr = RuoYiResponse.parse(normalText);
                if (rr.status == RuoYiResponse.Status.DENIED) {
                    ctx.log.log(path + " \u9700\u8981\u8ba4\u8bc1\uff0c\u8df3\u8fc7SQL\u6ce8\u5165\u68c0\u6d4b", LogConsole.Severity.INFO);
                    continue;
                }
            } catch (Exception ignored) {
                // \u6b63\u5e38\u8bf7\u6c42\u5931\u8d25\uff0c\u7ee7\u7eed\u5c1d\u8bd5\u6ce8\u5165\u68c0\u6d4b
            }

            // Step 2: \u53d1\u9001\u6ce8\u5165 payload
            String probeUrl = path + "?pageNum=1&pageSize=10&orderByColumn=" + inject;
            String url = cfg.fullApiUrl(probeUrl);
            try (Response resp = http.get(url, cfg)) {
                String text = resp.body() != null ? resp.body().string() : "";

                // Step 3: \u6392\u9664 RuoYi \u9519\u8bef\u5305\u88c5\uff08code\u2260200 \u7684 JSON \u54cd\u5e94\uff09
                RuoYiResponse.Result rr = RuoYiResponse.parse(text);
                if (rr.status == RuoYiResponse.Status.DENIED || rr.status == RuoYiResponse.Status.ERROR) {
                    ctx.log.log(path + " \u6ce8\u5165\u8bf7\u6c42\u8fd4\u56de\u9519\u8bef\u54cd\u5e94\uff08\u975e\u6ce8\u5165\uff0c\u53ef\u80fd\u662f\u6846\u67b6\u62a5\u9519\uff09", LogConsole.Severity.INFO);
                    continue;
                }

                // Step 4: errorKeywords \u5339\u914d
                boolean hit = false;
                for (String kw : errorKeywords) {
                    if (text.toLowerCase().contains(kw.toLowerCase())) { hit = true; break; }
                }
                if (hit) {
                    ctx.log.log("[\u4e25\u91cd] SQL\u6ce8\u5165\u6f0f\u6d1e(\u7591\u4f3c): " + url, LogConsole.Severity.CRITICAL);
                } else {
                    ctx.log.log(path + " SQL\u6ce8\u5165\u6f0f\u6d1e\u4e0d\u5b58\u5728 (normal response)", LogConsole.Severity.INFO);
                }
            } catch (Exception e) {
                ctx.log.log(path + " \u68c0\u6d4b\u5931\u8d25: " + e.getMessage(), LogConsole.Severity.WARNING);
            }
        }
    }

    public static void runScheduledTask(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        String url = cfg.fullApiUrl("/monitor/job");
        String body = PocLoader.get().basic().scheduled_task_json;
        try (Response resp = http.put(url, cfg, body)) {
            String text = resp.body() != null ? resp.body().string() : "";
            RuoYiResponse.Result rr = RuoYiResponse.parse(text);
            if (rr.status == RuoYiResponse.Status.SUCCESS || rr.status == RuoYiResponse.Status.SUCCESS_EMPTY) {
                ctx.log.log("/monitor/job \u8bf7\u6c42\u6210\u529f\uff08\u4e1a\u52a1code=200\uff09\u2014\u2014\u5b9a\u65f6\u4efb\u52a1\u63a5\u53e3\u53ef\u80fd\u5b58\u5728\u6f0f\u6d1e\uff0c\u9700\u8fdb\u4e00\u6b65\u9a8c\u8bc1", LogConsole.Severity.WARNING);
            } else if (rr.status == RuoYiResponse.Status.DENIED) {
                ctx.log.log("/monitor/job \u9700\u8981\u8ba4\u8bc1\uff0c\u8df3\u8fc7", LogConsole.Severity.INFO);
            } else {
                ctx.log.log("/monitor/job \u6f0f\u6d1e\u53ef\u80fd\u4e0d\u5b58\u5728", LogConsole.Severity.INFO);
            }
        } catch (Exception e) {
            ctx.log.log("/monitor/job \u8bf7\u6c42\u5931\u8d25: " + e.getMessage(), LogConsole.Severity.WARNING);
        }
    }

    public static void runPasswordReset(ScanContext ctx, ScanConfig cfg) {
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        String body = PocLoader.get().basic().password_reset_json;
        String url = cfg.fullApiUrl("/system/user/profile");
        try (Response resp = http.put(url, cfg, body)) {
            String text = resp.body() != null ? resp.body().string() : "";
            RuoYiResponse.Result rr = RuoYiResponse.parse(text);
            if (rr.status == RuoYiResponse.Status.SUCCESS) {
                // code==200 \u4e14\u6709\u4e1a\u52a1\u6570\u636e \u2192 \u5bc6\u7801\u91cd\u7f6e\u53ef\u80fd\u6210\u529f\uff0c\u9700\u8981\u4e8c\u6b21\u9a8c\u8bc1
                ctx.log.log("/system/user/profile \u8bf7\u6c42\u6210\u529f\uff08\u4e1a\u52a1code=200\uff0cdata\u975e\u7a7a\uff09\u2014\u2014\u5bc6\u7801\u91cd\u7f6e\u53ef\u80fd\u5b58\u5728\u6f0f\u6d1e\uff0c\u9700\u4e8c\u6b21\u9a8c\u8bc1\u767b\u5f55", LogConsole.Severity.WARNING);
            } else if (rr.status == RuoYiResponse.Status.SUCCESS_EMPTY) {
                ctx.log.log("/system/user/profile \u8bf7\u6c42\u6210\u529f\u4f46data\u4e3a\u7a7a\u2014\u2014\u53ef\u80fd\u9700\u8ba4\u8bc1\u6216\u53c2\u6570\u4e0d\u5b8c\u6574", LogConsole.Severity.INFO);
            } else if (rr.status == RuoYiResponse.Status.DENIED) {
                ctx.log.log("/system/user/profile \u9700\u8981\u8ba4\u8bc1\uff0c\u8df3\u8fc7", LogConsole.Severity.INFO);
            } else {
                ctx.log.log("/system/user/profile \u6f0f\u6d1e\u53ef\u80fd\u4e0d\u5b58\u5728", LogConsole.Severity.INFO);
            }
        } catch (Exception e) {
            ctx.log.log("/system/user/profile \u8bf7\u6c42\u5931\u8d25: " + e.getMessage(), LogConsole.Severity.WARNING);
        }
    }

    private static void detectByKeyword(ScanContext ctx, HttpService http, ScanConfig cfg, String path, String keyword) {
        String url = cfg.fullUrl(path);
        try (Response resp = http.get(url, cfg)) {
            String text = resp.body() != null ? resp.body().string() : "";
            // \u5148\u6392\u9664 RuoYi \u9519\u8bef\u5305\u88c5\uff08\u907f\u514d keyword \u5728\u9519\u8bef\u54cd\u5e94\u4e2d\u8bef\u5339\u914d\uff09
            RuoYiResponse.Result rr = RuoYiResponse.parse(text);
            if (rr.status == RuoYiResponse.Status.DENIED || rr.status == RuoYiResponse.Status.ERROR) {
                ctx.log.log(path + " \u9700\u8981\u8ba4\u8bc1\u6216\u4e0d\u5b58\u5728\uff0c\u8df3\u8fc7", LogConsole.Severity.INFO);
                return;
            }
            if (keyword != null && text.contains(keyword)) {
                ctx.log.log(path + " \u5b58\u5728\u6f0f\u6d1e", LogConsole.Severity.CRITICAL);
            } else {
                ctx.log.log(path + " \u6f0f\u6d1e\u53ef\u80fd\u4e0d\u5b58\u5728", LogConsole.Severity.INFO);
            }
        } catch (Exception e) {
            ctx.log.log(path + " \u8bf7\u6c42\u5931\u8d25: " + e.getMessage(), LogConsole.Severity.WARNING);
        }
    }
}

