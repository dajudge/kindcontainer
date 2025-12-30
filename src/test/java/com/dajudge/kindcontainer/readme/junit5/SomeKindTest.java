package com.dajudge.kindcontainer.readme.junit5;

import com.dajudge.kindcontainer.KindContainer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class SomeKindTest {
    @Container
    public static final KindContainer<?> KUBE = new KindContainer<>();

    @Test
    public void verify_node_is_present() {
        // Create a fabric8 client and use it!
        try (final KubernetesClient client = new KubernetesClientBuilder().withConfig(fromKubeconfig(KUBE.getKubeconfig())).build()) {
            assertEquals(1, client.nodes().list().getItems().size());
        }
    }
}