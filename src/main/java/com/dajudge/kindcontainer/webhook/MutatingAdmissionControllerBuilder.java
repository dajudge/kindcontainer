package com.dajudge.kindcontainer.webhook;

public interface MutatingAdmissionControllerBuilder {

    WebhookBuilder<? extends MutatingAdmissionControllerBuilder> withNewWebhook(String name);

    void build();
}
