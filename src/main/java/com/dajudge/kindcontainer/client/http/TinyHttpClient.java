package com.dajudge.kindcontainer.client.http;

public interface TinyHttpClient {
    RequestBuilder request();

    static TinyHttpClientBuilder newHttpClient() {
        return new TinyHttpClientBuilder();
    }
}
