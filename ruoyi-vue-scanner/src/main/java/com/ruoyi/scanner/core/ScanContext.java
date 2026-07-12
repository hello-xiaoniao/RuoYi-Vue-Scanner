/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.core;

import com.ruoyi.scanner.core.LogConsole;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class ScanContext {
    public final LogConsole log;
    public final ProgressBar progressBar;
    public final Label statusBar;
    public final AtomicBoolean scanning = new AtomicBoolean(false);
    public final Set<String> discoveredApis = new HashSet<String>();
    public volatile List<JsFile> jsFiles = new ArrayList<JsFile>();
    public final int timeoutSec = 15;
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "scanner-task");
        t.setDaemon(true);
        return t;
    });

    public ScanContext(LogConsole log, ProgressBar bar, Label status) {
        this.log = log;
        this.progressBar = bar;
        this.statusBar = status;
    }

    public boolean tryStart() {
        return this.scanning.compareAndSet(false, true);
    }

    public void stop() {
        this.scanning.set(false);
        this.setStatus("\u626b\u63cf\u5df2\u505c\u6b62");
    }

    public void finish(String text) {
        this.scanning.set(false);
        this.setProgress(1.0);
        this.setStatus(text);
    }

    public void setProgress(double v) {
        Platform.runLater(() -> this.progressBar.setProgress(v));
    }

    public void setProgressMax(int current, int total) {
        if (total <= 0) {
            return;
        }
        double v = Math.min(1.0, (double)current / (double)total);
        this.setProgress(v);
        this.setStatus("\u8fdb\u5ea6: " + current + "/" + total + " (" + (int)(v * 100.0) + "%)");
    }

    public void setStatus(String text) {
        Platform.runLater(() -> this.statusBar.setText(text));
    }

    public void runAsync(Runnable r) {
        this.taskExecutor.submit(() -> {
            try {
                r.run();
            }
            catch (Throwable t) {
                this.log.log("\u4efb\u52a1\u5f02\u5e38: " + t.getMessage(), LogConsole.Severity.CRITICAL);
            }
        });
    }

    public void shutdown() {
        this.scanning.set(false);
        this.taskExecutor.shutdownNow();
    }

    public static class JsFile {
        public final String url;
        public final String content;

        public JsFile(String url, String content) {
            this.url = url;
            this.content = content;
        }
    }
}

