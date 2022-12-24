package com.dajudge.kindcontainer.client.model.admission.v1;

public class WebhookClientConfig {
    private String caBundle;

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getCaBundle() {
        return caBundle;
    }

    public void setCaBundle(final String caBundle) {
        this.caBundle = caBundle;
    }
}
