package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import com.dajudge.kindcontainer.Utils.ThrowingFunction;
import com.dajudge.kindcontainer.Utils.ThrowingRunnable;
import com.dajudge.kindcontainer.helm.Helm3Container;
import com.dajudge.kindcontainer.kubectl.KubectlContainer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.dajudge.kindcontainer.kubectl.KubectlContainer.DEFAULT_KUBECTL_IMAGE;
import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static java.util.Arrays.asList;

public abstract class KubernetesContainer<T extends KubernetesContainer<T>> extends GenericContainer<T> {
    private final List<ThrowingRunnable<Exception>> postStartupExecutions = new ArrayList<>();
    private Helm3Container<?> helm3;
    private KubectlContainer<?> kubectl;

    public KubernetesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        this.withExposedPorts(getInternalPort()).waitingFor(new WaitForPortsExternallyStrategy());
    }

    public void runWithClient(final ThrowingConsumer<DefaultKubernetesClient, Exception> consumer) {
        runWithClient(client -> {
            consumer.accept(client);
            return null;
        });
    }

    public <R> R runWithClient(final ThrowingFunction<DefaultKubernetesClient, R, Exception> function) {
        try (final DefaultKubernetesClient client = newClient()) {
            try {
                return function.apply(client);
            } catch (final Exception e) {
                throw new RuntimeException("Error running with client", e);
            }
        }
    }

    /**
     * Returns a fabric8 Kubernetes client with administrative access.
     *
     * @return a <code>DefaultKubernetesClient</code> with cluster-admin permissions
     */
    public DefaultKubernetesClient newClient() {
        return new DefaultKubernetesClient(fromKubeconfig(getExternalKubeconfig()));
    }

    /**
     * The hostname of the API server in the container's docker network.
     *
     * @return the internal hostname
     */
    public abstract String getInternalHostname();

    /**
     * The port of the API server in the container's docker network.
     *
     * @return the internal API server port
     */
    public abstract int getInternalPort();

    /**
     * Returns a kubeconfig that can be used for access from the container's docker network.
     *
     * @return the kubeconfig
     */
    public abstract String getInternalKubeconfig();

    public T withHelm3(final ThrowingConsumer<Helm3Container<?>, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(helm3()));
    }

    public T withKubectl(final ThrowingConsumer<KubectlContainer<?>, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(kubectl()));
    }

    public synchronized Helm3Container<?> helm3() {
        if (helm3 == null) {
            helm3 = new Helm3Container<>(this::getInternalKubeconfig, getNetwork());
            helm3.start();
        }
        return helm3;
    }

    public synchronized KubectlContainer<?> kubectl() {
        if (kubectl == null) {
            kubectl = new KubectlContainer<>(DEFAULT_KUBECTL_IMAGE, this::getInternalKubeconfig).withNetwork(getNetwork());
            kubectl.start();
        }
        return kubectl;
    }

    private void runPostAvailabilityExecutions() {
        postStartupExecutions.forEach(r -> {
            try {
                r.run();
            } catch (final Exception e) {
                throw new RuntimeException("Failed to execute post startup runnable", e);
            }
        });
    }

    /**
     * Returns a kubeconfig that can be used for access from the outside.
     *
     * @return the kubeconfig
     */
    public abstract String getExternalKubeconfig();

    @Override
    public void stop() {
        try {
            if (helm3 != null) {
                helm3.stop();
            }
        } finally {
            try {
                if (kubectl != null) {
                    kubectl.stop();
                }
            } finally {
                super.stop();
            }
        }
    }

    protected T withPostStartupExecution(final ThrowingRunnable<Exception> runnable) {
        postStartupExecutions.add(runnable);
        return self();
    }

    @Override
    public T withExposedPorts(final Integer... ports) {
        final HashSet<Integer> exposedPorts = new HashSet<>(asList(ports));
        exposedPorts.add(getInternalPort());
        return super.withExposedPorts(exposedPorts.toArray(new Integer[]{}));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        runPostAvailabilityExecutions();
        super.containerIsStarting(containerInfo);
    }
}
