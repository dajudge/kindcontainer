package com.dajudge.kindcontainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DeploymentWaitStrategy extends AbstractWaitStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentWaitStrategy.class);
    private final String namespace;
    private final String name;

    public DeploymentWaitStrategy(final String namespace, final String name) {
        this.namespace = namespace;
        this.name = name;
    }

    @Override
    protected void waitUntilReady() {
        if (!(waitStrategyTarget instanceof KubernetesContainer)) {
            throw new IllegalArgumentException(format(
                    "%s only works with a %s",
                    DeploymentWaitStrategy.class.getSimpleName(),
                    KubernetesContainer.class.getSimpleName()
            ));
        }
        LOG.info("Waiting for Deployment to be ready: {}/{}", namespace, name);
        final KubernetesContainer<?> k8s = (KubernetesContainer<?>) waitStrategyTarget;
        k8s.runWithClient(client -> {
            client.apps().deployments().inNamespace(namespace).withName(name).waitUntilReady(120, SECONDS);
        });
    }

    public static DeploymentWaitStrategy deploymentIsReady(final String namespace, final String name) {
        return new DeploymentWaitStrategy(namespace, name);
    }
}
