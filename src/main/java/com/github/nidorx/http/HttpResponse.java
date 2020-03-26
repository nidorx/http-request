package com.github.nidorx.http;


import com.github.nidorx.http.util.ParameterizedTypeReference;
import com.github.nidorx.http.gson.Gson;

import java.lang.reflect.Type;
import java.net.CookieManager;
import java.util.ArrayList;
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


    public Map<String, Object> fromJson() throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, HashMap.class);
    }

    public <T> T fromJson(Class<T> classOfT) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, classOfT);
    }

    public <T> T fromJson(Type typeOfT) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, typeOfT);
    }

    public <T> T fromJson(ParameterizedTypeReference<T> parameterizedTypeReferenceOfT) throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, parameterizedTypeReferenceOfT.getType());
    }

    public List<Map<String, Object>> fromJsonToList() throws Exception {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.fromJson(content, ArrayList.class);
    }

    public boolean isSuccess() {
        return (statusCode >= 200 && statusCode < 300);
    }
}
