package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import com.dajudge.kindcontainer.Utils.ThrowingRunnable;
import com.dajudge.kindcontainer.client.TinyK8sClient;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.UserSpec;
import com.dajudge.kindcontainer.client.model.base.Metadata;
import com.dajudge.kindcontainer.client.model.v1.ObjectReference;
import com.dajudge.kindcontainer.client.model.v1.Secret;
import com.dajudge.kindcontainer.client.model.v1.ServiceAccount;
import com.dajudge.kindcontainer.helm.Helm3Container;
import com.dajudge.kindcontainer.kubectl.KubectlContainer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.dajudge.kindcontainer.client.KubeConfigUtils.parseKubeConfig;
import static com.dajudge.kindcontainer.client.KubeConfigUtils.serializeKubeConfig;
import static com.dajudge.kindcontainer.kubectl.KubectlContainer.DEFAULT_KUBECTL_IMAGE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Base64.getDecoder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public abstract class KubernetesContainer<T extends KubernetesContainer<T>> extends BaseGenericContainer<T> {
    private final List<ThrowingRunnable<Exception>> postStartupExecutions = new ArrayList<>();
    private Helm3Container<?> helm3;
    private KubectlContainer<?, T> kubectl;

    public KubernetesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        this.withExposedPorts(getInternalPort())
                .waitingFor(new WaitForPortsExternallyStrategy())
                .withStartupTimeout(Duration.of(300, ChronoUnit.SECONDS));
    }

    /**
     * The hostname of the API server in the container's docker network.
     *
     * @return the internal hostname
     */
    public final String getInternalHostname() {
        return "localhost";
    }

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
    public String getInternalKubeconfig() {
        return getKubeconfig(format("https://%s:%d", getInternalHostname(), getInternalPort()));
    }

    protected abstract String getKubeconfig(final String server);

    public T withHelm3(final ThrowingConsumer<Helm3Container<?>, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(helm3()));
    }

    public T withKubectl(final ThrowingConsumer<KubectlContainer<?, T>, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(kubectl()));
    }

    public T withKubeconfig(final ThrowingConsumer<String, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(getKubeconfig()));
    }

    public synchronized Helm3Container<?> helm3() {
        if (helm3 == null) {
            helm3 = new Helm3Container<>(this::getInternalKubeconfig)
                    .withNetworkMode("container:" + getContainerId());
            helm3.start();
        }
        return helm3;
    }

    public synchronized KubectlContainer<?, T> kubectl() {
        if (kubectl == null) {
            kubectl = new KubectlContainer<>(DEFAULT_KUBECTL_IMAGE, this::getInternalKubeconfig, self())
                    .withNetworkMode("container:" + getContainerId());
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
     * Returns a kubeconfig that can be used for access from the outside (e.g. the JVM of JUnit tests).
     *
     * @return the kubeconfig
     */
    public final String getKubeconfig() {
        return getKubeconfig(format("https://%s:%d", getHost(), getMappedPort(getInternalPort())));
    }

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
    protected void containerIsStarting(final InspectContainerResponse containerInfo, final boolean reused) {
        super.containerIsStarting(containerInfo, reused);
        if (!reused) {
            runPostAvailabilityExecutions();
        }
    }

    protected TinyK8sClient client() {
        return TinyK8sClient.fromKubeconfig(getKubeconfig());
    }

    /**
     * Returns a kubeconfig that can be used for access from the outside (e.g. the JVM of JUnit tests) that is
     * restricted to the permissions of a provided <code>ServiceAccount</code>.
     *
     * @param serviceAccountNamespace the namespace of the service account
     * @param serviceAccountName      the name of the service account
     * @return the kubeconfig
     */
    public String getServiceAccountKubeconfig(final String serviceAccountNamespace, final String serviceAccountName) {
        return getServiceAccountKubeconfig(serviceAccountNamespace, serviceAccountName, true);
    }

    /**
     * Returns a kubeconfig that can be used for access from the outside (e.g. the JVM of JUnit tests) that is
     * restricted to the permissions of a provided <code>ServiceAccount</code>.
     *
     * @param serviceAccountNamespace the namespace of the service account
     * @param serviceAccountName      the name of the service account
     * @param autoCreateToken         if <code>true</code>, the service account will be created if it doesn't exist
     * @return the kubeconfig
     */
    public String getServiceAccountKubeconfig(
            final String serviceAccountNamespace,
            final String serviceAccountName,
            final boolean autoCreateToken
    ) {
        final KubeConfig kubeconfig = parseKubeConfig(getKubeconfig());
        final UserSpec userSpec = new UserSpec();
        userSpec.setToken(getServiceAccountToken(serviceAccountNamespace, serviceAccountName, autoCreateToken, client()));
        kubeconfig.getUsers().get(0).setUser(userSpec);
        return serializeKubeConfig(kubeconfig);
    }

    private String getServiceAccountToken(
            final String serviceAccountNamespace,
            final String serviceAccountName,
            final boolean autoCreateToken,
            final TinyK8sClient client
    ) {
        final Secret secret = getServiceAccountSecret(serviceAccountNamespace, serviceAccountName, autoCreateToken, client);
        final String token = secret.getData().get("token");
        if (token == null) {
            final String saName = serviceAccountNamespace + "/" + serviceAccountName;
            throw new RuntimeException(String.format("No token found in service account secret: %s", saName));
        }
        return new String(getDecoder().decode(token), UTF_8);
    }

    @NotNull
    private Secret getServiceAccountSecret(String serviceAccountNamespace, String serviceAccountName, boolean autoCreateToken, TinyK8sClient client) {
        final String saName = serviceAccountNamespace + "/" + serviceAccountName;
        final ServiceAccount sa = client.v1().serviceAccounts()
                .inNamespace(serviceAccountNamespace)
                .find(serviceAccountName)
                .orElseThrow(() -> new RuntimeException(format("ServiceAccount %s not found", saName)));
        if (sa.getSecrets() == null || sa.getSecrets().isEmpty()) {
            if (autoCreateToken) {
                return createServiceAccountToken(serviceAccountNamespace, serviceAccountName, client);
            } else {
                throw new RuntimeException(format("ServiceAccount %s has no secrets", saName));
            }
        }
        final ObjectReference secretRef = sa.getSecrets().get(0);
        final String secretNamespace = Optional.ofNullable(secretRef.getNamespace()).orElse(serviceAccountNamespace);
        final String secretName = secretRef.getName();
        final Secret secret = client.v1().secrets().inNamespace(secretNamespace).find(secretName)
                .orElseThrow(() -> new RuntimeException(format("Secret %s/%s not found", secretNamespace, secretName)));
        if (!"kubernetes.io/service-account-token".equals(secret.getType())) {
            throw new RuntimeException(format("Secret %s/%s is not of type kubernetes.io/service-account-token", secretNamespace, secretName));
        }
        return secret;
    }

    private Secret createServiceAccountToken(String serviceAccountNamespace, String serviceAccountName, TinyK8sClient client) {
        Secret secret = new Secret();
        secret.setKind("Secret");
        secret.setApiVersion("v1");
        secret.setType("kubernetes.io/service-account-token");
        secret.setMetadata(new Metadata());
        secret.getMetadata().setName(String.format("kindcontainer-%s", UUID.randomUUID()));
        secret.getMetadata().setNamespace(serviceAccountNamespace);
        secret.getMetadata().setAnnotations(new HashMap<>());
        secret.getMetadata().getAnnotations().put("kubernetes.io/service-account.name", serviceAccountName);
        client.v1().secrets()
                .inNamespace(serviceAccountNamespace)
                .create(secret);
        final String saName = format("%s/%s", secret.getMetadata().getNamespace(), secret.getMetadata().getName());
        return await("Token for service account secret " + saName)
                .atMost(10, SECONDS)
                .until(
                        () -> client.v1().secrets().inNamespace(serviceAccountNamespace)
                                .find(secret.getMetadata().getName()),
                        it -> it.map(Secret::getData).map(data -> data.get("token")).isPresent()
                )
                .orElseThrow(() -> new RuntimeException("No token found in secret: " + saName));
    }
}
