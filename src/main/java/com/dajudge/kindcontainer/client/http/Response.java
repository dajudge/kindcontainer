package com.dajudge.kindcontainer.client.http;

import org.apache.commons.io.input.ProxyInputStream;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

import static com.dajudge.kindcontainer.client.http.RequestBody.JSON_MAPPER;

public class Response implements AutoCloseable {
    private final ResponseWrapper wrapper;

    public Response(final ResponseWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public int code() {
        return wrapper.code;
    }

    public <T> T readJsonFromBody(final TypeReference<T> type) throws IOException {
        return JSON_MAPPER.readValue(dontClose(wrapper.stream), type);
    }

    @NotNull
    private ProxyInputStream dontClose(InputStream v) {
        return new ProxyInputStream(v) {
            @Override
            public void close() {
            }
        };
    }

    @Override
    public void close() {
        try {
            wrapper.close();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to close HTTP connection's input stream", e);
        }
    }

    public String bodyAsString(final Charset charset) throws IOException {
        return new String(bodyAsBytes(), charset);
    }

    public byte[] bodyAsBytes() throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = wrapper.stream.read(buffer)) > 0) {
            bos.write(buffer, 0, read);
        }
        return bos.toByteArray();
    }

    public String statusMessage() {
        return wrapper.statusMessage;
    }

    static class ResponseWrapper {
        private final int code;
        private final String statusMessage;
        private final InputStream stream;
        private final HttpURLConnection conn;

        ResponseWrapper(final int code, final String statusMessage, final InputStream stream, final HttpURLConnection conn) {
            this.code = code;
            this.statusMessage = statusMessage;
            this.stream = stream;
            this.conn = conn;
        }

        public void close() throws IOException {
            try {
                stream.close();
            } finally {
                conn.disconnect();
            }
        }

        public int getCode() {
            return code;
        }
    }
}
