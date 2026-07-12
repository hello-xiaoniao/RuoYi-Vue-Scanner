/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.core;

import com.ruoyi.scanner.core.ScanConfig;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpService {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private final OkHttpClient client;

    public HttpService(ScanConfig config, int timeoutSec) {
        OkHttpClient.Builder b = new OkHttpClient.Builder().connectTimeout(timeoutSec, TimeUnit.SECONDS).readTimeout(timeoutSec, TimeUnit.SECONDS).writeTimeout(timeoutSec, TimeUnit.SECONDS).retryOnConnectionFailure(false);
        try {
            TrustManager[] tm = new TrustManager[]{new X509TrustManager(){

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tm, new SecureRandom());
            b.sslSocketFactory(ctx.getSocketFactory(), (X509TrustManager)tm[0]);
            b.hostnameVerifier((h, s) -> true);
        }
        catch (Exception tm) {
            // empty catch block
        }
        if (config != null && config.proxy != null && !config.proxy.isEmpty()) {
            try {
                String p = config.proxy.replaceAll("^https?://", "");
                String[] parts = p.split(":");
                if (parts.length == 2) {
                    b.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(parts[0], Integer.parseInt(parts[1]))));
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.client = b.build();
    }

    public Headers buildHeaders(ScanConfig config, Map<String, String> extra) {
        Headers.Builder hb = new Headers.Builder();
        hb.add("User-Agent", UA);
        if (config != null) {
            if (config.cookie != null && !config.cookie.isEmpty()) {
                hb.add("Cookie", config.cookie);
            }
            if (config.authorization != null && !config.authorization.isEmpty()) {
                hb.add("Authorization", config.authorization);
            }
        }
        if (extra != null) {
            extra.forEach(hb::set);
        }
        return hb.build();
    }

    public Response get(String url, ScanConfig config) throws Exception {
        return this.get(url, config, null);
    }

    public Response get(String url, ScanConfig config, Map<String, String> extraHeaders) throws Exception {
        Request req = new Request.Builder().url(url).headers(this.buildHeaders(config, extraHeaders)).get().build();
        return this.client.newCall(req).execute();
    }

    public Response post(String url, ScanConfig config, RequestBody body, Map<String, String> extraHeaders) throws Exception {
        Request req = new Request.Builder().url(url).headers(this.buildHeaders(config, extraHeaders)).post(body).build();
        return this.client.newCall(req).execute();
    }

    public Response postForm(String url, ScanConfig config, Map<String, String> form) throws Exception {
        FormBody.Builder fb = new FormBody.Builder();
        if (form != null) {
            form.forEach(fb::add);
        }
        return this.post(url, config, fb.build(), null);
    }

    public Response postJson(String url, ScanConfig config, String json) throws Exception {
        HashMap<String, String> h = new HashMap<String, String>();
        h.put("Content-Type", "application/json");
        return this.post(url, config, RequestBody.create(json == null ? "" : json, JSON), h);
    }

    public Response put(String url, ScanConfig config, String json) throws Exception {
        HashMap<String, String> h = new HashMap<String, String>();
        h.put("Content-Type", "application/json");
        Request req = new Request.Builder().url(url).headers(this.buildHeaders(config, h)).put(RequestBody.create(json == null ? "" : json, JSON)).build();
        return this.client.newCall(req).execute();
    }
}

