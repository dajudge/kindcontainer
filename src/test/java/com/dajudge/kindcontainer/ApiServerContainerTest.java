package com.dajudge.kindcontainer;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ApiServerContainerTest {

    @Test
    public void starts_apiserver() {
        try (final KubernetesClient client = StaticContainers.apiServer().newClient()) {
            assertTrue(client.nodes().list().getItems().isEmpty());
        }
    }
}