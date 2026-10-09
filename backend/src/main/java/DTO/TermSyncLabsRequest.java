package com.eiu.capstone.backend.DTO;

import java.util.List;
import java.util.UUID;

public record TermSyncLabsRequest(List<UUID> sourceLabIds) {
}
