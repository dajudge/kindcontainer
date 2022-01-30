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

import com.dajudge.kindcontainer.Utils.ThrowingConsumer;
import com.dajudge.kindcontainer.Utils.ThrowingRunnable;
import com.dajudge.kindcontainer.helm.Helm3Container;
import com.dajudge.kindcontainer.kubectl.KubectlContainer;
import com.github.dockerjava.api.command.InspectContainerResponse;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.dajudge.kindcontainer.kubectl.KubectlContainer.DEFAULT_KUBECTL_IMAGE;
import static java.util.Arrays.asList;

public abstract class KubernetesContainer<T extends KubernetesContainer<T>> extends GenericContainer<T> {
    public abstract DefaultKubernetesClient getClient();

    private final List<ThrowingRunnable<Exception>> postStartupExecutions = new ArrayList<>();
    private Helm3Container<?> helm3;
    private KubectlContainer<?> kubectl;

    public KubernetesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        this.withExposedPorts(getInternalPort())
                .waitingFor(new WaitForPortsExternallyStrategy());
    }

    public void runWithClient(final Consumer<DefaultKubernetesClient> callable) {
        runWithClient(client -> {
            callable.accept(client);
            return null;
        });
    }

    public <R> R runWithClient(final Function<DefaultKubernetesClient, R> callable) {
        try (final DefaultKubernetesClient client = getClient()) {
            return callable.apply(client);
        }
    }

    public abstract String getInternalHostname();

    public abstract int getInternalPort();

    public abstract String getInternalKubeconfig();

    public T withHelm3(final ThrowingConsumer<Helm3Container<?>, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(helm3()));
    }

    public T withKubectl(final ThrowingConsumer<KubectlContainer<?>, Exception> consumer) {
        return withPostStartupExecution(() -> consumer.accept(kubectl()));
    }

    public synchronized Helm3Container<?> helm3() {
        if (helm3 == null) {
            helm3 = new Helm3Container<>(this::getInternalKubeconfig, getNetwork());
            helm3.start();
        }
        return helm3;
    }

    public synchronized KubectlContainer<?> kubectl() {
        if (kubectl == null) {
            kubectl = new KubectlContainer<>(DEFAULT_KUBECTL_IMAGE, this::getInternalKubeconfig)
                    .withNetwork(getNetwork());
            kubectl.start();
        }
        return kubectl;
    }

    private void runPostAvailabilityExecutions() {
        postStartupExecutions.forEach(
                r -> {
                    try {
                        r.run();
                    } catch (final Exception e) {
                        throw new RuntimeException("Failed to execute post startup runnable", e);
                    }
                }
        );
    }

    @Override
    public void stop() {
        try {
            if (helm3 != null) {
                helm3.stop();
            }
        } finally {
            try {
                if (kubectl != null) {
                    kubectl.stop();
                }
            } finally {
                super.stop();
            }
        }
    }

    protected T withPostStartupExecution(final ThrowingRunnable<Exception> runnable) {
        postStartupExecutions.add(runnable);
        return self();
    }

    @Override
    public T withExposedPorts(final Integer... ports) {
        final HashSet<Integer> exposedPorts = new HashSet<>(asList(ports));
        exposedPorts.add(getInternalPort());
        return super.withExposedPorts(exposedPorts.toArray(new Integer[]{}));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        runPostAvailabilityExecutions();
        super.containerIsStarting(containerInfo);
    }
}
