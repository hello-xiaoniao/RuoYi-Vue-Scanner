/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner;

import com.ruoyi.scanner.poc.PocLoader;
import com.ruoyi.scanner.ui.MainView;
import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class App
extends Application {
    private static void installTrustAllSSL() {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager(){

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] c, String a) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] c, String a) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
            SSLContext.setDefault(sc);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Override
    public void start(Stage stage) {
        App.installTrustAllSSL();
        // init POC loader: parent dir of the jar, or current dir if running from IDE
        try {
            File jarFile = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            PocLoader.init(jarFile.isFile() ? jarFile.getParentFile() : new File("."));
        } catch (Exception e) {
            System.err.println("[PocLoader] Init failed: " + e.getMessage() + " — using user.dir");
            PocLoader.init(new File("."));
        }
        MainView view = new MainView();
        Scene scene = new Scene(view.getRoot(), 1300.0, 800.0);
        try {
            scene.getStylesheets().add(this.getClass().getResource("/css/style.css").toExternalForm());
        }
        catch (Exception exception) {
            // empty catch block
        }
        stage.setTitle("\u82e5\u4f9dVue\u6f0f\u6d1e\u68c0\u6d4b\u5de5\u5177");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            view.shutdown();
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args2) {
        App.launch(args2);
    }
}

