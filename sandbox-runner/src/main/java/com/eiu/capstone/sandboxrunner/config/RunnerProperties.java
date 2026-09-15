package com.eiu.capstone.sandboxrunner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sandbox.runner")
public class RunnerProperties {

    private String token = "";
    private String image = "oop-autograder-sandbox-worker:latest";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? "" : token;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
