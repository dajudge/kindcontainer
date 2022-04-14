package com.dajudge.kindcontainer.client.http;

import javax.net.ssl.SSLSocketFactory;

public class TinyHttpClientImpl implements TinyHttpClient {
    private final SSLSocketFactory sslSocketFactory;

    public TinyHttpClientImpl(final SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    @Override
    public RequestBuilder request() {
        return new RequestBuilder(sslSocketFactory);
    }
}
