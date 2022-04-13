package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.http.*;
import com.dajudge.kindcontainer.client.model.base.ResourceAction;
import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dajudge.kindcontainer.client.http.RequestBody.createFromJson;
import static com.dajudge.kindcontainer.client.model.base.ResourceAction.ADDED;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class HttpSupport {
    private static final Logger LOG = LoggerFactory.getLogger(HttpSupport.class);
    private final TinyHttpClient client;
    private final String masterUrl;

    public HttpSupport(final TinyHttpClient client, final String masterUrl) {
        this.client = client;
        this.masterUrl = masterUrl;
    }

    public <T extends WithMetadata, L extends ResourceList<T>, I extends WatchStreamItem<T>> Watch watch(
            final String path,
            final TypeReference<? extends ResourceList<T>> listClazz,
            final TypeReference<? extends WatchStreamItem<T>> itemClazz,
            final WatchCallback<T> callback
    ) {
        final WatchCallback<T> safeCallback = safe(callback);
        final ResourceList<? extends T> list = syncGet(path, listClazz);
        list.getItems().forEach(item -> safeCallback.onAction(ADDED, item));
        final String watchUrl = format("%s?watch=1&resourceVersion=%s", path, list.getMetadata().getResourceVersion());
        return asyncReq("GET", watchUrl, null, itemClazz, item -> {
            safeCallback.onAction(item.getType(), item.getObject());
            return true;
        }, safeCallback::onClose, ex -> {
            throw ex;
        });
    }

    public <T> T syncGet(final String path, final TypeReference<T> type) {
        return syncReq("GET", null, path, type);
    }

    public <T> T syncDelete(final String path, final TypeReference<T> type) {
        return syncReq("DELETE", null, path, type);
    }

    public <I, O> O syncPut(final String path, final I requestBody, final TypeReference<O> type) {
        return syncReq("PUT", requestBody, path, type);
    }

    public <I, O> O syncPost(final String path, final I requestBody, final TypeReference<O> type) {
        return syncReq("POST", requestBody, path, type);
    }

    private <T> T syncReq(
            final String method,
            final Object requestBody,
            final String path,
            final TypeReference<T> type
    ) {
        final AtomicReference<T> ref = new AtomicReference<>();
        final Watch watch = asyncReq(method, path, requestBody, type, result -> {
            ref.set(result);
            return false;
        }, ex -> {
            throw ex;
        });
        try {
            watch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted during HTTP request", e);
        }
        return ref.get();
    }

    private <I, T> Watch asyncReq(
            final String method, final String path, final I requestBody,
            final TypeReference<T> type,
            final Function<T, Boolean> sink,
            final Consumer<RuntimeException> errorSink
    ) {
        return asyncReq(method, path, requestBody, type, sink, () -> {
        }, errorSink);
    }

    private <I, T> Watch asyncReq(
            final String method,
            final String path, final I requestBody,
            final TypeReference<T> type,
            final Function<T, Boolean> sink,
            final Runnable closeCallback,
            final Consumer<RuntimeException> errorSink
    ) {
        try {
            final RequestBody wrappedRequestBody = createFromJson(requestBody);
            final Response result = client.request()
                    .url(format("%s%s", masterUrl, path))
                    .method(method, wrappedRequestBody)
                    .execute();
            if (result.code() > 400) {
                try {
                    if (result.code() == 409) {
                        errorSink.accept(new ConflictException());
                        return Watch.CLOSED;
                    }
                    if (result.code() == 404) {
                        return Watch.CLOSED;
                    }
                    final String bodyString = result.bodyAsString(US_ASCII);
                    throw new RuntimeException("HTTP request failed with status " + result.code() + ": " + bodyString);
                } finally {
                    result.close();
                }
            }

            final AtomicBoolean closing = new AtomicBoolean();
            final CountDownLatch requestCompleted = new CountDownLatch(1);
            new Thread(() -> {
                try {
                    boolean readNext = true;
                    while (readNext) {
                        final T item = result.readJsonFromBody(type);
                        readNext = sink.apply(item);
                    }
                } catch (final IOException e) {
                    if (!(e instanceof StreamResetException) || !closing.get()) {
                        LOG.error("Unexpected exception handling HTTP request", e);
                    }
                } finally {
                    if (!closing.get()) {
                        result.close();
                    }
                    closeCallback.run();
                    requestCompleted.countDown();
                }
            }).start();
            return new Watch() {
                @Override
                public void close() {
                    closing.set(true);
                }

                @Override
                public void await() throws InterruptedException {
                    requestCompleted.await();
                }
            };
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T> WatchCallback<T> safe(final WatchCallback<T> callback) {
        return new WatchCallback<T>() {
            @Override
            public void onAction(final ResourceAction action, final T object) {
                try {
                    callback.onAction(action, object);
                } catch (final Exception e) {
                    LOG.error("Unhandled exception in watch callback", e);
                }
            }

            @Override
            public void onClose() {
                try {
                    callback.onClose();
                } catch (final Exception e) {
                    LOG.error("Unhandled exception in watch callback", e);
                }
            }
        };
    }
}
