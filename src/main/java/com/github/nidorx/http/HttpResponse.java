package com.github.nidorx.http;


import com.google.gson.Gson;

import java.net.CookieManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstração de uma resposta para requisição
 */
public class HttpResponse {

    private static final Gson OBJECT_MAPPER = new Gson();

    public int statusCode;

    public Map<String, List<String>> headers;

    public String content;

    public byte[] data;

    public CookieManager cookieManager;

    public Map<String, Object> asJson() throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, HashMap.class);
    }

    public <T> T asJson(Class<T> type) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, type);
    }

    public boolean isSuccess() {
        return (statusCode >= 200 && statusCode < 300);
    }
}
