package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import static org.junit.Assert.fail;

public class ServiceAccountTest extends BaseFullContainersTest {

    public ServiceAccountTest(final KubernetesWithKubeletContainer<?> k8s) {
        super(k8s.withKubectl(kubectl -> kubectl.apply
                .fileFromClasspath("manifests/serviceaccount1.yaml")
                .run()));
        }

    @Test
    public void creates_client_for_service_account() {
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
}
