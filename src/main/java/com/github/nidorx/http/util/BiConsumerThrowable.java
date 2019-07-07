package com.github.nidorx.http.util;

import java.util.Objects;

@FunctionalInterface
public interface BiConsumerThrowable<T, U> {

    void accept(T t, U u) throws Exception;

    default BiConsumerThrowable<T, U> andThen(BiConsumerThrowable<? super T, ? super U> after) throws Exception {
        Objects.requireNonNull(after);

        return (l, r) -> {
            accept(l, r);
            after.accept(l, r);
        };
    }
}
