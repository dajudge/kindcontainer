package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.http.Response;

import java.io.IOException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TinyHttpClientRuntimeException extends RuntimeException {
    public TinyHttpClientRuntimeException(final String method, final String path, final Response result) throws IOException {
        super(format("HTTP %s %s -> %s -> %s", method, path, result.statusMessage(), result.bodyAsString(UTF_8)));
    }
}