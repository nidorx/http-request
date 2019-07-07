package com.github.nidorx.http.util;

/**
 * Representa um callback para execução da requisição
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface Callback<T, U, R> {

    R apply(T t, U u) throws Exception;
}
