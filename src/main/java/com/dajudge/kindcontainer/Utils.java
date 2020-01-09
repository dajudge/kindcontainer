package com.dajudge.kindcontainer;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Utils {
    private Utils() {
    }

    @NotNull
    static String readString(final InputStream is) throws IOException {
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        while ((read = is.read(buffer)) > 0) {
            bos.write(buffer, 0, read);
        }
        return new String(bos.toByteArray(), UTF_8);
    }

    static String loadResource(final String name) {
        final InputStream stream = Utils.class.getClassLoader().getResourceAsStream(name);
        try (final InputStream is = stream) {
            return readString(is);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load resource: " + name, e);
        }
    }
}
