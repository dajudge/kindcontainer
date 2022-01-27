package com.dajudge.kindcontainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NetworkingTest extends BaseCommonTest {

    public NetworkingTest(final KubernetesContainer<?> k8s) {
        super(k8s);
    }

    @Test
    public void can_connect_internally() {
        try (final GenericContainer<?> curl = createContainer()) {
            curl.start();
            try {
                final String url = String.format("https://%s:%d", k8s.getInternalHostname(), k8s.getInternalPort());
                final Container.ExecResult result = curl.execInContainer("curl", "-v", url);
                System.out.println(result.getStdout());
                System.out.println(result.getStderr());
                assertEquals(0, result.getExitCode());
            } catch (final IOException | InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private GenericContainer<?> createContainer() {
        return new GenericContainer<>("curlimages/curl:7.81.0")
                .withNetwork(k8s.getNetwork());
    }
}
