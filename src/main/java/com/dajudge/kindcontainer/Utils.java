/*
Copyright 2020-2021 Alex Stockinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.dajudge.kindcontainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;

final class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private Utils() {
    }

    static String readString(final InputStream is) throws IOException {
        return new String(readBytes(is), UTF_8);
    }

    private static byte[] readBytes(final InputStream is) throws IOException {
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        while ((read = is.read(buffer)) > 0) {
            bos.write(buffer, 0, read);
        }
        return bos.toByteArray();
    }

    static String loadResource(final String name) {
        final InputStream stream = Utils.class.getClassLoader().getResourceAsStream(name);
        try (final InputStream is = stream) {
            return readString(is);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to load resource: " + name, e);
        }
    }

    static <T, E extends Exception> T waitUntilNotNull(
            final Supplier<T> check,
            final int timeout,
            final String message,
            final Supplier<E> error
    ) throws E {
        boolean first = true;
        final long start = currentTimeMillis();
        while ((currentTimeMillis() - start) < timeout) {
            final T result = check.get();
            if (result != null) {
                return result;
            }
            if (first) {
                LOG.info("{}", message);
            }
            first = false;
            try {
                sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw error.get();
    }

    public static String indent(final String prefix, final String string) {
        return string.replaceAll("(?m)^", prefix);
    }
}
