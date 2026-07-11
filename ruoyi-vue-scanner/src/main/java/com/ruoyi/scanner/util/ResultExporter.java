package com.ruoyi.scanner.util;

import com.google.gson.GsonBuilder;
import com.ruoyi.scanner.core.LogEntry;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Export structured log entries to JSON / CSV / TXT. */
public class ResultExporter {

  private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public static void exportJson(List<LogEntry> entries, File file) throws IOException {
    ExportJson wrapper = new ExportJson();
    wrapper.exportTime = SDF.format(new Date());
    wrapper.total = entries.size();
    for (LogEntry e : entries) {
      ExportJson.Entry je = new ExportJson.Entry();
      je.timestamp = e.timestamp;
      je.time = SDF.format(new Date(e.timestamp));
      je.severity = e.severity.name();
      je.message = e.formatted;
      je.raw = e.raw;
      je.pure = e.pure;
      wrapper.entries.add(je);
    }
    String json = new GsonBuilder().setPrettyPrinting().create().toJson(wrapper);
    try (FileWriter w = new FileWriter(file, StandardCharsets.UTF_8)) {
      w.write(json);
    }
  }

  public static void exportCsv(List<LogEntry> entries, File file) throws IOException {
    try (PrintWriter w = new PrintWriter(file, "UTF-8")) {
      w.println("timestamp,time,severity,message,raw,pure");
      for (LogEntry e : entries) {
        w.print(e.timestamp);
        w.print(',');
        w.print(escapeCsv(SDF.format(new Date(e.timestamp))));
        w.print(',');
        w.print(e.severity.name());
        w.print(',');
        w.print(escapeCsv(e.formatted));
        w.print(',');
        w.print(escapeCsv(e.raw));
        w.print(',');
        w.print(e.pure);
        w.println();
      }
    }
  }

  public static void exportTxt(List<LogEntry> entries, File file) throws IOException {
    try (PrintWriter w = new PrintWriter(file, "UTF-8")) {
      for (LogEntry e : entries) {
        w.println(e.formatted);
      }
    }
  }

  private static String escapeCsv(String s) {
    if (s == null) return "";
    if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
  }

  // ── JSON wrapper (Gson serializes fields directly) ────────
  private static class ExportJson {
    String exportTime;
    int total;
    List<Entry> entries = new java.util.ArrayList<>();

    private static class Entry {
      long timestamp;
      String time;
      String severity;
      String message;
      String raw;
      boolean pure;
    }
  }
}