package com.dajudge.kindcontainer.client;

import com.dajudge.kindcontainer.client.config.KubeConfig;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.Map;

import static com.dajudge.kindcontainer.client.Deserialization.JSON_MAPPER;

public final class KubeConfigUtils {
    private static final Yaml YAML = new Yaml();

    public KubeConfigUtils() {
        throw new UnsupportedOperationException("Do not instantiate!");
    }

    public static KubeConfig parseKubeConfig(final String kubeconfig) {
        try {
            final String jsonString = JSON_MAPPER.writeValueAsString(YAML.<Map<?, ?>>load(kubeconfig));
            return JSON_MAPPER.readValue(jsonString, KubeConfig.class);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to deserialize kubeconfig", e);
        }
    }

    public static String serializeKubeConfig(final KubeConfig config) {
        try {
            return YAML.dump(JSON_MAPPER.readValue(JSON_MAPPER.writeValueAsString(config), Map.class));
        } catch (final IOException e) {
            throw new RuntimeException("Failed to serialize kubeconfig", e);
        }
    }

    public static String replaceServerInKubeconfig(final String server, final String string) {
        final KubeConfig kubeconfig = parseKubeConfig(string);
        kubeconfig.getClusters().get(0).getCluster().setServer(server);
        return serializeKubeConfig(kubeconfig);
    }
}
