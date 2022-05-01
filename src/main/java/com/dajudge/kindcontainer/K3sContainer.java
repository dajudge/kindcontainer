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

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;
import org.testcontainers.utility.DockerImageName;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static com.dajudge.kindcontainer.client.KubeConfigUtils.replaceServerInKubeconfig;
import static com.github.dockerjava.api.model.DockerObjectAccessor.overrideRawValue;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class K3sContainer<SELF extends K3sContainer<SELF>> extends KubernetesWithKubeletContainer<SELF> {
    private static final Logger LOG = LoggerFactory.getLogger(K3sContainer.class);
    private static final int INTERNAL_API_SERVER_PORT = 6443;
    private static final HashMap<String, String> TMP_FILESYSTEMS = new HashMap<String, String>() {{
        put("/run", "");
        put("/var/run", "");
    }};
    private int minNodePort = 30000;
    private int maxNodePort = 32767;

    public K3sContainer() {
        this(Version.getLatest());
    }

    public K3sContainer(final Version version) {
        this(DockerImageName.parse(String.format(
                "rancher/k3s:%s-k3s%d",
                version.descriptor.getKubernetesVersion(),
                version.k3sVersion
        )));
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
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        new LogMessageWaitStrategy().withRegEx(".*Node controller sync successful.*")
                .waitUntilReady(this);
        super.containerIsStarting(containerInfo);
    }

    @Override
    protected String getKubeconfig(final String server) {
        return replaceServerInKubeconfig(server, getOriginalKubeconfig());
    }

    private String getOriginalKubeconfig() {
        return copyFileFromContainer("/etc/rancher/k3s/k3s.yaml", Utils::readString);
    }


    /**
     * The available k3s versions.
     */
    public enum Version {
        VERSION_1_21_9_K3S1(new KubernetesVersionDescriptor(1, 21, 9), 1),
        VERSION_1_22_6_K3S1(new KubernetesVersionDescriptor(1, 22, 6), 1),
        VERSION_1_23_3_K3S1(new KubernetesVersionDescriptor(1, 23, 3), 1);

        private static final Comparator<Version> COMPARE_ASCENDING = comparator();
        private static final Comparator<Version> COMPARE_DESCENDING = COMPARE_ASCENDING.reversed();
        @VisibleForTesting
        final KubernetesVersionDescriptor descriptor;

        final int k3sVersion;

        Version(final KubernetesVersionDescriptor descriptor, final int k3sVersion) {
            this.descriptor = descriptor;
            this.k3sVersion = k3sVersion;
        }

        /**
         * Returns the latest supported version.
         *
         * @return the latest supported version.
         */
        public static Version getLatest() {
            return descending().get(0);
        }

        /**
         * Returns the list of available versions in descending order (latest is first).
         *
         * @return the list of available versions in descending order (latest is first).
         */
        public static List<Version> descending() {
            return Stream.of(Version.values())
                    .sorted(COMPARE_DESCENDING)
                    .collect(toList());
        }

        public KubernetesVersionDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public String toString() {
            return format(
                    "%d.%d.%d-k3s%d",
                    descriptor.getMajor(),
                    descriptor.getMinor(),
                    descriptor.getPatch(),
                    k3sVersion
            );
        }

        private static Comparator<Version> comparator() {
            return Comparator.<Version, KubernetesVersionDescriptor>comparing(a -> a.descriptor)
                    .thenComparingInt(it -> it.k3sVersion);
        }
    }
}
