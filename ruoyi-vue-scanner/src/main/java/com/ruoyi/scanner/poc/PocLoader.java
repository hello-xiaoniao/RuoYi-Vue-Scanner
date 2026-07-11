package com.ruoyi.scanner.poc;

import com.google.gson.Gson;
import com.ruoyi.scanner.poc.PocData.BasicPoc;
import com.ruoyi.scanner.poc.PocData.DruidPoc;
import com.ruoyi.scanner.poc.PocData.SensitivePoc;
import com.ruoyi.scanner.poc.PocData.SwaggerPoc;
import com.ruoyi.scanner.poc.PocData.SystemApiPoc;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Singleton loader for POC JSON files.
 *
 * Finds {@code <jarDir>/poc/} at runtime. If a file is missing it is
 * created from the embedded default under {@code /poc-default/*.json}.
 * Call {@link #reload()} after editing files to pick up changes.
 */
public class PocLoader {

  private static final String[] POC_FILES = {
    "swagger.json", "druid.json", "basic.json",
    "system_api.json", "sensitive.json"
  };

  private static final Gson GSON = new Gson();

  private static volatile PocLoader instance;

  private final File pocDir;
  private SwaggerPoc swagger;
  private DruidPoc druid;
  private BasicPoc basic;
  private SystemApiPoc systemApi;
  private SensitivePoc sensitive;

  // ── init ──────────────────────────────────────────────────

  private PocLoader(File pocDir) {
    this.pocDir = pocDir;
    ensureDir();
    ensureFiles();
    reload();
  }

  /** Bootstrap – call once at app startup */
  public static synchronized PocLoader init(File jarLocation) {
    if (instance != null) throw new IllegalStateException("PocLoader already initialized");
    File dir = new File(jarLocation, "poc");
    instance = new PocLoader(dir);
    return instance;
  }

  public static PocLoader get() {
    if (instance == null) throw new IllegalStateException("PocLoader not initialized");
    return instance;
  }

  // ── public accessors ──────────────────────────────────────

  public SwaggerPoc swagger()   { return swagger; }
  public DruidPoc druid()       { return druid; }
  public BasicPoc basic()       { return basic; }
  public SystemApiPoc systemApi() { return systemApi; }
  public SensitivePoc sensitive() { return sensitive; }

  public synchronized void reload() {
    this.swagger   = load("swagger.json",    SwaggerPoc.class);
    this.druid     = load("druid.json",      DruidPoc.class);
    this.basic     = load("basic.json",      BasicPoc.class);
    this.systemApi = load("system_api.json", SystemApiPoc.class);
    this.sensitive = load("sensitive.json",  SensitivePoc.class);
  }

  /** The path to the poc/ directory, for "open folder" buttons */
  public File pocDirectory() { return pocDir; }

  // ── internals ─────────────────────────────────────────────

  private void ensureDir() {
    if (!pocDir.isDirectory()) pocDir.mkdirs();
  }

  private void ensureFiles() {
    for (String name : POC_FILES) {
      File target = new File(pocDir, name);
      if (target.isFile()) continue;
      String resourcePath = "/poc-default/" + name;
      try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
        if (in == null) {
          System.err.println("[PocLoader] Embedded default not found: " + resourcePath);
          continue;
        }
        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[PocLoader] Released default: " + target.getAbsolutePath());
      } catch (IOException e) {
        System.err.println("[PocLoader] Failed to write " + target + ": " + e.getMessage());
      }
    }
  }

  private <T> T load(String filename, Class<T> clazz) {
    File file = new File(pocDir, filename);
    if (!file.isFile()) return newInstance(clazz);
    try {
      byte[] bytes = Files.readAllBytes(file.toPath());
      T parsed = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
      if (parsed == null) return newInstance(clazz);
      return parsed;
    } catch (Exception e) {
      System.err.println("[PocLoader] Failed to parse " + filename + ": " + e.getMessage());
      return newInstance(clazz);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T newInstance(Class<T> clazz) {
    try {
      return clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      // If all else fails, return a default SwaggerPoc
      return (T) new PocData.SwaggerPoc();
    }
  }
}