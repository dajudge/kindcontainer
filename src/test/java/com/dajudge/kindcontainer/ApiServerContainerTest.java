package com.dajudge.kindcontainer;

import org.junit.Rule;
import org.junit.Test;

import static com.dajudge.kindcontainer.util.TestUtils.runWithClient;
import static org.junit.Assert.assertTrue;

public class ApiServerContainerTest {
    @Rule
    public final ApiServerContainer<?> apiServer = new ApiServerContainer<>();

    @Test
    public void starts_apiserver() {
        runWithClient(apiServer, client -> {
            assertTrue(client.nodes().list().getItems().toString(), client.nodes().list().getItems().isEmpty());
        });
    }
}