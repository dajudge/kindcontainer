package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.model.admission.v1.Webhook;

public interface WebhookBuilder<R> {
    WebhookBuilder<R> atPort(int port);

    WebhookBuilder<R> withPath(String path);

    WebhookRuleBuilder<? extends WebhookBuilder<R>> withNewRule();

    LabelSelectorBuilder<? extends WebhookBuilder<R>> withNewNamespaceSelector();

    LabelSelectorBuilder<? extends WebhookBuilder<R>> withNewObjectSelector();

    R endWebhook();

    Webhook toWebhook(AdmissionControllerManager manager, String configName);
}
