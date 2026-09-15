package com.eiu.capstone.sandboxrunner.docker;

public interface ContainerFactory {

    String createIdleContainer();

    void destroyContainer(String containerId);
}
