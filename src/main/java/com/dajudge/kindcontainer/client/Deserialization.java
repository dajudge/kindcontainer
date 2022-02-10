package com.dajudge.kindcontainer.client;

import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

final class Deserialization {
    static final ObjectMapper JSON_MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Deserialization() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }
}
