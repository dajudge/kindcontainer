package com.dajudge.kindcontainer.kubectl;

import com.dajudge.kindcontainer.BaseSidecarContainer;
import com.dajudge.kindcontainer.exception.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateSecretDockerRegistryFluent<P> {
    private final BaseSidecarContainer.ExecInContainer exec;
    private final P parent;
    private String namespace;
    private String dockerUsername;
    private String dockerPassword;
    private String dockerServer;
    private String dockerEmail;

    public CreateSecretDockerRegistryFluent(
            final BaseSidecarContainer.ExecInContainer exec,
            final P parent
    ) {
        this.exec = exec;
        this.parent = parent;
    }

    public CreateSecretDockerRegistryFluent<P> dockerUsername(final String username) {
        this.dockerUsername = username;
        return this;
    }

    public CreateSecretDockerRegistryFluent<P> dockerPassword(final String password) {
        this.dockerPassword = password;
        return this;
    }

    public CreateSecretDockerRegistryFluent<P> dockerServer(final String server) {
        this.dockerServer = server;
        return this;
    }

    public CreateSecretDockerRegistryFluent<P> dockerEmail(final String email) {
        this.dockerEmail = email;
        return this;
    }

    public CreateSecretDockerRegistryFluent<P> namespace(final String namespace) {
        this.namespace = namespace;
        return this;
    }

    public P run(final String name) throws IOException, ExecutionException, InterruptedException {
        try {
            final List<String> maskStrings = new ArrayList<>();
            final List<String> cmdline = new ArrayList<>();
            cmdline.add("kubectl");
            cmdline.add("create");
            cmdline.add("secret");
            cmdline.add("docker-registry");
            if (dockerUsername != null) {
                cmdline.add("--docker-username");
                cmdline.add(dockerUsername);
            }
            if (dockerPassword != null) {
                cmdline.add("--docker-password");
                cmdline.add(dockerPassword);
                maskStrings.add(dockerPassword);
            }
            if (dockerServer != null) {
                cmdline.add("--docker-server");
                cmdline.add(dockerServer);
            }
            if (dockerEmail != null) {
                cmdline.add("--docker-email");
                cmdline.add(dockerEmail);
            }
            if(namespace != null) {
                cmdline.add("-n");
                cmdline.add(namespace);
            }
            cmdline.add(name);
            exec.safeExecInContainer(maskStrings, cmdline.toArray(new String[]{}));
        } finally {
            resetFluent();
        }
        return parent;
    }

    private void resetFluent() {
        dockerUsername = null;
        dockerPassword = null;
        dockerServer = null;
        dockerEmail = null;
        namespace = null;
    }
}
