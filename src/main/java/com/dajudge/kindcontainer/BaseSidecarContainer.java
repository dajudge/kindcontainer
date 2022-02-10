package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.exception.ExecutionException;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static com.dajudge.kindcontainer.Utils.prefixLines;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;

public class BaseSidecarContainer<T extends BaseSidecarContainer<T>> extends GenericContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseSidecarContainer.class);
    private static final String KUBECONFIG_PATH = "/tmp/helmcontainer.kubeconfig";
    private final KubeConfigSupplier kubeConfigSupplier;
    private boolean kubeConfigWritten = false;

    protected BaseSidecarContainer(final DockerImageName dockerImageName, final KubeConfigSupplier kubeConfigSupplier) {
        super(dockerImageName);
        this.kubeConfigSupplier = kubeConfigSupplier;
        this.withEnv("KUBECONFIG", KUBECONFIG_PATH)
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("sh", "-c", "trap 'echo signal;exit 0' SIGTERM; while : ; do sleep 1 ; done");
                });
    }

    public interface FileTarget {
        void copyFileToContainer(Transferable file, String path);
    }

    public interface ExecInContainer {
        void safeExecInContainer(final String... cmd) throws IOException, InterruptedException, ExecutionException;
    }

    @Override
    public ExecResult execInContainer(final String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        writeKubeConfig();
        return super.execInContainer(command);
    }

    @Override
    public ExecResult execInContainer(final Charset outputCharset, final String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        writeKubeConfig();
        return super.execInContainer(outputCharset, command);
    }

    protected void safeExecInContainer(final String... cmd) throws IOException, InterruptedException, ExecutionException {
        LOG.info("Executing command: {}", join(" ", Arrays.asList(cmd)));
        final ExecResult result = execInContainer(cmd);
        LOG.trace("{}", prefixLines(result.getStdout(), "STDOUT: "));
        LOG.trace("{}", prefixLines(result.getStderr(), "STDERR: "));
        if (result.getExitCode() != 0) {
            throw new ExecutionException(cmd, result);
        }
    }

    private synchronized void writeKubeConfig() {
        if (!kubeConfigWritten) {
            final String kubeconfig = kubeConfigSupplier.kubeconfig();
            copyFileToContainer(Transferable.of(kubeconfig.getBytes(UTF_8)), KUBECONFIG_PATH);
        }
        kubeConfigWritten = true;
    }
}
