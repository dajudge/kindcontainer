package com.dajudge.kindcontainer.client.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;

public class RequestBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(RequestBuilder.class);
    private final SSLSocketFactory sslSocketFactory;
    private URL url;
    private String method = "GET";
    private RequestBody body;
    private int connectTimeoutMillis;
    private int readTimeoutMillis;

    public RequestBuilder(final SSLSocketFactory sslSocketFactory) {

        this.sslSocketFactory = sslSocketFactory;
    }

    public RequestBuilder url(final String url) {
        try {
            this.url = new URL(url);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }
        return this;
    }

    public RequestBuilder method(final String method, final RequestBody body) {
        this.method = method;
        this.body = body;
        return this;
    }

    /**
     * Limits how long establishing the connection may block. A zero duration keeps the JDK default of no timeout.
     */
    public RequestBuilder connectTimeout(final Duration timeout) {
        this.connectTimeoutMillis = timeoutMillis(timeout, "connect timeout");
        return this;
    }

    /**
     * Limits how long reading the response may block. A zero duration keeps the JDK default of no timeout.
     */
    public RequestBuilder readTimeout(final Duration timeout) {
        this.readTimeoutMillis = timeoutMillis(timeout, "read timeout");
        return this;
    }

    public Response execute() throws IOException {
        if (url == null) {
            throw new IllegalStateException("No URL specified");
        }
        if (method == null) {
            throw new IllegalStateException("No METHOD specified");
        }
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        if (conn instanceof HttpsURLConnection) {
            if (sslSocketFactory != null) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
            }
        }
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        if (body != null) {
            conn.setRequestProperty("Content-Type", body.getContentType());
            conn.setDoOutput(true);
            try (final OutputStream out = conn.getOutputStream()) {
                out.write(body.getBytes());
            }
        }
        final Response.ResponseWrapper wrapper = executeRequest(conn);
        LOG.debug("HTTP {} {} -> {}", method, url, wrapper.getCode());
        return new Response(wrapper);
    }

    private Response.ResponseWrapper executeRequest(final HttpURLConnection conn) throws IOException {
        final int responseCode = conn.getResponseCode();
        return new Response.ResponseWrapper(
                responseCode,
                conn.getResponseMessage(),
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                conn
        );
    }

    private static int timeoutMillis(final Duration timeout, final String description) {
        Objects.requireNonNull(timeout, description);
        if (timeout.isNegative()) {
            throw new IllegalArgumentException(description + " must not be negative");
        }

        final long millis = timeout.toMillis();
        if (millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(description + " must not exceed " + Integer.MAX_VALUE + "ms");
        }

        // HttpURLConnection treats zero as infinite, so round positive sub-millisecond durations up.
        return millis == 0 && !timeout.isZero() ? 1 : (int) millis;
    }

}
