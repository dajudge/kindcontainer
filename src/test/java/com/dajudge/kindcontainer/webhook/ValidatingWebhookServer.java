package com.dajudge.kindcontainer.webhook;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;

public class ValidatingWebhookServer extends AbstractWebhookServer {

    @Override
    protected AdmissionReview review(final AdmissionReview review) {
        final ConfigMap cm = (ConfigMap) review.getRequest().getObject();

        if ("true".equals(cm.getData().get("allowed"))) {
            return new AdmissionReviewBuilder()
                    .withNewResponse()
                    .withUid(review.getRequest().getUid())
                    .withAllowed(true)
                    .endResponse()
                    .build();
        } else {
            return new AdmissionReviewBuilder()
                    .withNewResponse()
                    .withUid(review.getRequest().getUid())
                    .withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(400)
                            .withMessage("Not allowed")
                            .build())
                    .endResponse()
                    .build();
        }
    }

    @Override
    public void register(final AdmissionControllerBuilder admission) {
        admission.validating()
                .withNewWebhook("validating.kindcontainer.dajudge.com")
                .atPort(getPort())
                .withNewRule()
                .withApiGroups("")
                .withApiVersions("v1")
                .withOperations("CREATE", "UPDATE")
                .withResources("configmaps")
                .withScope("Namespaced")
                .endRule()
                .endWebhook()
                .build();
    }
}
