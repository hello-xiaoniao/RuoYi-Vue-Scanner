package com.ruoyi.scanner.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * RuoYi 响应统一判定工具。
 *
 * 核心问题：RuoYi 框架把所有业务响应（包括 401/403/404/500）都包装在 HTTP 200 里，
 * 形如 {"code":401,"msg":"认证失败","data":null}。
 * 仅判断 HTTP 200 会产生大量误报。本工具解析 JSON 的 code 字段做三态判定。
 */
public final class RuoYiResponse {

    public enum Status {
        /** code==200 且业务数据非空（真实成功，有业务结果） */
        SUCCESS,
        /** code==200 但 data/rows 为空（接口可达，但无业务数据，仅作线索） */
        SUCCESS_EMPTY,
        /** code in {401,403,404} 或 msg 含认证/未找到特征（接口需认证或不存在） */
        DENIED,
        /** code==500 或 msg 含服务异常（服务端报错，非业务成功） */
        ERROR,
        /** 响应是 HTML/SPA 前端页面，不是接口 JSON */
        SPA,
        /** 非 JSON、无法判定 */
        UNKNOWN
    }

    public static class Result {
        public final Status status;
        public final int code;          // JSON 里的 code，无则 -1
        public final String msg;        // JSON 里的 msg，无则 ""
        public final boolean hasData;   // data/rows 是否非空

        public Result(Status status, int code, String msg, boolean hasData) {
            this.status = status;
            this.code = code;
            this.msg = msg == null ? "" : msg;
            this.hasData = hasData;
        }
    }

    private static final Gson GSON = new Gson();

    private RuoYiResponse() {}

    /**
     * 解析响应体，返回判定结果。body 为 null/空时返回 UNKNOWN。
     */
    public static Result parse(String body) {
        if (body == null || body.isEmpty()) {
            return new Result(Status.UNKNOWN, -1, "", false);
        }
        String trimmed = body.trim();

        // HTML / SPA 前端页面特征（<!doctype html、<html、<div id="app"）
        String low = trimmed.toLowerCase();
        if (low.startsWith("<!doctype html") || low.startsWith("<html")
                || low.contains("<div id=\"app\"") || low.contains("<div id=app")) {
            return new Result(Status.SPA, -1, "", false);
        }

        // 尝试解析 JSON
        JsonElement el;
        try {
            el = GSON.fromJson(trimmed, JsonElement.class);
        } catch (Exception e) {
            return new Result(Status.UNKNOWN, -1, "", false);
        }
        if (el == null || !el.isJsonObject()) {
            return new Result(Status.UNKNOWN, -1, "", false);
        }
        JsonObject obj = el.getAsJsonObject();

        int code = -1;
        if (obj.has("code") && obj.get("code").isJsonPrimitive()
                && obj.get("code").getAsJsonPrimitive().isNumber()) {
            code = obj.get("code").getAsInt();
        }
        String msg = "";
        if (obj.has("msg") && obj.get("msg").isJsonPrimitive()) {
            msg = obj.get("msg").getAsString();
        }
        boolean hasData = hasBusinessData(obj);

        // 无 code 字段：可能是非标准 JSON（如纯 Swagger 文档 {"swagger":"2.0",...}）
        if (code == -1) {
            // Swagger/OpenAPI 文档特征
            if (obj.has("swagger") || obj.has("openapi") || obj.has("paths") || obj.has("swaggerVersion")) {
                return new Result(Status.SUCCESS, -1, msg, true);
            }
            return new Result(Status.UNKNOWN, -1, msg, hasData);
        }

        if (code == 200) {
            return new Result(hasData ? Status.SUCCESS : Status.SUCCESS_EMPTY, code, msg, hasData);
        }
        if (code == 401 || code == 403 || code == 404) {
            return new Result(Status.DENIED, code, msg, hasData);
        }
        if (code == 500) {
            return new Result(Status.ERROR, code, msg, hasData);
        }
        // 其它非 200 code：含认证/未找到特征按 DENIED，否则 ERROR
        String msgLow = msg.toLowerCase();
        if (msgLow.contains("认证失败") || msgLow.contains("未登录") || msgLow.contains("no endpoint")
                || msgLow.contains("not found") || msgLow.contains("无法访问系统资源")
                || msgLow.contains("access denied") || msgLow.contains("unauthorized")
                || msgLow.contains("没有权限")) {
            return new Result(Status.DENIED, code, msg, hasData);
        }
        return new Result(Status.ERROR, code, msg, hasData);
    }

    /**
     * 判断 data/rows 是否包含真实业务数据（非 null、非空数组、非空对象）。
     */
    private static boolean hasBusinessData(JsonObject obj) {
        for (String key : new String[]{"data", "rows", "list", "records"}) {
            if (obj.has(key)) {
                JsonElement v = obj.get(key);
                if (v.isJsonNull()) continue;
                if (v.isJsonArray() && v.getAsJsonArray().size() > 0) return true;
                if (v.isJsonObject() && v.getAsJsonObject().size() > 0) return true;
                if (v.isJsonPrimitive() && !v.getAsString().isEmpty()) return true;
            }
        }
        return false;
    }

    /**
     * 是否为 RuoYi 错误包装响应（DENIED 或 ERROR）。用于在命中特征前先排雷。
     */
    public static boolean isRuoYiError(String body) {
        Result r = parse(body);
        return r.status == Status.DENIED || r.status == Status.ERROR;
    }
}
