package com.dajudge.kindcontainer.client;

public class ConflictException extends TinyHttpClientRuntimeException {
    public ConflictException(final String method, final String path, final String statusMessage) {
        super(method, path, statusMessage);
    }
}
