package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.util.ContainerVersionHelpers.KubernetesTestPackage;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.kubeletContainers;
import static com.dajudge.kindcontainer.util.ContainerVersionHelpers.runWithK8s;
import static org.junit.jupiter.api.Assertions.fail;

public class ServiceAccountTest {

    @TestFactory
    public Stream<DynamicTest> creates_client_for_service_account() {
        return kubeletContainers(this::assertCreatesClientForServiceAccount);
    }

    private void assertCreatesClientForServiceAccount(final KubernetesTestPackage<? extends KubernetesWithKubeletContainer<?>> testPkg) {
        runWithK8s(configureContainer(testPkg.newContainer()), k8s -> {
            final String kubeconfig1 = k8s.getKubeconfig();
            try (final NamespacedKubernetesClient client = new KubernetesClientBuilder()
                    .withConfig(Config.fromKubeconfig(kubeconfig1))
                    .build()
                    .adapt(NamespacedKubernetesClient.class)) {
                client.pods().inNamespace("my-namespace").list();
                client.inNamespace("my-namespace").secrets().list();
            }

            final String kubeconfig2 = k8s.getServiceAccountKubeconfig("my-namespace", "my-service-account");
            try (final NamespacedKubernetesClient client = new KubernetesClientBuilder()
                    .withConfig(Config.fromKubeconfig(kubeconfig2))
                    .build()
                    .adapt(NamespacedKubernetesClient.class)) {
                client.pods().inNamespace("my-namespace").list();
                try {
                    client.inNamespace("my-namespace").secrets().list();
                    fail("Should not be able to list secrets");
                } catch (final KubernetesClientException e) {
                    // expected
                }
            }
        });
    }

    private static KubernetesWithKubeletContainer<?> configureContainer(
            final KubernetesWithKubeletContainer<?> container
    ) {
        return container.withKubectl(kubectl -> kubectl.apply
                .fileFromClasspath("manifests/serviceaccount1.yaml")
                .run());
    }
}
