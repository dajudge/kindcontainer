/*
Copyright 2020-2022 Alex Stockinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.dajudge.kindcontainer;

import com.dajudge.kindcontainer.exception.ExecutionException;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BaseSidecarContainer<T extends BaseSidecarContainer<T>> extends GenericContainer<T> {
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
        final ExecResult result = execInContainer(cmd);
        if (result.getExitCode() != 0) {
            throw new ExecutionException(cmd, result);
        }
    }

    private synchronized void writeKubeConfig() {
        if (!kubeConfigWritten) {
            copyFileToContainer(Transferable.of(kubeConfigSupplier.kubeconfig().getBytes(UTF_8)), KUBECONFIG_PATH);
        }
        kubeConfigWritten = true;
    }
}
