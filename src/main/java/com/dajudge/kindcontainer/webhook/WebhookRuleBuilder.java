package com.dajudge.kindcontainer.webhook;

public interface WebhookRuleBuilder<R> {
    WebhookRuleBuilder<R> withApiGroups(String... apiGroups);

    WebhookRuleBuilder<R> withApiVersions(String... apiVersions);

    WebhookRuleBuilder<R> withOperations(String... operations);

    WebhookRuleBuilder<R> withResources(String... resources);

    WebhookRuleBuilder<R> withScope(String scope);

    R endRule();
}
