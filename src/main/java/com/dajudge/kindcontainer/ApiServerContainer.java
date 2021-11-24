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

import com.dajudge.kindcontainer.pki.CertAuthority;
import com.dajudge.kindcontainer.pki.KeyStoreWrapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.GeneralName;

import java.util.Base64;
import java.util.Map;

import static com.dajudge.kindcontainer.Utils.writeAsciiFile;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

public class ApiServerContainer extends BusyBoxContainer<ApiServerContainer> {
    private static final String API_SERVER_IMAGE = "k8s.gcr.io/kube-apiserver:v1.21.1";
    private static final String PKI_BASEDIR = "/etc/kubernetes/pki";
    private static final String ETCD_PKI_BASEDIR = PKI_BASEDIR + "/etcd";
    private static final String ETCD_CLIENT_KEY = ETCD_PKI_BASEDIR + "/etcd/apiserver-client.key";
    private static final String ETCD_CLIENT_CERT = ETCD_PKI_BASEDIR + "/etcd/apiserver-client.crt";
    private static final String ETCD_CLIENT_CA = ETCD_PKI_BASEDIR + "/etcd/ca.crt";
    private static final String API_SERVER_CA = PKI_BASEDIR + "/ca.crt";
    private static final String API_SERVER_CERT = PKI_BASEDIR + "/apiserver.crt";
    private static final String API_SERVER_KEY = PKI_BASEDIR + "/apiserver.key";
    private static final String API_SERVER_PUBKEY = PKI_BASEDIR + "/apiserver.pub";
    private static final String DOCKER_BASE_PATH = "/docker";
    private static final String RUN_SCRIPT_PATH = DOCKER_BASE_PATH + "/run-apiserver.sh";
    private static final String ENTRYPOINT_PATH = DOCKER_BASE_PATH + "/entrypoint-etcd.sh";
    private static final String IP_ADDRESS_PATH = DOCKER_BASE_PATH + "/ip.txt";
    private static final String ETCD_HOSTNAME_PATH = DOCKER_BASE_PATH + "/etcd.txt";
    private final CertAuthority apiServerCa = new CertAuthority(System::currentTimeMillis, "CN=API Server CA");
    private final EtcdContainer etcd;
    private final Config config = Config.empty();

    public ApiServerContainer() {
        this(API_SERVER_IMAGE);
    }

    public ApiServerContainer(final String apiServerImage) {
        super(apiServerImage);
        etcd = new EtcdContainer();
        this
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint(ENTRYPOINT_PATH);
                    cmd.withCmd(RUN_SCRIPT_PATH);
                })
                .withEnv("ETCD_CLIENT_KEY", ETCD_CLIENT_KEY)
                .withEnv("ETCD_CLIENT_CERT", ETCD_CLIENT_CERT)
                .withEnv("ETCD_CLIENT_CA", ETCD_CLIENT_CA)
                .withEnv("API_SERVER_CA", API_SERVER_CA)
                .withEnv("API_SERVER_CERT", API_SERVER_CERT)
                .withEnv("API_SERVER_KEY", API_SERVER_KEY)
                .withEnv("API_SERVER_PUBKEY", API_SERVER_PUBKEY)
                .withEnv("IP_ADDRESS_PATH", IP_ADDRESS_PATH)
                .withEnv("ETCD_HOSTNAME_PATH", ETCD_HOSTNAME_PATH)
                .withCopyFileToContainer(forClasspathResource("scripts/entrypoint-apiserver.sh", 755), ENTRYPOINT_PATH)
                .withCopyFileToContainer(forClasspathResource("scripts/run-apiserver.sh", 755), RUN_SCRIPT_PATH)
                .withExposedPorts(6443);
    }

    public DefaultKubernetesClient getClient() {
        return new DefaultKubernetesClient(config);
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo) {
        etcd.start();
        try {
            final String apiServerIpAddress = getApiServerIpAddress();
            final KeyStoreWrapper apiServerKeyPair = apiServerCa.newKeyPair("O=system:masters,CN=kubernetes-admin", asList(
                    new GeneralName(GeneralName.iPAddress, apiServerIpAddress),
                    new GeneralName(GeneralName.dNSName, "localhost"),
                    new GeneralName(GeneralName.iPAddress, "127.0.0.1")
            ));
            final KeyStoreWrapper etcdClientKeyPair = etcd.newClientKeypair("CN=API Server");
            writeAsciiFile(this, etcdClientKeyPair.getCertificatePem(), ETCD_CLIENT_CERT);
            writeAsciiFile(this, etcdClientKeyPair.getPrivateKeyPem(), ETCD_CLIENT_KEY);
            writeAsciiFile(this, etcd.getCaCertificatePem(), ETCD_CLIENT_CA);
            writeAsciiFile(this, apiServerKeyPair.getCertificatePem(), API_SERVER_CERT);
            writeAsciiFile(this, apiServerKeyPair.getPrivateKeyPem(), API_SERVER_KEY);
            writeAsciiFile(this, apiServerKeyPair.getPublicKeyPem(), API_SERVER_PUBKEY);
            writeAsciiFile(this, apiServerCa.getCaKeyStore().getCertificatePem(), API_SERVER_CA);
            writeAsciiFile(this, apiServerIpAddress, IP_ADDRESS_PATH);
            writeAsciiFile(this, etcd.getEtcdIpAddress(), ETCD_HOSTNAME_PATH);
            config.setCaCertData(base64(apiServerCa.getCaKeyStore().getCertificatePem()));
            config.setClientCertData(base64(apiServerKeyPair.getCertificatePem()));
            config.setClientKeyData(base64(apiServerKeyPair.getPrivateKeyPem()));
            config.setMasterUrl(format("https://%s:%d", getContainerIpAddress(), getMappedPort(6443)));
            config.setConnectionTimeout(10000);
            config.setRequestTimeout(60000);
        } catch (final RuntimeException e) {
            etcd.close();
            throw e;
        }
    }

    private String base64(final String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(US_ASCII));
    }

    public String getApiServerIpAddress() {
        final Map<String, ContainerNetwork> networks = getContainerInfo().getNetworkSettings().getNetworks();
        return networks.values().iterator().next().getIpAddress();
    }

    @Override
    public void stop() {
        super.stop();
        etcd.stop();
    }
}
