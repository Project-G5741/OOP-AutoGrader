package com.eiu.capstone.backend.DTO;

import java.util.List;

public record AdminMascotBulkGrantResult(
        int usersUpdated,
        int petsGranted,
        int skippedAlreadyOwned,
        List<AdminUserMascotRow> updatedRows) {
}
