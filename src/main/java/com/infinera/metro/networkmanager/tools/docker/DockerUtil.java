package com.infinera.metro.networkmanager.tools.docker;

import com.google.common.collect.ImmutableMap;
import com.palantir.docker.compose.DockerComposeRule;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.NetworkSettings;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

public enum DockerUtil {
    DOCKER_UTIL;

    public String getContainerIpAddress(String containerName) {
        if(isRunningInDockerContainer()) {
            return getContainerIpAddressJvmInsideDockerContainer(containerName);
        } else {
            return getContainerIpAddressJvmInDevEnvironment(containerName);
        }
    }

    private String getContainerIpAddressJvmInsideDockerContainer(String containerName) {
        try {
            return InetAddress.getByName(containerName).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getContainerIpAddressJvmInDevEnvironment(String containerName) {
        final DockerClient dockerClient;
        try {
            dockerClient = DefaultDockerClient.fromEnv().build();
            final ContainerInfo info = dockerClient.inspectContainer(containerName);
            final NetworkSettings networkSettings = info.networkSettings();
            final ImmutableMap<String, AttachedNetwork> networks = networkSettings.networks();
            return Objects.requireNonNull(networks).entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(attachedNetwork -> Objects.requireNonNull(attachedNetwork.aliases()).contains(containerName))
                .findFirst()
                .orElseThrow(RuntimeException::new)
                .ipAddress();
        } catch (DockerCertificateException | InterruptedException | DockerException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean isRunningInDockerContainer() {
        return Files.exists(Paths.get("/.dockerenv"));
    }

    @Deprecated
    public String getContainerIpAddress(DockerComposeRule dockerComposeRule, String nodeName) throws IOException {
        final InputStream inputStream = dockerComposeRule
            .dockerExecutable()
            .execute("inspect", "-f", "'{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'", nodeName)
            .getInputStream();
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name())
            .replaceAll("\n","")
            .replaceAll("\'","");
    }
}