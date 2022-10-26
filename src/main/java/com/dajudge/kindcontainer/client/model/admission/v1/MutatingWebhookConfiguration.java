package com.dajudge.kindcontainer.client.model.admission.v1;

import com.dajudge.kindcontainer.client.model.base.ResourceList;
import com.dajudge.kindcontainer.client.model.base.WatchStreamItem;
import com.dajudge.kindcontainer.client.model.base.WithMetadata;

import java.util.List;

public class MutatingWebhookConfiguration extends WithMetadata {
    private List<Webhook> webhooks;

    public List<Webhook> getWebhooks() {
        return webhooks;
    }

    public void setWebhooks(final List<Webhook> webhooks) {
        this.webhooks = webhooks;
    }

    public static class ItemList extends ResourceList<MutatingWebhookConfiguration> {
    }

    public static class StreamItem extends WatchStreamItem<MutatingWebhookConfiguration> {
    }
}
