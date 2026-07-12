/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class LogConsole {
    private static final String INIT_HTML = "<!DOCTYPE html><html><head><style>*{box-sizing:border-box;margin:0;padding:0;}html,body{height:100%;background:#1E293B;}body{color:#CBD5E1;font-family:Consolas,'Courier New',monospace;font-size:13px;line-height:1.5;padding:8px 10px;word-break:break-all;overflow-y:auto;}#log{display:block;}.row{display:block;white-space:pre-wrap;margin:0;padding:0;}.CRITICAL{color:#F87171;font-weight:bold;}.WARNING{color:#FBBF24;}.PATH{color:#60A5FA;}.PREVIEW{color:#FCD34D;}.RESPONSE_CONTENT{color:#FCA5A5;}.INFO{color:#CBD5E1;}</style></head><body><div id='log'></div><script>function append(cls,txt){  var d=document.getElementById('log');  var row=document.createElement('div');  row.className='row '+cls;  row.textContent=txt;  d.appendChild(row);  window.scrollTo(0,document.body.scrollHeight);  if(d.childElementCount>3000){d.removeChild(d.firstElementChild);}}function clearLog(){document.getElementById('log').innerHTML='';}</script></body></html>";
    private final WebView webView = new WebView();
    private final WebEngine engine;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final List<LogEntry> entries = Collections.synchronizedList(new ArrayList<>());

    public LogConsole() {
        this.engine = this.webView.getEngine();
        this.webView.getStyleClass().add("console-web");
        this.engine.loadContent(INIT_HTML);
    }

    public WebView getNode() {
        return this.webView;
    }

    public List<LogEntry> getEntries() {
        synchronized (entries) { return new ArrayList<>(entries); }
    }

    public void log(String message, Severity severity) {
        this.log(message, severity, false);
    }

    public void log(String message, Severity severity, boolean pure) {
        String raw = message;
        String text = pure ? message : "[" + this.sdf.format(new Date()) + "] [" + severity.name() + "] " + message;
        long now = System.currentTimeMillis();
        entries.add(new LogEntry(now, severity, text, raw, pure));
        String escaped = text.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", " ");
        String js = "append('" + severity.name() + "','" + escaped + "');";
        Platform.runLater(() -> {
            try {
                this.engine.executeScript(js);
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }

    public void clear() {
        synchronized (entries) { entries.clear(); }
        Platform.runLater(() -> {
            try {
                this.engine.executeScript("clearLog();");
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }

    public static enum Severity {
        INFO("#CBD5E1"),
        WARNING("#FBBF24"),
        CRITICAL("#F87171"),
        PATH("#60A5FA"),
        PREVIEW("#FCD34D"),
        RESPONSE_CONTENT("#FCA5A5");

        public final String color;

        private Severity(String color) {
            this.color = color;
        }
    }
}

