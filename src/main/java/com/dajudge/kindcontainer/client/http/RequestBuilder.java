package com.dajudge.kindcontainer.client.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RequestBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(RequestBuilder.class);
    private final SSLSocketFactory sslSocketFactory;
    private URL url;
    private String method = "GET";
    private RequestBody body;

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

    public Response execute() throws IOException {
        if (url == null) {
            throw new IllegalStateException("No URL specified");
        }
        if (method == null) {
            throw new IllegalStateException("No METHOD specified");
        }
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
        try {
            return new Response.ResponseWrapper(conn.getResponseCode(), conn.getInputStream(), conn);
        } catch(final IOException e) {
            return new Response.ResponseWrapper(conn.getResponseCode(), conn.getErrorStream(), conn);
        }
    }

}
