package com.dajudge.kindcontainer;

import org.junit.Test;

import static com.dajudge.kindcontainer.TestUtils.runWithClient;
import static org.junit.Assert.assertTrue;

public class ApiServerContainerTest {

    @Test
    public void starts_apiserver() {
        runWithClient(StaticContainers.apiServer(), client -> {
            assertTrue(client.nodes().list().getItems().toString(), client.nodes().list().getItems().isEmpty());
        });
    }
}