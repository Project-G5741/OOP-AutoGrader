package com.eiu.capstone.backend.DTO.feedback;

import java.util.List;
import java.util.UUID;

public record FeedbackConsumeRepliesRequest(List<UUID> ids) {
}
