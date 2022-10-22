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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.dajudge.kindcontainer.Utils.prefixLines;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

public class BaseSidecarContainer<T extends BaseSidecarContainer<T>> extends GenericContainer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseSidecarContainer.class);
    private static final String KUBECONFIG_PATH = "/tmp/kindcontainer.kubeconfig";
    private final KubeConfigSupplier kubeConfigSupplier;
    private final Logger log;
    private boolean kubeConfigWritten = false;

    protected BaseSidecarContainer(
            final DockerImageName dockerImageName,
            final KubeConfigSupplier kubeConfigSupplier
    ) {
        this(LOG, dockerImageName, kubeConfigSupplier);
    }

    protected BaseSidecarContainer(
            final Logger log,
            final DockerImageName dockerImageName,
            final KubeConfigSupplier kubeConfigSupplier
    ) {
        super(dockerImageName);
        this.log = log;
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
        void safeExecInContainer(final List<String> maskStrings, final String... cmd) throws IOException, InterruptedException, ExecutionException;

        default void safeExecInContainer(final String... cmd) throws IOException, InterruptedException, ExecutionException {
            safeExecInContainer(emptyList(), cmd);
        }
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

    protected void safeExecInContainer(final List<String> maskStrings, final String... cmd) throws IOException, InterruptedException, ExecutionException {
        log.info("Executing command: {}", mask(maskStrings, join(" ", Arrays.asList(cmd))));
        final ExecResult result = execInContainer(cmd);
        log.trace("{}", prefixLines(mask(maskStrings, result.getStdout()), "STDOUT: "));
        log.trace("{}", prefixLines(mask(maskStrings, result.getStderr()), "STDERR: "));
        if (result.getExitCode() != 0) {
            throw new ExecutionException(cmd, result);
        }
    }

    private String mask(final List<String> maskStrings, final String string) {
        return maskStrings.stream()
                .map(maskString -> (Function<String, String>) in -> in.replace(maskString, "*****"))
                .reduce(Function.identity(), Function::andThen)
                .apply(string);
    }

    private synchronized void writeKubeConfig() {
        if (!kubeConfigWritten) {
            final String kubeconfig = kubeConfigSupplier.kubeconfig();
            copyFileToContainer(Transferable.of(kubeconfig.getBytes(UTF_8)), KUBECONFIG_PATH);
        }
        kubeConfigWritten = true;
    }
}
