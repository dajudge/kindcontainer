package com.dajudge.kindcontainer;

/*
 * This file is loosely inspired by https://github.com/testcontainers/testcontainers-java/blob/1.16.3/modules/k3s/src/main/java/org/testcontainers/k3s/K3sContainer.java
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2019 Richard North
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.testcontainers.containers.BindMode;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;

import static com.dajudge.kindcontainer.KubernetesVersionEnum.latest;
import static com.dajudge.kindcontainer.client.KubeConfigUtils.replaceServerInKubeconfig;
import static com.github.dockerjava.api.model.DockerObjectAccessor.overrideRawValue;

public class K3sContainer<SELF extends K3sContainer<SELF>> extends KubernetesWithKubeletContainer<SELF> {
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final HashMap<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "");
        put("/var/run", "");
    }};
    private int minNodePort = 30000;
    private int maxNodePort = 32767;

    public K3sContainer() {
        this(latest(K3sContainerVersion.class));
    }

    public K3sContainer(final K3sContainerVersion version) {
        this(DockerImageName.parse(String.format("rancher/k3s:%s-k3s1", version.descriptor().getKubernetesVersion())));
    }

    public K3sContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        this
                .withExposedPorts(INTERNAL_API_SERVER_PORT)
                .withPrivilegedMode(true)
                .withCreateContainerCmdModifier(it -> overrideRawValue(it.getHostConfig(), "CgroupnsMode", "host"))
                .withFileSystemBind("/sys/fs/cgroup", "/sys/fs/cgroup", BindMode.READ_WRITE)
                .withTmpFs(TMP_FILESYSTEMS);
    }

    @Override
    public void start() {
        this.withCommand(
                "server",
                "--no-deploy=traefik",
                "--tls-san=" + this.getHost(),
                String.format("--service-node-port-range=%d-%d", minNodePort, maxNodePort)
        );
        super.start();
    }

    @Override
    public SELF withNodePortRange(int minPort, int maxPort) {
        this.minNodePort = minPort;
        this.maxNodePort = maxPort;
        return self();
    }

    @Override
    public int getInternalPort() {
        return INTERNAL_API_SERVER_PORT;
    }

    @Override
    protected String getKubeconfig(final String server) {
        return replaceServerInKubeconfig(server, getOriginalKubeconfig());
    }

    private String getOriginalKubeconfig() {
        return copyFileFromContainer("/etc/rancher/k3s/k3s.yaml", Utils::readString);
    }
}
