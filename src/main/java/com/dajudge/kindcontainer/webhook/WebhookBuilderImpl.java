package com.dajudge.kindcontainer.webhook;

import com.dajudge.kindcontainer.client.model.admission.v1.Webhook;
import com.dajudge.kindcontainer.client.model.admission.v1.WebhookClientConfig;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

class WebhookBuilderImpl<R> implements WebhookBuilder<R> {
    private final List<WebhookRuleBuilderImpl<WebhookBuilderImpl<R>>> rules = new ArrayList<>();
    private String name;
    private R parent;
    private int port;
    private LabelSelectorBuilderImpl<? extends WebhookBuilder<R>> namespaceSelector;
    private LabelSelectorBuilderImpl<? extends WebhookBuilder<R>> objectSelector;
    private Integer timeoutSeconds;
    private String failurePolicy;
    private String matchPolicy;

    public WebhookBuilderImpl(final String name, final R parent) {
        this.name = name;
        this.parent = parent;
    }

    @Override
    public WebhookBuilderImpl<R> atPort(final int port) {
        this.port = port;
        return this;
    }

    @Override
    public WebhookRuleBuilder<? extends WebhookBuilder<R>> withNewRule() {
        final WebhookRuleBuilderImpl<WebhookBuilderImpl<R>> rule = new WebhookRuleBuilderImpl<>(this);
        rules.add(rule);
        return rule;
    }

    @Override
    public LabelSelectorBuilder<? extends WebhookBuilder<R>> withNewNamespaceSelector() {
        return namespaceSelector = new LabelSelectorBuilderImpl<>(this);
    }

    @Override
    public LabelSelectorBuilder<? extends WebhookBuilder<R>> withNewObjectSelector() {
        return objectSelector = new LabelSelectorBuilderImpl<>(this);
    }

    @Override
    public R endWebhook() {
        return parent;
    }

    @Override
    public Webhook toWebhook(final AdmissionControllerManager manager, final String configName) {
        final Webhook webhook = new Webhook();
        webhook.setName(name);
        webhook.setFailurePolicy(null);
        webhook.setMatchPolicy(null);
        webhook.setSideEffects("None");
        webhook.setClientConfig(toClientConfig(manager, configName));
        webhook.setAdmissionReviewVersions(singletonList("v1"));
        webhook.setNamespaceSelector(namespaceSelector == null ? null : namespaceSelector.toLabelSelector());
        webhook.setObjectSelector(objectSelector == null ? null : objectSelector.toLabelSelector());
        webhook.setTimeoutSeconds(timeoutSeconds);
        webhook.setFailurePolicy(failurePolicy);
        webhook.setMatchPolicy(matchPolicy);
        webhook.setRules(this.rules.stream()
                .map(WebhookRuleBuilderImpl::toRuleWithOperations)
                .collect(toList()));
        return webhook;
    }

    private WebhookClientConfig toClientConfig(final AdmissionControllerManager manager, final String configName) {
        final String webhookUrl = manager.mapWebhook(configName, name, port);
        final WebhookClientConfig clientConfig = new WebhookClientConfig();
        clientConfig.setCaBundle(Base64.getEncoder().encodeToString(manager.getCaCertPem().getBytes(UTF_8)));
        clientConfig.setUrl(webhookUrl);
        return clientConfig;
    }
}
