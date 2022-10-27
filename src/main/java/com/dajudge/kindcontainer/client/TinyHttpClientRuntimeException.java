package com.dajudge.kindcontainer.client;

import static java.lang.String.format;

public class TinyHttpClientRuntimeException extends RuntimeException {
    public TinyHttpClientRuntimeException(final String method, final String path, final String statusMessage) {
        super(format("HTTP %s %s -> %s", method, path, statusMessage));
    }
}
