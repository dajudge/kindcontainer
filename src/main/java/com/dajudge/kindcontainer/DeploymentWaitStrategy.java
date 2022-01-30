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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DeploymentWaitStrategy extends AbstractWaitStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DeploymentWaitStrategy.class);
    private final String namespace;
    private final String name;

    public DeploymentWaitStrategy(final String namespace, final String name) {
        this.namespace = namespace;
        this.name = name;
    }

    @Override
    protected void waitUntilReady() {
        if (!(waitStrategyTarget instanceof KubernetesContainer)) {
            throw new IllegalArgumentException(format(
                    "%s only works with a %s",
                    DeploymentWaitStrategy.class.getSimpleName(),
                    KubernetesContainer.class.getSimpleName()
            ));
        }
        LOG.info("Waiting for Deployment to be ready: {}/{}", namespace, name);
        final KubernetesContainer<?> k8s = (KubernetesContainer<?>) waitStrategyTarget;
        k8s.runWithClient(client -> {
            client.apps().deployments().inNamespace(namespace).withName(name).waitUntilReady(120, SECONDS);
        });
    }

    public static DeploymentWaitStrategy deploymentIsReady(final String namespace, final String name) {
        return new DeploymentWaitStrategy(namespace, name);
    }
}
