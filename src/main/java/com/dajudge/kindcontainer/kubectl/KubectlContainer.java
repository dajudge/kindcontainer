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
package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.helm.KubeConfigSupplier;
import org.testcontainers.utility.DockerImageName;

public class KubectlContainer<T extends KubectlContainer<T>> extends BaseSidecarContainer<T> {
    public static final DockerImageName DEFAULT_KUBECTL_IMAGE = DockerImageName.parse("bitnami/kubectl:1.21.9-debian-10-r10");

    public ApplyFluent apply = new ApplyFluent(this::safeExecInContainer, this::copyFileToContainer);
    public DeleteFluent delete = new DeleteFluent(this::safeExecInContainer);

    public KubectlContainer(final DockerImageName dockerImageName, final KubeConfigSupplier kubeConfigSupplier) {
        super(dockerImageName, kubeConfigSupplier);
    }
}
