/*
 * Decompiled with CFR 0.152.
 */
package com.ruoyi.scanner.core;

public class ScanConfig {
    public final String url;
    public final String cookie;
    public final String authorization;
    public final String proxy;
    public final String baseApi;

    public ScanConfig(String url, String cookie, String authorization, String proxy, String baseApi) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.cookie = cookie == null ? "" : cookie.replaceAll("[\\r\\n]", "");
        this.authorization = authorization == null ? "" : authorization.replaceAll("[\\r\\n]", "");
        this.proxy = proxy == null ? "" : proxy.trim();
        String b = (baseApi == null || baseApi.trim().isEmpty()) ? "/prod-api" : baseApi.trim();
        if (!b.startsWith("/")) {
            b = "/" + b;
        }
        if (b.length() > 1 && b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        this.baseApi = b;
    }

    public String fullUrl(String path) {
        String p = path == null ? "" : path;
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return this.url + p;
    }

    public String fullApiUrl(String path) {
        String p = path == null ? "" : path;
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return this.url + this.baseApi + p;
    }
}

