package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;
import org.junit.Test;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

public class KubectlCreateSecretFluentUnitTest {
    private final BaseSidecarContainer.ExecInContainer exec = mock(BaseSidecarContainer.ExecInContainer.class);
    private final CreateSecretDockerRegistryFluent<?> underTest = new CreateSecretDockerRegistryFluent<>(exec);

    @Test
    public void masksPassword() throws IOException, ExecutionException, InterruptedException {
        underTest.dockerPassword("lolcats123").run("mySecret");
        verify(exec).safeExecInContainer(eq(singletonList("lolcats123")), any(String.class));
    }
}
