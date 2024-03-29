package com.dajudge.kindcontainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class Utils {
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
            final long timeout,
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

    static String resolve(final String hostname) {
        try {
            return InetAddress.getByName(hostname).getHostAddress();
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    static String prefixLines(final String strings, final String prefix) {
        return strings.replaceAll("(?m)^", prefix);
    }

    public interface ThrowingRunnable<E extends Exception> {
        void run() throws E;
    }

    public interface ThrowingFunction<I, O, E extends Exception> {
        O apply(I i) throws E;
    }

    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    public interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    public interface LazyContainer<T> {
        T get();

        boolean isCreated();

        void guardedClose();

        static <T extends GenericContainer<?>> LazyContainer<T> from(Supplier<T> supplier) {
            final AtomicReference<T> containerRef = new AtomicReference<>();
            return new LazyContainer<T>() {
                @Override
                public T get() {
                    if (!isCreated()) {
                        containerRef.set(supplier.get());
                        containerRef.get().start();
                    }
                    return containerRef.get();
                }

                @Override
                public boolean isCreated() {
                    return containerRef.get() != null;
                }

                @Override
                public void guardedClose() {
                    if (isCreated()) {
                        try {
                            get().stop();
                        } catch (final Exception e) {
                            LOG.error("Failed to stop container", e);
                        }
                    }
                }
            };
        }
    }
}
