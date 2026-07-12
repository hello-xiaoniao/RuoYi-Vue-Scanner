/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.modules;

import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.ScanContext;
import com.ruoyi.scanner.poc.PocData.SensitivePoc.SensitiveRule;
import com.ruoyi.scanner.poc.PocLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveInfoCollector {
    private static List<Rule> RULES = null;

    public static void run(ScanContext ctx) {
        if (ctx.jsFiles == null || ctx.jsFiles.isEmpty()) {
            ctx.log.log("\u8bf7\u5148\u6267\u884cJS\u63a5\u53e3\u83b7\u53d6\uff01", LogConsole.Severity.WARNING);
            return;
        }
        ensureRules();
        ctx.log.log("\n[+] \u5f00\u59cb\u654f\u611f\u4fe1\u606f\u641c\u96c6...", LogConsole.Severity.CRITICAL, true);
        int total = ctx.jsFiles.size();
        int idx = 0;
        for (ScanContext.JsFile js : ctx.jsFiles) {
            if (!ctx.scanning.get()) break;
            ctx.setProgressMax(idx, total);
            ++idx;
            boolean found = false;
            for (Rule r : RULES) {
                try {
                    Matcher m = r.pattern.matcher(js.content);
                    while (m.find()) {
                        found = true;
                        String value = null;
                        for (int i = 1; i <= m.groupCount(); ++i) {
                            String g = m.group(i);
                            if (g == null) continue;
                            value = g;
                            break;
                        }
                        if (value == null) value = m.group();
                        String masked = value.length() > 4
                                ? value.substring(0, 2) + "*".repeat(value.length() - 4) + value.substring(value.length() - 2)
                                : value;
                        ctx.log.log("[!] " + r.name + " \u53d1\u73b0\u4e8e " + js.url + ":\n    " + masked + " (\u539f\u59cb\u503c: " + value + ")", LogConsole.Severity.CRITICAL);
                    }
                } catch (Exception e) {
                    ctx.log.log("\u6b63\u5219\u5339\u914d\u5f02\u5e38 [" + r.name + "]: " + e.getMessage(), LogConsole.Severity.WARNING);
                }
            }
            if (!found) {
                ctx.log.log("[\u221a] " + js.url + " \u672a\u53d1\u73b0\u654f\u611f\u4fe1\u606f", LogConsole.Severity.INFO);
            }
        }
    }

    /** Reload rules from POC config. Call after poc-reload. */
    public static synchronized void reloadRules() {
        RULES = null;
    }

    private static void ensureRules() {
        if (RULES != null) return;
        List<Rule> list = new ArrayList<>();
        for (SensitiveRule sr : PocLoader.get().sensitive().rules) {
            list.add(new Rule(sr.name, Pattern.compile(sr.pattern)));
        }
        RULES = list;
    }

    private static class Rule {
        final String name;
        final Pattern pattern;
        Rule(String name, Pattern pattern) { this.name = name; this.pattern = pattern; }
    }
}

