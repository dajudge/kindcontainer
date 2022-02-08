package com.dajudge.kindcontainer.client.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterSpec {
    private String certificateAuthorityData;
    private String server;

    @JsonProperty("certificate-authority-data")
    public String getCertificateAuthorityData() {
        return certificateAuthorityData;
    }

    public void setCertificateAuthorityData(String certificateAuthorityData) {
        this.certificateAuthorityData = certificateAuthorityData;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
