package com.eiu.capstone.backend.DTO;

public class ChallengeUploadResult {

    private final String challengeName;
    private final int mmdFileCount;
    private final int classFileCount;

    public ChallengeUploadResult(String challengeName, int mmdFileCount, int classFileCount) {
        this.challengeName = challengeName;
        this.mmdFileCount = mmdFileCount;
        this.classFileCount = classFileCount;
    }

    public String getChallengeName() { return challengeName; }
    public int getMmdFileCount() { return mmdFileCount; }
    public int getClassFileCount() { return classFileCount; }
}