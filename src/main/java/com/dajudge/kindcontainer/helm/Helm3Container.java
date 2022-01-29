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
package com.dajudge.kindcontainer.helm;


import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class Helm3Container<SELF extends Helm3Container<SELF>> extends BaseSidecarContainer<SELF> {
    private static final String DEFAULT_HELM_IMAGE = "alpine/helm:3.7.2";

    public final RepoFluent repo = new RepoFluent(this::safeExecInContainer);
    public final InstallFluent install = new InstallFluent(this::safeExecInContainer);

    public Helm3Container(final KubeConfigSupplier kubeConfigSupplier, final Network network) {
        this(DockerImageName.parse(DEFAULT_HELM_IMAGE), kubeConfigSupplier, network);
    }

    public Helm3Container(
            final DockerImageName dockerImageName,
            final KubeConfigSupplier kubeConfigSupplier,
            final Network network
    ) {
        super(dockerImageName, kubeConfigSupplier);
        this.withNetwork(network);
    }


}
