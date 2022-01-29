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

import com.dajudge.kindcontainer.BaseSidecarContainer.ExecInContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class InstallFluent {

    private final ExecInContainer c;
    private String namespace;
    private boolean createNamespace;
    private Map<String, String> params = new HashMap<>();

    public InstallFluent(final ExecInContainer c) {
        this.c = c;
    }

    public InstallFluent set(final String key, final String value) {
        params.put(key, value);
        return this;
    }

    public InstallFluent namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public InstallFluent createNamespace(final boolean createNamespace) {
        this.createNamespace = createNamespace;
        return this;
    }

    public void run(final String releaseName, final String chart) throws IOException, InterruptedException, ExecutionException {
        try {
            final List<String> cmdline = new ArrayList<>(asList("helm", "install"));
            if (namespace != null) {
                cmdline.addAll(asList("--namespace", namespace));
            }
            if (createNamespace) {
                cmdline.add("--create-namespace");
            }
            params.forEach((k, v) -> {
                cmdline.addAll(asList("--set", String.format("%s=%s", k, v)));
            });
            cmdline.addAll(asList(releaseName, chart));
            c.safeExecInContainer(cmdline.toArray(new String[]{}));
        } finally {
            createNamespace = false;
            namespace = null;
            params.clear();
        }
    }
}
