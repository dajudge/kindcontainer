package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class ServiceAccountTest {
    @ClassRule
    public static final KindContainer<?> k8s = new KindContainer<>()
            .withKubectl(kubectl -> {
                kubectl.apply
                        .withFile(forClasspathResource("manifests/serviceaccount1.yaml"), "/tmp/manifest.yaml")
                        .run("/tmp/manifest.yaml");
            });

    @Test
    public void creates_client_for_service_account() {
        k8s.runWithClient("my-namespace", "my-service-account", client -> {
            client.inNamespace("my-namespace").pods().list();
            try {
                client.inNamespace("my-namespace").secrets().list();
                fail("Should not be able to list secrets");
            } catch (final KubernetesClientException e) {
                // expected
            }
        });
    }
}
