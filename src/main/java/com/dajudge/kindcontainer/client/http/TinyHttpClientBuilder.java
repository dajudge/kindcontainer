package com.dajudge.kindcontainer.client.http;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class TinyHttpClientBuilder {
    private SSLSocketFactory socketFactory;

    TinyHttpClientBuilder() {
    }

    public TinyHttpClientBuilder withSslSocketFactory(final SSLSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
        return this;
    }

    public TinyHttpClient build() {
        return new TinyHttpClientImpl(socketFactory);
    }
}
