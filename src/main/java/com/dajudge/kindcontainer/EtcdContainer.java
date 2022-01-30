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

import com.dajudge.kindcontainer.pki.CertAuthority;
import com.dajudge.kindcontainer.pki.KeyStoreWrapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;

import java.util.Map;

import static com.dajudge.kindcontainer.Utils.writeAsciiFile;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

class EtcdContainer extends GenericContainer<EtcdContainer> {
    private static final String DOCKER_BASE_PATH = "/docker";
    private static final String RUN_SCRIPT_PATH = DOCKER_BASE_PATH + "/run-etcd.sh";
    private static final String ENTRYPOINT_PATH = DOCKER_BASE_PATH + "/entrypoint-etcd.sh";
    private static final String SERVER_CERT_PATH = DOCKER_BASE_PATH + "/server.crt";
    private static final String SERVER_KEY_PATH = DOCKER_BASE_PATH + "/server.key";
    private static final String SERVER_CACERTS_PATH = DOCKER_BASE_PATH + "/ca.crt";
    private static final String IP_ADDRESS_PATH = DOCKER_BASE_PATH + "/ip.txt";
    private static final int ETCD_PORT = 2379;
    private static final String ETCD_IMAGE = "k8s.gcr.io/etcd:3.4.13-0";
    private final CertAuthority etcdCa = new CertAuthority(System::currentTimeMillis, "CN=etcd CA");

    EtcdContainer(final Network network) {
        super(ETCD_IMAGE);
        this
                .withNetwork(network)
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint(ENTRYPOINT_PATH);
                    cmd.withCmd(RUN_SCRIPT_PATH);
                })
                .withEnv("SERVER_CERT_PATH", SERVER_CERT_PATH)
                .withEnv("SERVER_KEY_PATH", SERVER_KEY_PATH)
                .withEnv("SERVER_CACERTS_PATH", SERVER_CACERTS_PATH)
                .withEnv("IP_ADDRESS_PATH", IP_ADDRESS_PATH)
                .withCopyFileToContainer(forClasspathResource("scripts/entrypoint-etcd.sh", 755), ENTRYPOINT_PATH)
                .withCopyFileToContainer(forClasspathResource("scripts/run-etcd.sh", 755), RUN_SCRIPT_PATH)
                .withExposedPorts(ETCD_PORT)
                .waitingFor(new WaitForPortsExternallyStrategy());
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        final String etcdIpAddress = getEtcdIpAddress();
        final KeyStoreWrapper etcdKeypair = etcdCa.newKeyPair(
                "CN=etcd",
                singletonList(new GeneralName(GeneralName.iPAddress, etcdIpAddress))
        );
        writeAsciiFile(this, etcdKeypair.getCertificatePem(), SERVER_CERT_PATH);
        writeAsciiFile(this, etcdKeypair.getPrivateKeyPem(), SERVER_KEY_PATH);
        writeAsciiFile(this, etcdCa.getCaKeyStore().getCertificatePem(), SERVER_CACERTS_PATH);
        writeAsciiFile(this, String.format("%s\n", etcdIpAddress), IP_ADDRESS_PATH);
    }

    public String getEtcdIpAddress() {
        final Map<String, ContainerNetwork> networks = getContainerInfo().getNetworkSettings().getNetworks();
        return networks.values().iterator().next().getIpAddress();
    }

    public KeyStoreWrapper newClientKeypair(final String dn) {
        return etcdCa.newKeyPair(dn, emptyList());
    }

    protected String getCaCertificatePem() {
        return etcdCa.getCaKeyStore().getCertificatePem();
    }
}
