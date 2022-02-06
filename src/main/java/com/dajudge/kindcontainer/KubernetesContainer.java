package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import com.dajudge.kindcontainer.Utils.ThrowingRunnable;
import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.UserSpec;
import com.dajudge.kindcontainer.client.model.base.Metadata;
import com.dajudge.kindcontainer.client.model.v1.Namespace;
import com.dajudge.kindcontainer.client.model.v1.ObjectReference;
import com.dajudge.kindcontainer.client.model.v1.Secret;
import com.dajudge.kindcontainer.client.model.v1.ServiceAccount;
import com.dajudge.kindcontainer.helm.Helm3Container;
import com.dajudge.kindcontainer.kubectl.KubectlContainer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;

import static com.dajudge.kindcontainer.client.KubeConfigUtils.parseKubeConfig;
import static com.dajudge.kindcontainer.client.KubeConfigUtils.serializeKubeConfig;
import static com.dajudge.kindcontainer.kubectl.KubectlContainer.DEFAULT_KUBECTL_IMAGE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;

public abstract class KubernetesContainer<T extends KubernetesContainer<T>> extends GenericContainer<T> {
    private final List<ThrowingRunnable<Exception>> postStartupExecutions = new ArrayList<>();
    private Helm3Container<?> helm3;
    private KubectlContainer<?> kubectl;

    public KubernetesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        this.withExposedPorts(getInternalPort())
                .waitingFor(new WaitForPortsExternallyStrategy())
                .withStartupTimeout(Duration.of(300, SECONDS));
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

    protected TinyK8sClient client() {
        return TinyK8sClient.fromKubeconfig(getExternalKubeconfig());
    }

    public String getServiceAccountKubeconfig(
            final String serviceAccountNamespace,
            final String serviceAccountName
    ) {
        final KubeConfig kubeconfig = parseKubeConfig(getExternalKubeconfig());
        final UserSpec userSpec = new UserSpec();
        userSpec.setToken(getServiceAccountToken(serviceAccountNamespace, serviceAccountName, client()));
        kubeconfig.getUsers().get(0).setUser(userSpec);
        return serializeKubeConfig(kubeconfig);
    }

    private String getServiceAccountToken(
            final String serviceAccountNamespace,
            final String serviceAccountName,
            final TinyK8sClient client
    ) {
        final String saName = serviceAccountNamespace + "/" + serviceAccountName;
        final ServiceAccount sa = client.v1().serviceAccounts().inNamespace(serviceAccountNamespace).find(serviceAccountName)
                .orElseThrow(() -> new RuntimeException(format("ServiceAccount %s not found", saName)));
        if (sa.getSecrets() == null || sa.getSecrets().isEmpty()) {
            throw new RuntimeException(format("ServiceAccount %s has no secrets", saName));
        }
        final ObjectReference secretRef = sa.getSecrets().get(0);
        final String secretNamespace = Optional.ofNullable(secretRef.getNamespace()).orElse(serviceAccountNamespace);
        final String secretName = secretRef.getName();
        final Secret secret = client.v1().secrets().inNamespace(secretNamespace).find(secretName)
                .orElseThrow(() -> new RuntimeException(format("Secret %s/%s not found", secretNamespace, secretName)));
        if (!"kubernetes.io/service-account-token".equals(secret.getType())) {
            throw new RuntimeException(format("Secret %s/%s is not of type kubernetes.io/service-account-token", secretNamespace, secretName));
        }
        return new String(Base64.getDecoder().decode(secret.getData().get("token")), UTF_8);
    }

    /**
     * Creates a new namespace.
     *
     * @param name the name of the namespace to create
     * @return the created namespace
     */
    public String createNamespace(final String name) {
        final Namespace namespace = new Namespace();
        namespace.setMetadata(new Metadata());
        namespace.getMetadata().setName(name);
        client().v1().namespaces().create(namespace);
        return name;
    }

    /**
     * Creates a new namespace with a random name.
     *
     * @return the created namespace
     */
    public String createNamespace() {
        return createNamespace(UUID.randomUUID().toString());
    }
}
