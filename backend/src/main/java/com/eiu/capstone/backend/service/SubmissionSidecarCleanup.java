package com.eiu.capstone.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SubmissionSidecarCleanup {

    private final SubmissionCompileErrorStore compileErrorStore;
    private final SubmissionPackageNormalizationStore packageNormalizationStore;
    private final SubmissionMmdMetaStore mmdMetaStore;
    private final ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore;

    public SubmissionSidecarCleanup(
            SubmissionCompileErrorStore compileErrorStore,
            SubmissionPackageNormalizationStore packageNormalizationStore,
            SubmissionMmdMetaStore mmdMetaStore,
            ParsedSubmissionSnapshotStore parsedSubmissionSnapshotStore) {
        this.compileErrorStore = compileErrorStore;
        this.packageNormalizationStore = packageNormalizationStore;
        this.mmdMetaStore = mmdMetaStore;
        this.parsedSubmissionSnapshotStore = parsedSubmissionSnapshotStore;
    }

    public void deleteForSubmission(UUID submissionId) {
        if (submissionId == null) {
            return;
        }
        compileErrorStore.deleteIfExists(submissionId);
        packageNormalizationStore.deleteIfExists(submissionId);
        mmdMetaStore.deleteIfExists(submissionId);
        parsedSubmissionSnapshotStore.deleteIfExists(submissionId);
    }
}
