package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.model.v1.Node;
import com.dajudge.kindcontainer.client.model.v1.NodeCondition;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class KubernetesWithKubeletContainer<T extends KubernetesWithKubeletContainer<T>> extends KubernetesContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesWithKubeletContainer.class);
    private Duration startupTimeout = Duration.ofSeconds(300);

    KubernetesWithKubeletContainer(KubernetesImageSpec<?> imageSpec) {
        super(imageSpec);
    }

    public abstract T withNodePortRange(final int minPort, final int maxPort);

    /**
     * Sets the timeout applied when waiting for the Kubernetes node to become ready.
     *
     * @param startupTimeout the timeout
     * @return <code>this</code>
     */
    public T withNodeReadyTimeout(final Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
        return self();
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo, final boolean reused) {
        waitForNodeReady();
        super.containerIsStarting(containerInfo, reused);
    }

    private void waitForNodeReady() {
        LOG.info("Waiting for a node to become ready...");
        final Node readyNode = Awaitility.await("Ready node")
                .pollInSameThread()
                .pollDelay(0, MILLISECONDS)
                .pollInterval(100, MILLISECONDS)
                .ignoreExceptions()
                .timeout(startupTimeout)
                .until(this::findReadyNode, Objects::nonNull);
        LOG.info("Node ready: {}", readyNode.getMetadata().getName());
    }

    private Node findReadyNode() {
        final Predicate<NodeCondition> isReadyStatus = cond ->
                "Ready".equals(cond.getType()) && "True".equals(cond.getStatus());
        final Predicate<Node> nodeIsReady = node -> node.getStatus().getConditions().stream()
                .anyMatch(isReadyStatus);
        final TinyK8sClient client = client();
        try {
            return client.v1().nodes().list().getItems().stream()
                    .peek(it -> LOG.trace("{} -> {}", it.getMetadata().getName(), it.getStatus().getConditions()))
                    .filter(nodeIsReady)
                    .findAny()
                    .orElse(null);
        } catch (final Exception e) {
            LOG.debug("Failed to list ready nodes", e);
            return null;
        }
    }
}
