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
            // try orderByColumn injection
            String probeUrl = path + "?pageNum=1&pageSize=10&orderByColumn=" + inject;
            String url = cfg.fullApiUrl(probeUrl);
            try (Response resp = http.get(url, cfg)) {
                String text = resp.body() != null ? resp.body().string() : "";
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
            if (resp.code() == 200) {
                ctx.log.log("/monitor/job \u8bf7\u6c42\u6210\u529f\u2014\u2014\u5b9a\u65f6\u4efb\u52a1\u53ef\u80fd\u5b58\u5728\u6f0f\u6d1e", LogConsole.Severity.CRITICAL);
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
            if (resp.code() == 200) {
                ctx.log.log("/system/user/profile \u8bf7\u6c42\u6210\u529f\u2014\u2014\u91cd\u7f6e\u5bc6\u7801\u53ef\u80fd\u5b58\u5728\u6f0f\u6d1e\uff0c\u5c1d\u8bd5\u767b\u5f55 admin/test123456", LogConsole.Severity.CRITICAL);
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

