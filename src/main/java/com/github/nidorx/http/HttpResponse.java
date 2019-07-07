package com.github.nidorx.http;

import com.github.nidorx.http.util.ObjectMapper;

import java.net.CookieManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstração de uma resposta para requisição
 */
public class HttpResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public int statusCode;

    public Map<String, List<String>> headers;

    public String content;

    public CookieManager cookieManager;

    public Map<String, Object> asJson() throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(content, HashMap.class);
    }

    public <T> T asJson(Class<T> type) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.readValue(content, type);
    }

    public boolean isSuccess() {
        return (statusCode >= 200 && statusCode < 300);
    }
}
