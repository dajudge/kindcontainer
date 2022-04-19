package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.exception.ExecutionException;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BaseSidecarContainerTest {
    private final Logger logger = mock(Logger.class);

    private static class TestSidecarContainer extends BaseSidecarContainer<TestSidecarContainer> {
        protected TestSidecarContainer(
                final Logger log,
                final DockerImageName dockerImageName,
                final KubeConfigSupplier kubeConfigSupplier
        ) {
            super(log, dockerImageName, kubeConfigSupplier);
        }
    }

    private final BaseSidecarContainer<?> container = new TestSidecarContainer(
            logger,
            DockerImageName.parse("busybox"),
            () -> "kubeconfig"
    );

    @Before
    public void start() {
        container.start();
    }

    @After
    public void stop() {
        container.stop();
    }

    @Test
    public void masks_strings_in_command_output() throws IOException, ExecutionException, InterruptedException {
        container.safeExecInContainer(singletonList("lolcats123"), "echo", "lolcats123");
        verify(logger).info("Executing command: {}", "echo *****");
    }

    @Test
    public void masks_strings_in_stdout() throws IOException, ExecutionException, InterruptedException {
        container.safeExecInContainer(singletonList("lolcats123"), "echo", "lolcats123");
        verify(logger).trace("{}", "STDOUT: *****\n");
    }

    @Test
    public void masks_strings_in_stderr() throws IOException, ExecutionException, InterruptedException {
        container.safeExecInContainer(singletonList("lolcats123"), "sh", "-c", "echo lolcats123 >&2");
        verify(logger).trace("{}", "STDERR: *****\n");
    }
}
