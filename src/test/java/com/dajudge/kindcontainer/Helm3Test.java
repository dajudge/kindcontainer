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

import com.dajudge.kindcontainer.exception.ExecutionException;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.UUID;

import static com.dajudge.kindcontainer.TestUtils.createNewNamespace;

@RunWith(Parameterized.class)
public class Helm3Test extends BaseCommonTest {

    public Helm3Test(final KubernetesContainer<?> k8s) {
        super(k8s);
    }

    @Test
    public void can_install_something() throws IOException, ExecutionException, InterruptedException {
        final String namespace = k8s.withClient(TestUtils::createNewNamespace);
        k8s.helm3().repo.add.run("mittwald", "https://helm.mittwald.de");
        k8s.helm3().repo.update.run();
        k8s.helm3().install
                .namespace(namespace)
                .run("kubernetes-replicator", "mittwald/kubernetes-replicator");
    }
}
