package com.dajudge.kindcontainer;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class K3sContainerTest {
    private static final String KUBECONFIG_PATH = "/etc/rancher/k3s/k3s.yaml";
    private static final String KUBECONFIG = """
            clusters:
            - cluster:
                server: https://127.0.0.1:6443
              name: default
            """;

    @Test
    void retriesFailedKubeconfigReadAndCachesFirstSuccessfulRead() throws Exception {
        final ExecResult failed = mock(ExecResult.class);
        when(failed.getExitCode()).thenReturn(1);
        when(failed.getStderr()).thenReturn("not ready");

        final ExecResult succeeded = mock(ExecResult.class);
        when(succeeded.getExitCode()).thenReturn(0);
        when(succeeded.getStdout()).thenReturn(KUBECONFIG);

        final K3sContainer<?> container = spy(new K3sContainer<>());
        doReturn(failed, succeeded).when(container).execInContainer("cat", KUBECONFIG_PATH);

        assertThrows(IllegalStateException.class, () -> container.getKubeconfig("https://localhost:6443"));
        container.getKubeconfig("https://localhost:6443");
        container.getKubeconfig("https://localhost:6443");

        verify(container, times(2)).execInContainer("cat", KUBECONFIG_PATH);
    }
}
