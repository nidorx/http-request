package com.github.nidorx.http.util;

import java.lang.reflect.Method;

/**
 * Utilitario para usar qualquer versão do Jackson ObjectMapper
 */
public class ObjectMapper {

    private static Object MAPPER_INSTANCE;

    private static Method MAPPER_READ_VALUE_METHOD;

    {
        Class<?> aClass;
        try {
            aClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");

        } catch (ClassNotFoundException e) {
            System.out.println("Class \"org.codehaus.jackson.map.ObjectMapper\" Not Found. Trying old version.");

            try {
                aClass = Class.forName("org.codehaus.jackson.map.ObjectMapper");
            } catch (ClassNotFoundException t) {
                throw new RuntimeException(t);
            }
        }

        try {
            MAPPER_INSTANCE = aClass.newInstance();
            Method readValue = aClass.getMethod("readValue", String.class, Class.class);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T readValue(String content, Class<T> valueType) throws Exception {
        return (T) MAPPER_READ_VALUE_METHOD.invoke(MAPPER_INSTANCE, content, valueType);
    }

}
