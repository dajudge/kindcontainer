package com.dajudge.kindcontainer.client.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSpec {
    private String clientCertificateData;
    private String clientKeyData;
    private String token;

    @JsonProperty("client-certificate-data")
    public String getClientCertificateData() {
        return clientCertificateData;
    }

    public void setClientCertificateData(String clientCertificateData) {
        this.clientCertificateData = clientCertificateData;
    }

    @JsonProperty("client-key-data")
    public String getClientKeyData() {
        return clientKeyData;
    }

    public void setClientKeyData(String clientKeyData) {
        this.clientKeyData = clientKeyData;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
