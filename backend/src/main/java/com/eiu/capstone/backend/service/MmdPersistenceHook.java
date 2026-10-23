package com.eiu.capstone.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * Extension point for persisting uploaded {@code .mmd} files. The default no-op
 * implementation keeps MMD off the compile-and-grade hot path until MMD grading
 * or archival is implemented.
 */
public interface MmdPersistenceHook {

    void onUploadComplete(String irn, String requestId, Map<String, List<MultipartFile>> mmdByChallenge);
}
