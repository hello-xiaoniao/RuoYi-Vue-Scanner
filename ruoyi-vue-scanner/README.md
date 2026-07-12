- # RuoYi-Vue Scanner

  若依 Vue 前后端分离版漏洞检测工具。支持 POC 外置热更新和结果导出。

  ## 直接使用（推荐）

  从 [Releases](https://github.com/你的用户名/你的仓库名/releases) 下载 `ruoyi-vue-scanner.jar`，**确保系统已安装 JDK 17+**，双击即可运行 GUI 界面。

  首次运行会自动在 jar 同级创建 `poc/` 目录并释放默认 POC JSON 文件。

  ## 从源码构建

  ### 环境要求

  - JDK 17+
  - 无需安装 Maven（自带 mvnw 包装器）

  ```bash
  # Linux/macOS
  ./mvnw package -DskipTests
  
  # Windows
  mvnw.cmd package -DskipTests
  ```

  构建产物在 `target/ruoyi-vue-scanner-7.1.0.jar`

  ## 原作者

  本工具基于 [AakiTT/ruoyi-vue-scanner](https://github.com/AakiTT/ruoyi-vue-scanner) 二开改造。

  ### 原版特性

  - 若依 Vue 框架的自动化漏洞检测
  - 支持 Swagger / Druid / SQL 注入 / Quartz RCE 等常见漏洞
  - 自动 Basedir 探测

  ### 二开新增特性

  - ✅ **结果导出** — 支持 JSON / CSV / TXT 三种格式
  - ✅ **POC 外置热更新** — `poc/` 目录下 JSON 文件，修改后无需重启，点「重载POC」即时生效
  - ✅ **结构化日志** — 每条日志带时间戳/级别/原文，导出保留完整信息
  - ✅ 消除反编译代码异味（Object 类型、泛型错乱等）
  - ✅ 去掉启动弹窗免责声明和界面原作者 GitHub 链接
  - ✅ 补充 2026 年最新 RuoYi CVEs（CVE-2026-37669、CVE-2026-57950 等）到 POC 库
  - ✅ 覆盖 RuoYi-Vue-Pro 版特有漏洞（Mock Token 绕过、路径遍历、ORDER BY SQL 注入、IoT SQL 注入、文件越权 IDOR）

  ## 功能

  ### 漏洞检测

  - **Swagger** — Swagger / knife4j API 文档泄露检测
  - **Druid** — Druid 监控页面发现 + 弱口令爆破
  - **文件下载** — 任意文件读取漏洞检测（路径遍历）
  - **SQL 注入** — orderByColumn / dataScope 注入检测
  - **定时任务** — Quartz RCE 检测
  - **密码重置** — 任意密码修改检测
  - **系统接口越权** — 越权访问批量检测

  ### 接口与情报

  - **JS 接口收集** — 自动抓取页面 JS 文件提取 API 路径
  - **接口测试** — 探测 API 可用性
  - **敏感信息搜集** — 正则匹配密码、Token、AK/SK 等敏感信息

  ### 批量扫描

  - 支持多目标 URL 批量导入检测
  - 自动提取 baseApi
  - 按目标串行执行全量检测

  ### 导出

  - 扫描结果导出为 JSON / CSV / TXT
  <img width="1242" height="165" alt="12bf89d8-5f2c-4371-97b7-772ed2018d61" src="https://github.com/user-attachments/assets/f703e0f8-ef96-4565-96e2-b15f18f9ea1c" />

  

  ## POC 配置

  `poc/` 目录结构：

  ```
  poc/
  ├── swagger.json       # Swagger 检测路径/特征/排除
  ├── druid.json         # Druid 检测路径/弱口令列表
  ├── basic.json         # 文件下载/SQL注入/定时任务/密码重置
  ├── system_api.json    # 系统接口路径
  └── sensitive.json     # 敏感信息正则规则
  ```

  编辑任一 JSON 后点界面「重载POC」按钮立即生效，无需重启。

  ## 注意

  仅用于授权安全测试。请遵守相关法律法规。