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

import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.dajudge.kindcontainer.TestUtils.stringResource;
import static java.util.Collections.singletonList;

public class IngressNginxTest {

    @ClassRule
    public static KindContainer<?> KIND = new KindContainer<>()
            .withHelm3(helm -> {
                helm.repo.add.run("ingress-nginx", "https://kubernetes.github.io/ingress-nginx");
                helm.repo.update.run();
                helm.install
                        .namespace("ingress-nginx")
                        .createNamespace(true)
                        .set("controller.service.type", "NodePort")
                        .set("controller.service.nodePorts.http", "30080")
                        .run("ingress-nginx", "ingress-nginx/ingress-nginx");
            });

    @Test
    public void ingress_deployment_becomes_ready() {
        KIND.runWithClient(client -> {
            LoggerFactory.getLogger(IngressNginxTest.class).info("Waiting for ingress-nginx deployment to become ready");
            client.inNamespace("ingress-nginx").apps().deployments()
                    .withName("ingress-nginx-controller")
                    .waitUntilReady(1200, TimeUnit.SECONDS);
        });
    }
}
