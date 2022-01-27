/*
Copyright 2020-2021 Alex Stockinger

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

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class KubernetesContainer<T extends KubernetesContainer<T>> extends GenericContainer<T> {
    public abstract DefaultKubernetesClient getClient();

    public KubernetesContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public void withClient(final Consumer<DefaultKubernetesClient> callable) {
        withClient(client -> {
            callable.accept(client);
            return null;
        });
    }

    public <R> R withClient(final Function<DefaultKubernetesClient, R> callable) {
        try (final DefaultKubernetesClient client = getClient()) {
            return callable.apply(client);
        }
    }
}
