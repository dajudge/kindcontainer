package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.CONTAINERS_WITH_KUBELET;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static org.junit.jupiter.api.Assertions.fail;

public class ServiceAccountTest {

    @ParameterizedTest
    @MethodSource(CONTAINERS_WITH_KUBELET)
    public void creates_client_for_service_account(final Supplier<KubernetesWithKubeletContainer<?>> factory) {
        runWithK8s(createContainer(factory), this::assertCreatesClientForServiceAccount);
    }

    private void assertCreatesClientForServiceAccount(KubernetesWithKubeletContainer<?> k8s) {
        // First do a sanity check w/ admin privileges
        final String kubeconfig1 = k8s.getKubeconfig();
        try (final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeconfig1))) {
            client.pods().inNamespace("my-namespace").list();
            client.inNamespace("my-namespace").secrets().list();
        }

        // Now try again with limited privileges
        final String kubeconfig2 = k8s.getServiceAccountKubeconfig("my-namespace", "my-service-account");
        try (final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeconfig2))) {
            client.pods().inNamespace("my-namespace").list();
            try {
                client.inNamespace("my-namespace").secrets().list();
                fail("Should not be able to list secrets");
            } catch (final KubernetesClientException e) {
                // expected
            }
        }
    }

    private static KubernetesWithKubeletContainer<?> createContainer(
            final Supplier<KubernetesWithKubeletContainer<?>> factory
    ) {
        return factory.get().withKubectl(kubectl -> kubectl.apply
                .fileFromClasspath("manifests/serviceaccount1.yaml")
                .run());
    }
}
