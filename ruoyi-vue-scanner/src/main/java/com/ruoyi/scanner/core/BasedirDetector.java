/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.core;

import com.ruoyi.scanner.core.LogConsole;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

public class BasedirDetector {
    private static final List<String> KNOWN_PREFIXES = Arrays.asList("/prod-api", "/dev-api", "/stage-api", "/test-api", "/api");
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static void detect(String targetUrl, int timeoutSec, Consumer<DetectResult> onResult, BiConsumer<String, LogConsole.Severity> onLog) {
        Platform.runLater(() -> BasedirDetector.doDetect(targetUrl, timeoutSec, onResult, onLog));
    }

    public static DetectResult detectSync(String targetUrl, int timeoutSec, BiConsumer<String, LogConsole.Severity> onLog) {
        AtomicReference ref = new AtomicReference();
        CountDownLatch latch = new CountDownLatch(1);
        BasedirDetector.detect(targetUrl, timeoutSec, r -> {
            ref.set(r);
            latch.countDown();
        }, onLog);
        try {
            latch.await((long)timeoutSec + 3L, TimeUnit.SECONDS);
        }
        catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        DetectResult r2 = (DetectResult)ref.get();
        if (r2 == null) {
            r2 = new DetectResult(targetUrl, "/prod-api", Collections.emptyList(), "\u63a2\u6d4b\u8d85\u65f6");
        }
        return r2;
    }

    private static void doDetect(String targetUrl, int timeoutSec, Consumer<DetectResult> onResult, BiConsumer<String, LogConsole.Severity> onLog) {
        WebView wv = new WebView();
        WebEngine engine = wv.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent(UA);
        boolean[] done = new boolean[]{false};
        boolean[] hookInjected = new boolean[]{false};
        engine.documentProperty().addListener((obs, o, n) -> {
            if (n != null && !hookInjected[0]) {
                BasedirDetector.injectHook(engine);
                hookInjected[0] = true;
                if (onLog != null) {
                    onLog.accept("[Basedir] \u5df2\u6ce8\u5165\u6293\u5305\u811a\u672c", LogConsole.Severity.INFO);
                }
            }
        });
        engine.getLoadWorker().exceptionProperty().addListener((obs, o, ex) -> {
            if (ex != null && onLog != null) {
                onLog.accept("[Basedir] \u52a0\u8f7d\u8b66\u544a: " + ex.getMessage(), LogConsole.Severity.WARNING);
            }
        });
        PauseTransition wait = new PauseTransition(Duration.seconds(timeoutSec));
        wait.setOnFinished(e -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            List<String> captured = BasedirDetector.collectUrls(engine);
            String basedir = BasedirDetector.chooseBasedir(captured);
            try {
                engine.load("about:blank");
            }
            catch (Exception exception) {
                // empty catch block
            }
            onResult.accept(new DetectResult(targetUrl, basedir, captured, null));
        });
        if (onLog != null) {
            onLog.accept("[Basedir] \u6d4f\u89c8\u5668\u52a0\u8f7d: " + targetUrl, LogConsole.Severity.INFO);
        }
        try {
            engine.load(targetUrl);
        }
        catch (Exception ex2) {
            done[0] = true;
            onResult.accept(new DetectResult(targetUrl, "/prod-api", Collections.emptyList(), "\u52a0\u8f7d\u5f02\u5e38: " + ex2.getMessage()));
            return;
        }
        wait.play();
    }

    private static void injectHook(WebEngine engine) {
        try {
            engine.executeScript("if(!window.__capturedUrls){window.__capturedUrls=[];var _o=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){  try{window.__capturedUrls.push(String(u));}catch(e){}  return _o.apply(this,arguments);};if(window.fetch){  var _f=window.fetch;  window.fetch=function(u,c){    try{window.__capturedUrls.push(typeof u==='string'?u:(u&&u.url));}catch(e){}    return _f.apply(this,arguments);  };}}");
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static List<String> collectUrls(WebEngine engine) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            Object lenObj = engine.executeScript("window.__capturedUrls?window.__capturedUrls.length:0");
            int len = lenObj instanceof Number ? ((Number)lenObj).intValue() : 0;
            for (int i = 0; i < len; ++i) {
                Object v = engine.executeScript("window.__capturedUrls[" + i + "]");
                if (v == null) continue;
                result.add(v.toString());
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return result;
    }

    private static String chooseBasedir(List<String> urls2) {
        for (String p : KNOWN_PREFIXES) {
            for (String u : urls2) {
                if (u == null || !u.contains(p + "/") && !u.endsWith(p)) continue;
                return p;
            }
        }
        LinkedHashMap<String, Integer> freq = new LinkedHashMap<String, Integer>();
        for (String u : urls2) {
            int s;
            String first;
            String path;
            if (u == null || u.isEmpty()) continue;
            try {
                if (u.startsWith("http://") || u.startsWith("https://")) {
                    path = new URL(u).getPath();
                } else {
                    if (!u.startsWith("/")) continue;
                    path = u;
                }
            }
            catch (Exception ex) {
                continue;
            }
            if (path == null || path.length() < 2 || (first = "/" + ((s = path.indexOf(47, 1)) > 0 ? path.substring(1, s) : path.substring(1))).matches("/(static|assets|js|css|img|images|fonts|favicon\\.ico|robots\\.txt)")) continue;
            freq.merge(first, 1, Integer::sum);
        }
        return freq.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("/prod-api");
    }

    public static class DetectResult {
        public final String targetUrl;
        public final String basedir;
        public final List<String> capturedUrls;
        public final String error;

        public DetectResult(String t, String b, List<String> u, String e) {
            this.targetUrl = t;
            this.basedir = b;
            this.capturedUrls = u == null ? Collections.emptyList() : u;
            this.error = e;
        }
    }
}

