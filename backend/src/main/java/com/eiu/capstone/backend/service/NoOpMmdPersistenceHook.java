package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class NoOpMmdPersistenceHook implements MmdPersistenceHook {

    @Override
    public void onUploadComplete(String irn, String requestId, Map<String, List<MultipartFile>> mmdByChallenge) {
    }
}
