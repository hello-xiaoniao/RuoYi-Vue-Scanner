/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.ui;

import com.ruoyi.scanner.core.BasedirDetector;
import com.ruoyi.scanner.core.LogConsole;
import com.ruoyi.scanner.core.ScanConfig;
import com.ruoyi.scanner.core.ScanContext;
import com.ruoyi.scanner.modules.ApiTester;
import com.ruoyi.scanner.modules.BasicVulnChecks;
import com.ruoyi.scanner.modules.DruidCheck;
import com.ruoyi.scanner.modules.JsApiCollector;
import com.ruoyi.scanner.modules.SensitiveInfoCollector;
import com.ruoyi.scanner.modules.SwaggerCheck;
import com.ruoyi.scanner.modules.SystemApiCheck;
import com.ruoyi.scanner.poc.PocLoader;
import com.ruoyi.scanner.util.ResultExporter;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

public class MainView {
    private final BorderPane root = new BorderPane();
    private final TextField tfUrl = new TextField();
    private final TextField tfCookie = new TextField();
    private final TextField tfAuth = new TextField();
    private final TextField tfProxy = new TextField();
    private final TextField tfBaseApi = new TextField("/prod-api");
    private final ComboBox<String> cbMethod = new ComboBox();
    private final TextArea taBody = new TextArea();
    private VBox bodyRow;
    private final ProgressBar progressBar = new ProgressBar(0.0);
    private final Label statusText = new Label("\u5c31\u7eea");
    private final Circle statusDot = new Circle(5.0);
    private final LogConsole logConsole = new LogConsole();
    private final List<String> batchUrls = new ArrayList<String>();
    private final CheckBox cbAutoBasedir = new CheckBox("\u81ea\u52a8\u63d0\u53d6Basedir");
    private boolean syncingBasedir = false;
    private final ScanContext ctx = new ScanContext(this.logConsole, this.progressBar, this.statusText);
    private static final String[] RUOYI_API_PREFIXES = new String[]{"prod-api", "dev-api", "stage-api", "test-api", "api"};

    public MainView() {
        this.root.getStyleClass().add("root");
        this.root.setTop(this.buildHeader());
        this.root.setCenter(this.buildCenter());
        this.root.setBottom(this.buildStatusBar());
        Runnable syncBasedir = () -> {
            if (this.syncingBasedir) {
                return;
            }
            if (!this.cbAutoBasedir.isSelected() || this.tfUrl.getText().trim().isEmpty()) {
                return;
            }
            String raw = this.tfUrl.getText().trim();
            String basedir = this.extractBasedir(raw);
            String cleanUrl = this.extractCleanBaseUrl(raw);
            this.syncingBasedir = true;
            try {
                this.tfBaseApi.setText(basedir);
                if (!this.tfUrl.getText().trim().equals(cleanUrl)) {
                    this.tfUrl.setText(cleanUrl);
                }
            }
            finally {
                this.syncingBasedir = false;
            }
        };
        this.tfUrl.textProperty().addListener((obs, o, n) -> syncBasedir.run());
        this.cbAutoBasedir.selectedProperty().addListener((obs, o, n) -> syncBasedir.run());
    }

