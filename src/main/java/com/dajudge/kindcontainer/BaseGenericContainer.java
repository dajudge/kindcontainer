package com.dajudge.kindcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import static java.nio.charset.StandardCharsets.US_ASCII;

class BaseGenericContainer<T extends BaseGenericContainer<T>> extends GenericContainer<T> {
    public BaseGenericContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    T withCopyAsciiToContainer(final String text, final String path) {
        return withCopyToContainer(Transferable.of(text.getBytes(US_ASCII)), path);
    }
}
