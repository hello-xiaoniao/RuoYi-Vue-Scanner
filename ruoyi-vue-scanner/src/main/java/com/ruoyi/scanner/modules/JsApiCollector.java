/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.ruoyi.scanner.core.HttpService;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Response;

public class JsApiCollector {
    private static final Pattern SCRIPT_SRC = Pattern.compile("<script\\b[^>]*src\\s*=\\s*['\"]([^'\"]+?\\.js(?:\\?[^'\"]*)?)['\"][^>]*>", 2);
    private static final Pattern CHUNK = Pattern.compile("\"\\b(chunk-[a-fA-F0-9]{8,32})\\b\"\\s*:\\s*\"([a-fA-F0-9]{8})\"");
    private static final Pattern[] API_PATTERNS = new Pattern[]{Pattern.compile("[\"'`]\\s*((?:/[\\w\\-/]+)+)\\s*[\"'`]", 2), Pattern.compile("\\.(?:get|post|put|patch|delete)\\s*\\(\\s*[\"'`]([^\"'`?#]+)", 2), Pattern.compile("path\\s*:\\s*[\"'`]([^\"'`?#]+)", 2), Pattern.compile("import\\(\\s*[\"'`]([^\"'`?#]+)[\"'`]", 2)};
    private static final String[] EXCLUDE_PREFIXES = new String[]{"static/", "assets/", "css/", "js/", "img/"};
    private static final String[] INCLUDE_KEYS = new String[]{"api", "prod-api", "dev-api", "monitor", "system"};
    private static final Pattern PATH_LIKE = Pattern.compile("^[\\w-]+/[\\w-]+(/[\\w-]+)*$");

    public static void run(ScanContext ctx, ScanConfig cfg) {
        ctx.log.log("\u5f00\u59cb\u6536\u96c6JS\u63a5\u53e3...", LogConsole.Severity.INFO);
        ctx.discoveredApis.clear();
        List jsFiles = Collections.synchronizedList(new ArrayList());
        HttpService http = new HttpService(cfg, ctx.timeoutSec);
        try {
            String html;
            try (Response resp = http.get(cfg.url, cfg);){
                if (resp.code() != 200) {
                    ctx.log.log("\u65e0\u6cd5\u83b7\u53d6\u4e3b\u9875\u5185\u5bb9\uff0c\u72b6\u6001\u7801\uff1a" + resp.code(), LogConsole.Severity.WARNING);
                    return;
                }
                html = resp.body() != null ? resp.body().string() : "";
            }
            LinkedHashSet<String> jsUrls = new LinkedHashSet<String>();
            Matcher m1 = SCRIPT_SRC.matcher(html);
            while (m1.find()) {
                jsUrls.add(JsApiCollector.resolve(cfg.url, m1.group(1)));
            }
            Matcher m2 = CHUNK.matcher(html);
            int chunkCount = 0;
            while (m2.find()) {
                jsUrls.add(JsApiCollector.resolve(cfg.url, "/static/js/" + m2.group(1) + "." + m2.group(2) + ".js"));
                ++chunkCount;
            }
            ctx.log.log("\u53d1\u73b0 " + jsUrls.size() + " \u4e2aJS\u6587\u4ef6\uff08\u542b" + chunkCount + "\u4e2a\u52a8\u6001chunk\uff09", LogConsole.Severity.INFO);
            ConcurrentHashMap.KeySetView discovered = ConcurrentHashMap.newKeySet();
            ExecutorService pool = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(jsUrls.size());
            AtomicInteger done = new AtomicInteger(0);
            int total = jsUrls.size();
            for (String url : jsUrls) {
                pool.submit(() -> {
                    try {
                        if (!ctx.scanning.get()) {
                            return;
                        }
                        try (Response resp = http.get(url, cfg);){
                            if (resp.code() != 200) {
                                return;
                            }
                            String contentType = resp.header("Content-Type", "").toLowerCase();
                            if (!contentType.contains("javascript") && !url.endsWith(".js")) {
                                return;
                            }
                            String text = resp.body() != null ? resp.body().string() : "";
                            jsFiles.add(new ScanContext.JsFile(url, text));
                            for (Pattern p : API_PATTERNS) {
                                Matcher mm = p.matcher(text);
                                while (mm.find()) {
                                    String cleaned = JsApiCollector.cleanPath(mm.group(1));
                                    if (cleaned == null) continue;
                                    discovered.add(cleaned);
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        ctx.log.log("\u5904\u7406 " + url + " \u65f6\u5f02\u5e38: " + e.getMessage(), LogConsole.Severity.WARNING);
                    }
                    finally {
                        int c = done.incrementAndGet();
                        ctx.setProgressMax(c, total);
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await(5L, TimeUnit.MINUTES);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            pool.shutdownNow();
            ctx.discoveredApis.addAll(discovered);
            ctx.jsFiles = new ArrayList<ScanContext.JsFile>(jsFiles);
            if (!ctx.discoveredApis.isEmpty()) {
                ctx.log.log("[+] \u53d1\u73b0\u4ee5\u4e0b\u63a5\u53e3\u8def\u5f84:", LogConsole.Severity.CRITICAL, true);
                ArrayList<String> sorted2 = new ArrayList<String>(ctx.discoveredApis);
                Collections.sort(sorted2);
                for (String api : sorted2) {
                    ctx.log.log(api, LogConsole.Severity.PATH, true);
                }
            } else {
                ctx.log.log("\u672a\u53d1\u73b0\u6709\u6548\u63a5\u53e3\u8def\u5f84", LogConsole.Severity.INFO);
            }
        }
        catch (Exception e) {
            ctx.log.log("\u63a5\u53e3\u6536\u96c6\u5f02\u5e38: " + e.getMessage(), LogConsole.Severity.CRITICAL);
        }
    }

    private static String resolve(String base, String path) {
        try {
            return URI.create(base + "/").resolve(path).toString();
        }
        catch (Exception e) {
            return path;
        }
    }

    private static String cleanPath(String raw) {
        int h;
        if (raw == null) {
            return null;
        }
        String path = raw;
        int q = (path = path.replaceAll("\\$\\{[^}]+}", "")).indexOf(63);
        if (q >= 0) {
            path = path.substring(0, q);
        }
        if ((h = path.indexOf(35)) >= 0) {
            path = path.substring(0, h);
        }
        path = path.replaceAll("//+", "/");
        if ((path = path.replaceAll("^/+", "").replaceAll("/+$", "")).isEmpty()) {
            return null;
        }
        for (String pre : EXCLUDE_PREFIXES) {
            if (!path.startsWith(pre)) continue;
            return null;
        }
        for (String k : INCLUDE_KEYS) {
            if (!path.contains(k)) continue;
            return path;
        }
        if (PATH_LIKE.matcher(path).matches()) {
            return path;
        }
        return null;
    }
}

