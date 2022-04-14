package com.dajudge.kindcontainer.client.http;

import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class RequestBody {
    static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final byte[] bytes;
    private final String contentType;

    private RequestBody(final byte[] bytes, final String contentType) {
        this.bytes = bytes;
        this.contentType = contentType;
    }

    public static <T> RequestBody createFromJson(final T data) {
        if (data == null) {
            return null;
        }
        try {
            return new RequestBody(JSON_MAPPER.writeValueAsBytes(data), "application/json; charset=utf-8");
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Unserializable JSON payload", e);
        }
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
