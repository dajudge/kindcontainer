package com.dajudge.kindcontainer.client;

public class NotFoundException extends TinyHttpClientRuntimeException {
    public NotFoundException(final String method, final String path, final String statusMessage) {
        super(method, path, statusMessage);
    }
}