    private HBox buildHeader() {
        Label title = new Label("RuoYi-Vue Scanner");
        title.getStyleClass().add("header-title");
        Label sub = new Label("\u82e5\u4f9dVue\u6f0f\u6d1e\u68c0\u6d4b\u5de5\u5177");
        sub.getStyleClass().add("header-sub");
        VBox text = new VBox(2.0, title, sub);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox bar = new HBox(12.0, text);
        bar.getStyleClass().add("header-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private VBox buildCenter() {
        VBox content = new VBox(14.0);
        content.setPadding(new Insets(16.0, 18.0, 12.0, 18.0));
        VBox configCard = this.buildConfigCard();
        VBox actionCard = this.buildActionCard();
        VBox resultCard = this.buildResultCard();
        VBox.setVgrow(resultCard, Priority.ALWAYS);
        content.getChildren().addAll((Node[])new Node[]{configCard, actionCard, resultCard});
        return content;
    }

    private VBox buildConfigCard() {
        VBox card = new VBox(10.0);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        Label title = new Label("\u2699  \u626b\u63cf\u914d\u7f6e");
        title.getStyleClass().add("card-title");
        GridPane g = new GridPane();
        g.setHgap(10.0);
        g.setVgap(8.0);
        ColumnConstraints lc = new ColumnConstraints();
        lc.setMinWidth(80.0);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        fc.setFillWidth(true);
        ColumnConstraints lc2 = new ColumnConstraints();
        lc2.setMinWidth(110.0);
        ColumnConstraints fc2 = new ColumnConstraints();
        fc2.setHgrow(Priority.ALWAYS);
        fc2.setFillWidth(true);
        g.getColumnConstraints().addAll((ColumnConstraints[])new ColumnConstraints[]{lc, fc, lc2, fc2});
        this.tfUrl.setPromptText("https://target.example.com");
        this.tfCookie.setPromptText("session=...; admin=...");
        this.tfAuth.setPromptText("Bearer eyJ...");
        this.tfProxy.setPromptText("127.0.0.1:8080");
        this.tfBaseApi.setPromptText("/prod-api");
        Label l = new Label("\u76ee\u6807URL:");
        l.getStyleClass().add("field-label");
        l.setMinWidth(Double.NEGATIVE_INFINITY);
        Button btnImport = this.btn("\u6279\u91cf\u5bfc\u5165", "btn-neutral", this::onBatchImport);
        HBox urlBox = new HBox(6.0, this.tfUrl, btnImport);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this.tfUrl, Priority.ALWAYS);
        urlBox.setMaxWidth(Double.MAX_VALUE);
        g.add(l, 0, 0);
        g.add(urlBox, 1, 0);
        GridPane.setHgrow(urlBox, Priority.ALWAYS);
        this.addCol(g, 0, 2, "Authorization:", this.tfAuth);
        this.addCol(g, 1, 0, "Cookie:", this.tfCookie);
        this.addCol(g, 1, 2, "HTTP\u4ee3\u7406:", this.tfProxy);
        Label lblBase = new Label("\u57fa\u7840\u63a5\u53e3\u8def\u5f84:");
        lblBase.getStyleClass().add("field-label");
        Label lblMethod = new Label("\u8bf7\u6c42\u65b9\u6cd5:");
        lblMethod.getStyleClass().add("field-label");
        this.cbMethod.getItems().addAll((String[])new String[]{"GET", "POST", "POST-JSON"});
        this.cbMethod.setValue("GET");
        this.cbMethod.setPrefWidth(140.0);
        this.cbMethod.valueProperty().addListener((obs, o, n) -> this.toggleBodyRow());
        HBox row3 = new HBox(10.0);
        row3.setAlignment(Pos.CENTER_LEFT);
        this.cbAutoBasedir.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        HBox left = new HBox(8.0, lblBase, this.tfBaseApi, this.cbAutoBasedir);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this.tfBaseApi, Priority.ALWAYS);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox right = new HBox(8.0, lblMethod, this.cbMethod);
        right.setAlignment(Pos.CENTER_LEFT);
        row3.getChildren().addAll((Node[])new Node[]{left, right});
        Label lblBody = new Label("\u8bf7\u6c42\u4f53:");
        lblBody.getStyleClass().add("field-label");
        this.taBody.setPrefRowCount(3);
        this.taBody.setWrapText(true);
        this.taBody.setPromptText("POST/POST-JSON \u8bf7\u6c42\u4f53\u5185\u5bb9");
        this.bodyRow = new VBox(6.0, lblBody, this.taBody);
        this.bodyRow.setVisible(false);
        this.bodyRow.setManaged(false);
        card.getChildren().addAll((Node[])new Node[]{title, g, row3, this.bodyRow});
        return card;
    }

    private void addCol(GridPane g, int row, int colStart, String labelText, Control field) {
        Label l = new Label(labelText);
        l.getStyleClass().add("field-label");
        l.setMinWidth(Double.NEGATIVE_INFINITY);
        g.add(l, colStart, row);
        g.add(field, colStart + 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof Region) {
            field.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void toggleBodyRow() {
        boolean show = !"GET".equals(this.cbMethod.getValue());
        this.bodyRow.setVisible(show);
        this.bodyRow.setManaged(show);
    }

    private VBox buildActionCard() {
        VBox card = new VBox(10.0);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        Label title = new Label("\ud83d\udd0d  \u6f0f\u6d1e\u6a21\u5757");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleBar = new HBox(8.0, title, spacer,
                this.btn("\u91cd\u8f7dPOC", "btn-neutral", this::onReloadPoc),
                this.btn("POC\u76ee\u5f55", "btn-neutral", this::onOpenPocDir));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        FlowPane row1 = new FlowPane(8.0, 8.0);
        row1.getChildren().addAll((Node[])new Node[]{this.groupLabel("\u6f0f\u6d1e\u68c0\u6d4b"), this.btn("Swagger", "btn-danger", this::onSwagger), this.btn("Druid", "btn-danger", this::onDruid), this.btn("\u6587\u4ef6\u8bfb\u53d6", "btn-danger", this::onFileDownload), this.btn("SQL\u6ce8\u5165", "btn-danger", this::onSqlInjection), this.btn("\u5b9a\u65f6\u4efb\u52a1", "btn-danger", this::onScheduledTask), this.btn("\u4efb\u610f\u5bc6\u7801\u4fee\u6539", "btn-danger", this::onPasswordReset), this.btn("\u7cfb\u7edf\u63a5\u53e3\u8d8a\u6743", "btn-danger", this::onSystemApi)});
        FlowPane row2 = new FlowPane(8.0, 8.0);
        row2.getChildren().addAll((Node[])new Node[]{this.groupLabel("\u63a5\u53e3\u4e0e\u60c5\u62a5"), this.btn("\u83b7\u53d6JS\u63a5\u53e3", "btn-primary", this::onCollectJsApis), this.btn("\u63a5\u53e3\u6d4b\u8bd5", "btn-primary", this::onApiTest), this.btn("\u654f\u611f\u4fe1\u606f\u641c\u96c6", "btn-primary", this::onSensitiveInfo), this.btn("\u5168\u9762\u68c0\u6d4b", "btn-success", this::onFullScan)});
        FlowPane row3 = new FlowPane(8.0, 8.0);
        row3.getChildren().addAll((Node[])new Node[]{this.groupLabel("\u6279\u91cf\u626b\u63cf"), this.btn("\u4e00\u952e\u68c0\u6d4b", "btn-success", this::onBatchScan)});
        card.getChildren().addAll((Node[])new Node[]{titleBar, row1, row2, row3});
        return card;
    }

    private Label groupLabel(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("group-label");
        l.setMinWidth(Double.NEGATIVE_INFINITY);
        l.setPadding(new Insets(6.0, 4.0, 6.0, 0.0));
        return l;
    }

    private Button btn(String text, String styleClass, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().addAll((String[])new String[]{"btn", styleClass});
        b.setOnAction(e -> action.run());
        return b;
    }

    private VBox buildResultCard() {
        VBox card = new VBox(8.0);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14.0, 16.0, 14.0, 16.0));
        Label title = new Label("\ud83d\udccb  \u626b\u63cf\u7ed3\u679c");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleBar = new HBox(8.0, title, spacer,
                this.btn("\u5bfc\u51fa\u7ed3\u679c", "btn-neutral", this::onExport),
                this.btn("\u6e05\u7a7a\u7ed3\u679c", "btn-neutral", this::onClear),
                this.btn("\u505c\u6b62\u626b\u63cf", "btn-warning", this::onStop));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        Label progressLabel = new Label("0%");
        progressLabel.getStyleClass().add("card-subtitle");
        progressLabel.setMinWidth(40.0);
        this.progressBar.progressProperty().addListener((obs, o, n) -> progressLabel.setText(String.format("%d%%", (int)(n.doubleValue() * 100.0))));
        this.progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this.progressBar, Priority.ALWAYS);
        HBox progressBox = new HBox(10.0, this.progressBar, progressLabel);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(this.logConsole.getNode(), Priority.ALWAYS);
        this.logConsole.getNode().setPrefHeight(360.0);
        card.getChildren().addAll((Node[])new Node[]{titleBar, progressBox, this.logConsole.getNode()});
        VBox.setVgrow(this.logConsole.getNode(), Priority.ALWAYS);
        return card;
    }

    private HBox buildStatusBar() {
        this.statusDot.getStyleClass().add("status-dot");
        this.statusText.getStyleClass().add("status-bar");
        this.statusText.setPadding(Insets.EMPTY);
        this.statusText.setStyle("-fx-text-fill: #E5E7EB; -fx-padding: 0;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hint = new Label("\u4ec5\u4f9b\u5408\u6cd5\u6388\u6743\u6d4b\u8bd5  \u00b7  \u8bf7\u9075\u5b88\u76f8\u5173\u6cd5\u5f8b\u6cd5\u89c4");
        hint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        HBox bar = new HBox(8.0, this.statusDot, this.statusText, spacer, hint);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void setBusy(boolean busy) {
        Platform.runLater(() -> {
            this.statusDot.getStyleClass().removeAll((String[])new String[]{"status-dot", "status-dot-busy"});
            this.statusDot.getStyleClass().add(busy ? "status-dot-busy" : "status-dot");
        });
    }

    private ScanConfig currentConfig() {
        return new ScanConfig(this.tfUrl.getText(), this.tfCookie.getText(), this.tfAuth.getText(), this.tfProxy.getText(), this.tfBaseApi.getText());
    }

    private ScanConfig waitForBasedir(ScanConfig cfg, boolean autoBase) {
        if (!autoBase) {
            return cfg;
        }
        this.ctx.log.log("[Basedir] \u626b\u63cf\u524d\u540c\u6b65\u63a2\u6d4b\u4e2d\u8bf7\u7a0d\u5019...", LogConsole.Severity.WARNING);
        BasedirDetector.DetectResult r = BasedirDetector.detectSync(cfg.url, 6, (m, s) -> this.ctx.log.log((String)m, (LogConsole.Severity)((Object)s)));
        String detected = r.basedir != null ? r.basedir : cfg.baseApi;
        for (String u : r.capturedUrls) {
            this.ctx.log.log("  \u2190 " + u, LogConsole.Severity.PATH);
        }
        this.ctx.log.log("[Basedir] \u4f7f\u7528: " + detected, LogConsole.Severity.CRITICAL);
        Platform.runLater(() -> this.tfBaseApi.setText(detected));
        return new ScanConfig(cfg.url, cfg.cookie, cfg.authorization, cfg.proxy, detected);
    }

    private boolean acquire() {
        if (this.tfUrl.getText() == null || this.tfUrl.getText().trim().isEmpty()) {
            this.showWarn("\u8bf7\u586b\u5199\u76ee\u6807URL");
            return false;
        }
        if (!this.ctx.tryStart()) {
            this.showWarn("\u5df2\u6709\u626b\u63cf\u4efb\u52a1\u6b63\u5728\u8fdb\u884c\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\uff01");
            return false;
        }
        this.setBusy(true);
        return true;
    }

    private void done(String msg) {
        this.ctx.finish(msg);
        this.setBusy(false);
    }

    private void runModule(String taskName, BiConsumer<ScanContext, ScanConfig> runner) {
        if (!this.batchUrls.isEmpty()) {
            if (!this.ctx.tryStart()) {
                this.showWarn("\u5df2\u6709\u626b\u63cf\u4efb\u52a1\u6b63\u5728\u8fdb\u884c\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\uff01");
                return;
            }
            ArrayList<String> urls2 = new ArrayList<String>(this.batchUrls);
            String cookie = this.tfCookie.getText();
            String auth = this.tfAuth.getText();
            String proxy = this.tfProxy.getText();
            String manualBase = this.tfBaseApi.getText();
            boolean autoBase = this.cbAutoBasedir.isSelected();
            this.setBusy(true);
            this.logConsole.clear();
            this.ctx.setStatus("\u6279\u91cf" + taskName + "\u4e2d...");
            this.ctx.runAsync(() -> {
                int total = urls2.size();
                for (int i = 0; i < total && this.ctx.scanning.get(); ++i) {
                    int idx = i;
                    String url = (String)urls2.get(idx);
                    String cleanUrl = autoBase ? this.extractCleanBaseUrl(url) : url;
                    String detected = manualBase;
                    if (autoBase) {
                        BasedirDetector.DetectResult r = BasedirDetector.detectSync(cleanUrl, 6, (m, s) -> this.ctx.log.log((String)m, (LogConsole.Severity)((Object)((Object)s))));
                        detected = r.basedir != null ? r.basedir : this.extractBasedir(url);
                    }
                    String base = detected;
                    Platform.runLater(() -> {
                        this.tfUrl.setText(cleanUrl);
                        this.tfBaseApi.setText(base);
                    });
                    this.ctx.log.log("===== [" + (idx + 1) + "/" + total + "] " + cleanUrl + " =====", LogConsole.Severity.WARNING, true);
                    this.ctx.setProgressMax(idx, total);
                    ScanConfig cfg = new ScanConfig(cleanUrl, cookie, auth, proxy, base);
                    try {
                        runner.accept(this.ctx, cfg);
                        continue;
                    }
                    catch (Exception e) {
                        this.ctx.log.log("[!] " + url + " \u5f02\u5e38: " + e.getMessage(), LogConsole.Severity.CRITICAL);
                    }
                }
                this.done(taskName + " \u6279\u91cf\u5b8c\u6210\uff0c\u5171 " + total + " \u4e2a\u76ee\u6807");
            });
        } else {
            if (!this.acquire()) {
                return;
            }
            ScanConfig baseCfg = this.currentConfig();
            boolean autoBase = this.cbAutoBasedir.isSelected();
            this.ctx.runAsync(() -> {
                ScanConfig cfg = this.waitForBasedir(baseCfg, autoBase);
                runner.accept(this.ctx, cfg);
                this.done(taskName + " \u5b8c\u6210");
            });
        }
    }

    private void showWarn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("\u63d0\u793a");
        a.showAndWait();
    }

    private void onSwagger() {
        this.runModule("Swagger\u68c0\u6d4b", SwaggerCheck::run);
    }

    private void onDruid() {
        this.runModule("Druid\u68c0\u6d4b", DruidCheck::run);
    }

    private void onFileDownload() {
        this.runModule("\u6587\u4ef6\u8bfb\u53d6\u68c0\u6d4b", (c, f) -> BasicVulnChecks.runFileDownload(c, f));
    }

    private void onSqlInjection() {
        this.runModule("SQL\u6ce8\u5165\u68c0\u6d4b", (c, f) -> BasicVulnChecks.runSqlInjection(c, f));
    }

    private void onScheduledTask() {
        this.runModule("\u5b9a\u65f6\u4efb\u52a1\u68c0\u6d4b", (c, f) -> BasicVulnChecks.runScheduledTask(c, f));
    }

    private void onPasswordReset() {
        this.runModule("\u4efb\u610f\u5bc6\u7801\u4fee\u6539\u68c0\u6d4b", (c, f) -> BasicVulnChecks.runPasswordReset(c, f));
    }

    private void onSystemApi() {
        this.runModule("\u7cfb\u7edf\u63a5\u53e3\u6d4b\u8bd5", SystemApiCheck::run);
    }

    private void onCollectJsApis() {
        this.runModule("JS\u63a5\u53e3\u6536\u96c6", JsApiCollector::run);
    }

    private void onApiTest() {
        if (this.ctx.discoveredApis.isEmpty()) {
            this.showWarn("\u8bf7\u5148\u6267\u884c [\u83b7\u53d6JS\u63a5\u53e3] \uff01");
            return;
        }
        if (!this.acquire()) {
            return;
        }
        ScanConfig baseCfg = this.currentConfig();
        boolean autoBase = this.cbAutoBasedir.isSelected();
        String method = (String)this.cbMethod.getValue();
        String body = this.taBody.getText();
        this.ctx.runAsync(() -> {
            ScanConfig cfg = this.waitForBasedir(baseCfg, autoBase);
            ApiTester.run(this.ctx, cfg, method, body);
            this.done("\u63a5\u53e3\u6d4b\u8bd5\u5b8c\u6210");
        });
    }

    private void onSensitiveInfo() {
        if (this.ctx.jsFiles == null || this.ctx.jsFiles.isEmpty()) {
            this.showWarn("\u8bf7\u5148\u6267\u884c [\u83b7\u53d6JS\u63a5\u53e3] \uff01");
            return;
        }
        if (!this.acquire()) {
            return;
        }
        this.ctx.runAsync(() -> {
            SensitiveInfoCollector.run(this.ctx);
            this.done("\u654f\u611f\u4fe1\u606f\u641c\u96c6\u5b8c\u6210");
        });
    }

    private void onFullScan() {
        if (!this.batchUrls.isEmpty()) {
            this.onBatchScan();
            return;
        }
        if (!this.acquire()) {
            return;
        }
        ScanConfig baseCfg = this.currentConfig();
        boolean autoBase = this.cbAutoBasedir.isSelected();
        this.logConsole.clear();
        this.ctx.setStatus("\u6b63\u5728\u6267\u884c\u5168\u9762\u68c0\u6d4b...");
        this.ctx.runAsync(() -> {
            ScanConfig cfg = this.waitForBasedir(baseCfg, autoBase);
            Runnable[] steps = new Runnable[]{() -> SwaggerCheck.run(this.ctx, cfg), () -> DruidCheck.run(this.ctx, cfg), () -> BasicVulnChecks.runFileDownload(this.ctx, cfg), () -> BasicVulnChecks.runSqlInjection(this.ctx, cfg), () -> BasicVulnChecks.runScheduledTask(this.ctx, cfg), () -> BasicVulnChecks.runPasswordReset(this.ctx, cfg)};
            for (int i = 0; i < steps.length && this.ctx.scanning.get(); ++i) {
                this.ctx.setProgressMax(i, steps.length);
                steps[i].run();
            }
            this.done("\u5168\u9762\u68c0\u6d4b\u5b8c\u6210");
        });
    }

    private void onClear() {
        this.logConsole.clear();
        this.progressBar.setProgress(0.0);
        this.ctx.setStatus("\u5c31\u7eea");
    }

    private void onStop() {
        this.ctx.stop();
        this.setBusy(false);
    }

    private void onBatchImport() {
        TextArea ta = new TextArea();
        ta.setPromptText("\u6bcf\u884c\u4e00\u4e2aURL\uff0c\u4f8b\u5982\uff1a\nhttp://192.168.1.1:8080\nhttp://192.168.1.2:8080");
        ta.setPrefRowCount(12);
        ta.setPrefWidth(520.0);
        if (!this.batchUrls.isEmpty()) {
            ta.setText(String.join((CharSequence)"\n", this.batchUrls));
        }
        DialogPane pane = new DialogPane();
        pane.setContent(ta);
        pane.getButtonTypes().addAll((ButtonType[])new ButtonType[]{ButtonType.OK, ButtonType.CANCEL});
        pane.setHeaderText("\u6279\u91cf\u5bfc\u5165URL\uff08\u6bcf\u884c\u4e00\u4e2a\uff09");
        Dialog dialog = new Dialog();
        dialog.setTitle("\u6279\u91cf\u5bfc\u5165URL");
        dialog.setDialogPane(pane);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                this.batchUrls.clear();
                Arrays.stream(ta.getText().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).forEach(this.batchUrls::add);
                if (!this.batchUrls.isEmpty()) {
                    this.tfUrl.setText(this.batchUrls.get(0));
                }
            }
        });
    }

    private void onBatchScan() {
        if (this.batchUrls.isEmpty()) {
            this.showWarn("\u8bf7\u5148\u70b9\u51fb\u3010\u6279\u91cf\u5bfc\u5165\u3011\u5bfc\u5165URL\u5217\u8868\uff01");
            return;
        }
        if (!this.ctx.tryStart()) {
            this.showWarn("\u5df2\u6709\u626b\u63cf\u4efb\u52a1\u6b63\u5728\u8fdb\u884c\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\uff01");
            return;
        }
        ArrayList<String> urls2 = new ArrayList<String>(this.batchUrls);
        String cookie = this.tfCookie.getText();
        String auth = this.tfAuth.getText();
        String proxy = this.tfProxy.getText();
        String manualBase = this.tfBaseApi.getText();
        boolean autoBase = this.cbAutoBasedir.isSelected();
        this.setBusy(true);
        this.logConsole.clear();
        this.ctx.setStatus("\u6279\u91cf\u68c0\u6d4b\u8fdb\u884c\u4e2d...");
        this.ctx.runAsync(() -> {
            int total = urls2.size();
            for (int i = 0; i < total && this.ctx.scanning.get(); ++i) {
                int idx = i;
                String url = (String)urls2.get(idx);
                String cleanUrl = autoBase ? this.extractCleanBaseUrl(url) : url;
                String detected = manualBase;
                if (autoBase) {
                    BasedirDetector.DetectResult r = BasedirDetector.detectSync(cleanUrl, 6, (m, s) -> this.ctx.log.log((String)m, (LogConsole.Severity)((Object)((Object)s))));
                    detected = r.basedir != null ? r.basedir : this.extractBasedir(url);
                }
                String base = detected;
                Platform.runLater(() -> {
                    this.tfUrl.setText(cleanUrl);
                    this.tfBaseApi.setText(base);
                });
                this.ctx.log.log("========== [" + (idx + 1) + "/" + total + "] \u5f00\u59cb\u626b\u63cf: " + cleanUrl + " ==========", LogConsole.Severity.WARNING, true);
                this.ctx.setProgressMax(idx, total);
                ScanConfig cfg = new ScanConfig(cleanUrl, cookie, auth, proxy, base);
                try {
                    SwaggerCheck.run(this.ctx, cfg);
                    if (!this.ctx.scanning.get()) break;
                    DruidCheck.run(this.ctx, cfg);
                    if (!this.ctx.scanning.get()) break;
                    BasicVulnChecks.runFileDownload(this.ctx, cfg);
                    if (!this.ctx.scanning.get()) break;
                    BasicVulnChecks.runSqlInjection(this.ctx, cfg);
                    if (!this.ctx.scanning.get()) break;
                    BasicVulnChecks.runScheduledTask(this.ctx, cfg);
                    if (!this.ctx.scanning.get()) break;
                    BasicVulnChecks.runPasswordReset(this.ctx, cfg);
                }
                catch (Exception e) {
                    this.ctx.log.log("[!] \u76ee\u6807 " + url + " \u626b\u63cf\u5f02\u5e38: " + e.getMessage(), LogConsole.Severity.CRITICAL);
                }
                this.ctx.log.log("========== [" + (idx + 1) + "/" + total + "] \u626b\u63cf\u5b8c\u6bd5: " + url + " ==========", LogConsole.Severity.WARNING, true);
            }
            this.done("\u6279\u91cf\u68c0\u6d4b\u5b8c\u6210\uff0c\u5171 " + total + " \u4e2a\u76ee\u6807");
        });
    }

    private void onExport() {
        List<com.ruoyi.scanner.core.LogEntry> entries = this.logConsole.getEntries();
        if (entries.isEmpty()) {
            this.showWarn("\u6ca1\u6709\u53ef\u5bfc\u51fa\u7684\u7ed3\u679c\uff0c\u8bf7\u5148\u6267\u884c\u626b\u63cf\uff01");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("\u5bfc\u51fa\u626b\u63cf\u7ed3\u679c");
        FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter("JSON \u683c\u5f0f (*.json)", "*.json");
        FileChooser.ExtensionFilter csvFilter  = new FileChooser.ExtensionFilter("CSV \u683c\u5f0f (*.csv)", "*.csv");
        FileChooser.ExtensionFilter txtFilter  = new FileChooser.ExtensionFilter("TXT \u683c\u5f0f (*.txt)", "*.txt");
        fc.getExtensionFilters().addAll(jsonFilter, csvFilter, txtFilter);
        fc.setSelectedExtensionFilter(jsonFilter);
        fc.setInitialFileName("scan-result.json");
        // \u5207\u6362\u8fc7\u6ee4\u5668\u65f6\u81ea\u52a8\u66f4\u65b0\u6587\u4ef6\u540d\u540e\u7f00
        fc.selectedExtensionFilterProperty().addListener((obs, oldFt, newFt) -> {
            String name = fc.getInitialFileName();
            if (name == null) name = "scan-result";
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            if (newFt == csvFilter)       fc.setInitialFileName(base + ".csv");
            else if (newFt == txtFilter)  fc.setInitialFileName(base + ".txt");
            else                          fc.setInitialFileName(base + ".json");
        });
        File file = fc.showSaveDialog(this.root.getScene().getWindow());
        if (file == null) return;
        String name = file.getName().toLowerCase();
        // \u5982\u679c\u7528\u6237\u6ca1\u6709\u624b\u52a8\u52a0\u4e0a\u6269\u5c55\u540d\uff0c\u6839\u636e\u6240\u9009\u8fc7\u6ee4\u5668\u8865\u5168
        FileChooser.ExtensionFilter selected = fc.getSelectedExtensionFilter();
        if (!name.contains(".")) {
            if (selected == csvFilter)       { file = new File(file.getParentFile(), file.getName() + ".csv");  name = file.getName().toLowerCase(); }
            else if (selected == txtFilter)  { file = new File(file.getParentFile(), file.getName() + ".txt");  name = file.getName().toLowerCase(); }
            else                             { file = new File(file.getParentFile(), file.getName() + ".json"); name = file.getName().toLowerCase(); }
        }
        try {
            if (name.endsWith(".csv")) {
                ResultExporter.exportCsv(entries, file);
            } else if (name.endsWith(".txt")) {
                ResultExporter.exportTxt(entries, file);
            } else {
                ResultExporter.exportJson(entries, file);
            }
            this.ctx.log.log("[+] \u7ed3\u679c\u5df2\u5bfc\u51fa: " + file.getAbsolutePath(), LogConsole.Severity.INFO);
        } catch (Exception ex) {
            this.showWarn("\u5bfc\u51fa\u5931\u8d25: " + ex.getMessage());
        }
    }

    private void onReloadPoc() {
        try {
            PocLoader.get().reload();
            SensitiveInfoCollector.reloadRules();
            this.ctx.log.log("[+] POC \u914d\u7f6e\u5df2\u91cd\u65b0\u52a0\u8f7d\uff08\u51715\u4e2a\u6587\u4ef6\uff09", LogConsole.Severity.INFO);
            this.ctx.log.log("[+] \u8bf7\u4fee\u6539 poc/*.json \u540e\u70b9\u6b64\u6309\u94ae\u5373\u53ef\u751f\u6548", LogConsole.Severity.INFO);
        } catch (Exception ex) {
            this.showWarn("POC \u91cd\u8f7d\u5931\u8d25: " + ex.getMessage());
        }
    }

    private void onOpenPocDir() {
        try {
            File dir = PocLoader.get().pocDirectory();
            if (!dir.isDirectory()) dir.mkdirs();
            java.awt.Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            this.showWarn("\u65e0\u6cd5\u6253\u5f00 POC \u76ee\u5f55: " + ex.getMessage());
        }
    }

    private String extractBasedir(String url) {
        try {
            String path = new URL(url.trim()).getPath();
            if (path != null) {
                for (String prefix : RUOYI_API_PREFIXES) {
                    if (!path.contains("/" + prefix)) continue;
                    return "/" + prefix;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "/prod-api";
    }

    private String extractCleanBaseUrl(String url) {
        try {
            URL u = new URL(url.trim());
            String path = u.getPath() == null ? "" : u.getPath();
            for (String prefix : RUOYI_API_PREFIXES) {
                int idx = path.indexOf("/" + prefix);
                if (idx < 0) continue;
                String cleanPath = path.substring(0, idx);
                String host = u.getProtocol() + "://" + u.getHost() + (String)(u.getPort() != -1 ? ":" + u.getPort() : "");
                return host + cleanPath;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return url.trim().replaceAll("/+$", "");
    }

    public Parent getRoot() {
        return this.root;
    }

    public void shutdown() {
        this.ctx.shutdown();
    }
}

