package com.eiu.capstone.backend.grading.testcase.transport;

public final class WorkerTransportUrls {

    private WorkerTransportUrls() {
    }

    public static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
