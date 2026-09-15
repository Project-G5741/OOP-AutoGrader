package com.eiu.capstone.sandboxrunner.docker;

import java.util.Map;

import com.eiu.capstone.sandboxrunner.config.RunnerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import org.springframework.stereotype.Component;

@Component
public class SandboxContainerFactory implements ContainerFactory {

    private static final long MEMORY_BYTES = 128L * 1024 * 1024;
    private static final long PIDS_LIMIT = 64L;

    private final DockerClient dockerClient;
    private final RunnerProperties runnerProperties;

    public SandboxContainerFactory(DockerClient dockerClient, RunnerProperties runnerProperties) {
        this.dockerClient = dockerClient;
        this.runnerProperties = runnerProperties;
    }

    public String createIdleContainer() {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode("none")
                .withMemory(MEMORY_BYTES)
                .withCpuQuota(50_000L)
                .withCpuPeriod(100_000L)
                .withPidsLimit(PIDS_LIMIT)
                .withTmpFs(Map.of("/work", "rw,size=67108864"));

        CreateContainerResponse response = dockerClient.createContainerCmd(runnerProperties.getImage())
                .withHostConfig(hostConfig)
                .withUser("sandbox")
                .withReadonlyRootfs(true)
                .withCmd("sleep", "infinity")
                .exec();
        String id = response.getId();
        dockerClient.startContainerCmd(id).exec();
        return id;
    }

    public void destroyContainer(String containerId) {
        if (containerId == null || containerId.isBlank()) {
            return;
        }
        try {
            dockerClient.killContainerCmd(containerId).exec();
        } catch (Exception ignored) {
            // already stopped
        }
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception ignored) {
            // best effort
        }
    }
}
