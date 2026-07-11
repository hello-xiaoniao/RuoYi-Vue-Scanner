package com.ruoyi.scanner.core;

/** A single structured log entry captured during scanning. */
public class LogEntry {
  public final long timestamp;
  public final LogConsole.Severity severity;
  /** The formatted message (with timestamp prefix if not pure) */
  public final String formatted;
  /** The raw message text (without any prefix) */
  public final String raw;
  /** Whether this entry was logged as "pure" (no timestamp prefix) */
  public final boolean pure;

  public LogEntry(long timestamp, LogConsole.Severity severity, String formatted, String raw, boolean pure) {
    this.timestamp = timestamp;
    this.severity = severity;
    this.formatted = formatted;
    this.raw = raw;
    this.pure = pure;
  }
}