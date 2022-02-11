package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class ServiceAccountTest {
    @ClassRule
    public static final KindContainer<?> k8s = new KindContainer<>(KindContainer.Version.VERSION_1_21_2)
            .withKubectl(kubectl -> kubectl.apply
                    .withFile(forClasspathResource("manifests/serviceaccount1.yaml"), "/tmp/manifest.yaml")
                    .run("/tmp/manifest.yaml"));

    @Test
    public void creates_client_for_service_account() {
        // First do a sanity check w/ admin privileges
        final String kubeconfig1 = k8s.getKubeconfig();
        try(final DefaultKubernetesClient client = new DefaultKubernetesClient(Config.fromKubeconfig(kubeconfig1))) {
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
}
