package com.dajudge.kindcontainer.webhook;

public interface ValidatingAdmissionControllerBuilder {

    WebhookBuilder<? extends ValidatingAdmissionControllerBuilder> withNewWebhook(String name);

    void build();
}
