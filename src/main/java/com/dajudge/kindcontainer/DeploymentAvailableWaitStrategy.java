package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.model.apps.v1.DeploymentCondition;
import com.dajudge.kindcontainer.client.model.base.BaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DeploymentAvailableWaitStrategy extends AbstractWaitStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentAvailableWaitStrategy.class);
    private final String namespace;
    private final String name;

    public DeploymentAvailableWaitStrategy(final String namespace, final String name) {
        this.namespace = namespace;
        this.name = name;
    }

    @Override
    protected void waitUntilReady() {
        if (!(waitStrategyTarget instanceof KubernetesContainer)) {
            throw new IllegalArgumentException(format(
                    "%s only works with a %s",
                    DeploymentAvailableWaitStrategy.class.getSimpleName(),
                    KubernetesContainer.class.getSimpleName()
            ));
        }

        LOG.info("Waiting for Deployment to be ready: {}/{}", namespace, name);
        final KubernetesContainer<?> k8s = (KubernetesContainer<?>) waitStrategyTarget;
        final TinyK8sClient client = TinyK8sClient.fromKubeconfig(k8s.getKubeconfig());
        Awaitility.await(format("Deployment %s/%s", namespace, name))
                .pollInSameThread()
                .pollDelay(0, SECONDS)
                .pollInterval(100, MILLISECONDS)
                .timeout(120, SECONDS)
                .until(() -> client.apps().v1().deployments().inNamespace(namespace).find(name)
                        .map(value -> Optional.ofNullable(value.getStatus())
                                .map(BaseStatus::getConditions)
                                .orElse(emptyList())
                                .stream()
                                .anyMatch(this::isReady))
                        .orElse(false));
    }

    private boolean isReady(DeploymentCondition it) {
        return "Available".equals(it.getType()) && "True".equals(it.getStatus());
    }

    public static DeploymentAvailableWaitStrategy deploymentIsAvailable(
            final String namespace,
            final String name
    ) {
        return new DeploymentAvailableWaitStrategy(namespace, name);
    }
}
