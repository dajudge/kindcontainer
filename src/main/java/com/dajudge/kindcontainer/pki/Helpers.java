package com.dajudge.kindcontainer.pki;

import com.dajudge.kindcontainer.Utils.ThrowingSupplier;

final class Helpers {
    private Helpers() {
        throw new IllegalStateException("Do not instantiate");
    }

    static <T> T call(final ThrowingSupplier<T, Exception> callable) {
        try {
            return callable.get();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
