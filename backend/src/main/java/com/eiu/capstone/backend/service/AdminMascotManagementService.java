package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.AdminGachaResourceUpdateRequest;
import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantRequest;
import com.eiu.capstone.backend.DTO.AdminMascotBulkGrantResult;
import com.eiu.capstone.backend.DTO.AdminMascotEquipRequest;
import com.eiu.capstone.backend.DTO.AdminMascotGrantRequest;
import com.eiu.capstone.backend.DTO.AdminMascotUpdateRequest;
import com.eiu.capstone.backend.DTO.AdminUserMascotRow;
import com.eiu.capstone.backend.model.StudentMascotPurchase;
import com.eiu.capstone.backend.model.StudentMascotPurchaseKey;
import com.eiu.capstone.backend.model.StudentStreak;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.StudentMascotPurchaseRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.utility.TimeUtil;

@Service
@Profile("!desktop & !desktop-lecturer")
public class AdminMascotManagementService {

    private static final int MAX_BULK_USERS = 500;
    private static final int MAX_PRICE_PAID = 1_000_000;
    private static final int MAX_POINTS = 100_000_000;
    private static final int MAX_CRYSTALS = 1_000_000;
    private static final int MAX_TICKETS = 1_000_000;
    private static final int MAX_GACHA_PITY = 1_000;
    private static final int MAX_EGG_SMASH_PITY = StudentEggSmashService.HARD_PITY_TURN - 1;
    private static final String EXCLUSIVE_EMAIL = "kiet.doan.cit23@eiu.edu.vn";

    private final JdbcTemplate jdbcTemplate;
    private final UserAccountRepository userAccounts;
    private final StudentStreakService streakService;
    private final StudentMascotPurchaseRepository purchases;

    public AdminMascotManagementService(
            JdbcTemplate jdbcTemplate,
            UserAccountRepository userAccounts,
            StudentStreakService streakService,
            StudentMascotPurchaseRepository purchases) {
        this.jdbcTemplate = jdbcTemplate;
        this.userAccounts = userAccounts;
        this.streakService = streakService;
        this.purchases = purchases;
    }

    public static boolean isIceQueenEligible(String studentCode, String email) {
        return StudentMascotService.EXCLUSIVE_STUDENT_CODE.equals(studentCode)
                || EXCLUSIVE_EMAIL.equalsIgnoreCase(email);
    }

    private static String resolveEquipped(String rawEquipped, boolean iceQueenEligible) {
        String canonical = MascotCatalog.canonicalId(rawEquipped);
        if (canonical == null || canonical.isBlank() || !MascotCatalog.isSelectable(canonical)) {
            return MascotCatalog.STARTER_ID;
        }
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(canonical) && !iceQueenEligible) {
            return MascotCatalog.STARTER_ID;
        }
        return canonical;
    }

    private static MascotCatalog.Offer requireValidNonStarterMascot(String rawMascotId, boolean iceQueenEligible) {
        String canonical = MascotCatalog.canonicalId(rawMascotId);
        if (canonical == null || canonical.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mascot ID is required.");
        }
        if (MascotCatalog.STARTER_ID.equals(canonical)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Fox is the starter pet and is owned by default.");
        }
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(canonical) && !iceQueenEligible) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ice Queen is exclusive to authorized account 2331200057.");
        }
        MascotCatalog.Offer offer = MascotCatalog.forSale(canonical);
        if (offer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mascot: " + rawMascotId);
        }
        return offer;
    }

    private static int validatePricePaid(Integer rawPricePaid) {
        int pricePaid = rawPricePaid == null ? 0 : rawPricePaid;
        if (pricePaid < 0 || pricePaid > MAX_PRICE_PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Price paid must be between 0 and " + MAX_PRICE_PAID + ".");
        }
        return pricePaid;
    }

    private static int clamp(long value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return (int) value;
    }

    @Transactional(readOnly = true)
    public List<AdminUserMascotRow> listUsers() {
        record RawUserProjection(
                UUID userId,
                String studentCode,
                String teacherCode,
                String fullName,
                String email,
                boolean active,
                boolean inCurrentTerm,
                String rawEquipped,
                int points,
                int crystals,
                int gachaTickets,
                int gachaPityCount,
                int eggSmashPity) {
        }

        List<RawUserProjection> users = jdbcTemplate.query("""
                SELECT ua.id,
                       ua.student_code,
                       ua.teacher_code,
                       ua.full_name,
                       ua.email,
                       ua.is_active,
                       EXISTS (
                           SELECT 1
                           FROM term_enrollment te
                           JOIN term t ON t.id = te.term_id AND t.is_current = TRUE
                           WHERE te.user_id = ua.id
                       ) AS in_current_term,
                       COALESCE(sds.equipped_mascot, 'fox') AS equipped_mascot,
                       COALESCE(sds.points, 0) AS points,
                       COALESCE(sds.crystals, 0) AS crystals,
                       COALESCE(sds.gacha_tickets, 0) AS gacha_tickets,
                       COALESCE(sds.gacha_pity_count, 0) AS gacha_pity_count,
                       COALESCE(sds.egg_smash_pity, 0) AS egg_smash_pity
                FROM user_account ua
                LEFT JOIN student_daily_streak sds ON sds.user_id = ua.id
                WHERE EXISTS (
                    SELECT 1
                    FROM user_role ur
                    JOIN role r ON r.id = ur.role_id
                    WHERE ur.user_id = ua.id
                      AND LOWER(r.name) IN ('student', 'admin')
                )
                   OR sds.user_id IS NOT NULL
                   OR EXISTS (
                    SELECT 1
                    FROM student_mascot_purchase smp
                    WHERE smp.user_id = ua.id
                )
                ORDER BY ua.full_name ASC, ua.email ASC
                """, (rs, rowNum) -> new RawUserProjection(
                rs.getObject(1, UUID.class),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getBoolean(6),
                rs.getBoolean(7),
                rs.getString(8),
                rs.getInt(9),
                rs.getInt(10),
                rs.getInt(11),
                rs.getInt(12),
                rs.getInt(13)));

        record RawPurchase(String mascotId, int pricePaid, Object purchasedAt) {
        }

        Map<UUID, List<RawPurchase>> purchasesByUser = new HashMap<>();
        jdbcTemplate.query("""
                SELECT user_id, mascot_id, price_paid, purchased_at
                FROM student_mascot_purchase
                ORDER BY purchased_at DESC, mascot_id ASC
                """, rs -> {
            UUID uid = rs.getObject(1, UUID.class);
            purchasesByUser.computeIfAbsent(uid, k -> new ArrayList<>())
                    .add(new RawPurchase(rs.getString(2), rs.getInt(3), rs.getObject(4)));
        });

        List<AdminUserMascotRow> rows = new ArrayList<>(users.size());
        for (RawUserProjection u : users) {
            boolean iceQueenEligible = isIceQueenEligible(u.studentCode(), u.email());
            String equipped = resolveEquipped(u.rawEquipped(), iceQueenEligible);
            List<RawPurchase> rawOwned = purchasesByUser.getOrDefault(u.userId(), List.of());

            Map<String, AdminUserMascotRow.OwnedPetView> byMascot = new LinkedHashMap<>();
            for (RawPurchase rp : rawOwned) {
                String canonical = MascotCatalog.canonicalId(rp.mascotId());
                if (canonical == null
                        || MascotCatalog.STARTER_ID.equals(canonical)
                        || !MascotCatalog.isSelectable(canonical)) {
                    continue;
                }
                if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(canonical) && !iceQueenEligible) {
                    continue;
                }
                if (byMascot.containsKey(canonical)) {
                    continue;
                }
                MascotCatalog.Offer offer = MascotCatalog.forSale(canonical);
                int catalogPrice = offer == null ? 0 : offer.price();
                byMascot.put(canonical, new AdminUserMascotRow.OwnedPetView(
                        canonical,
                        MascotCatalog.displayName(canonical),
                        catalogPrice,
                        rp.pricePaid(),
                        TimeUtil.formatDisplayTimestamp(rp.purchasedAt()),
                        canonical.equals(equipped)));
            }

            if (!MascotCatalog.STARTER_ID.equals(equipped) && !byMascot.containsKey(equipped)) {
                MascotCatalog.Offer offer = MascotCatalog.forSale(equipped);
                int catalogPrice = offer == null ? 0 : offer.price();
                byMascot.put(equipped, new AdminUserMascotRow.OwnedPetView(
                        equipped,
                        MascotCatalog.displayName(equipped),
                        catalogPrice,
                        0,
                        null,
                        true));
            }

            List<AdminUserMascotRow.OwnedPetView> ownedList = new ArrayList<>(byMascot.values());
            ownedList.sort(Comparator.comparing(AdminUserMascotRow.OwnedPetView::displayName, String.CASE_INSENSITIVE_ORDER));

            rows.add(new AdminUserMascotRow(
                    u.userId(),
                    u.studentCode(),
                    u.teacherCode(),
                    u.fullName(),
                    u.email(),
                    u.active(),
                    u.inCurrentTerm(),
                    iceQueenEligible,
                    equipped,
                    u.points(),
                    u.crystals(),
                    u.gachaTickets(),
                    u.gachaPityCount(),
                    u.eggSmashPity(),
                    ownedList));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public AdminUserMascotRow getUserRow(UUID userId) {
        UserAccount user = requireUser(userId);
        return buildUserRow(user);
    }

    @Transactional
    public AdminUserMascotRow updateResources(UUID userId, AdminGachaResourceUpdateRequest request) {
        if (request == null
                || (request.points() == null
                        && request.crystals() == null
                        && request.gachaTickets() == null
                        && request.gachaPityCount() == null
                        && request.eggSmashPity() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least one resource field to update.");
        }
        UserAccount user = requireUser(userId);

        if (request.points() != null && (request.points() < 0 || request.points() > MAX_POINTS)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Points must be between 0 and " + MAX_POINTS + ".");
        }
        if (request.crystals() != null && (request.crystals() < 0 || request.crystals() > MAX_CRYSTALS)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Crystals must be between 0 and " + MAX_CRYSTALS + ".");
        }
        if (request.gachaTickets() != null && (request.gachaTickets() < 0 || request.gachaTickets() > MAX_TICKETS)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tickets must be between 0 and " + MAX_TICKETS + ".");
        }
        if (request.gachaPityCount() != null
                && (request.gachaPityCount() < 0 || request.gachaPityCount() > MAX_GACHA_PITY)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Wheel pity must be between 0 and " + MAX_GACHA_PITY + ".");
        }
        if (request.eggSmashPity() != null
                && (request.eggSmashPity() < 0 || request.eggSmashPity() > MAX_EGG_SMASH_PITY)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Egg Smash pity must be between 0 and " + MAX_EGG_SMASH_PITY + ".");
        }

        StudentStreak streak = streakService.lock(userId);
        if (request.points() != null) {
            int nextPoints = request.points();
            streak.setPoints(nextPoints);
            streak.setCrystalSourcedPoints(Math.min(Math.max(0, streak.getCrystalSourcedPoints()), nextPoints));
        }
        if (request.crystals() != null) {
            streak.setCrystals(request.crystals());
        }
        if (request.gachaTickets() != null) {
            streak.setGachaTickets(request.gachaTickets());
        }
        if (request.gachaPityCount() != null) {
            streak.setGachaPityCount(request.gachaPityCount());
        }
        if (request.eggSmashPity() != null) {
            streak.setEggSmashPity(request.eggSmashPity());
        }

        streakService.save(streak);
        return buildUserRow(user);
    }

    @Transactional
    public AdminUserMascotRow grantPets(UUID userId, AdminMascotGrantRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing grant payload.");
        }
        UserAccount user = requireUser(userId);
        boolean iceQueenEligible = isIceQueenEligible(user.getStudentCode(), user.getEmail());

        List<String> requestedRaw = new ArrayList<>();
        if (request.mascotIds() != null) {
            requestedRaw.addAll(request.mascotIds());
        }
        if (request.mascotId() != null && !request.mascotId().isBlank()) {
            requestedRaw.add(request.mascotId());
        }

        Set<String> distinctIds = new LinkedHashSet<>();
        List<MascotCatalog.Offer> targets = new ArrayList<>();
        for (String raw : requestedRaw) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            MascotCatalog.Offer offer = requireValidNonStarterMascot(raw, iceQueenEligible);
            if (distinctIds.add(offer.id())) {
                targets.add(offer);
            }
        }
        if (targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one pet to grant.");
        }

        int pricePaid = validatePricePaid(request.pricePaid());
        StudentStreak streak = streakService.lock(userId);
        Set<String> existingOwned = loadOwnedCanonicalIdsAndBackfillEquipped(userId, streak, iceQueenEligible);

        OffsetDateTime now = TimeUtil.nowInVietnam();
        List<MascotCatalog.Offer> newlyGranted = new ArrayList<>();
        for (MascotCatalog.Offer offer : targets) {
            if (existingOwned.contains(offer.id())) {
                continue;
            }
            StudentMascotPurchase row = new StudentMascotPurchase();
            row.setId(new StudentMascotPurchaseKey(userId, offer.id()));
            row.setPricePaid(pricePaid);
            row.setPurchasedAt(now);
            purchases.save(row);
            existingOwned.add(offer.id());
            newlyGranted.add(offer);
        }

        if (newlyGranted.isEmpty()) {
            if (targets.size() == 1) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "User already owns " + targets.get(0).name() + ".");
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User already owns all selected pets.");
        }

        if (Boolean.TRUE.equals(request.equip())) {
            MascotCatalog.Offer toEquip = newlyGranted.get(newlyGranted.size() - 1);
            streak.setEquippedMascot(toEquip.id());
            streakService.save(streak);
        }

        purchases.flush();
        return buildUserRow(user);
    }

    @Transactional
    public AdminMascotBulkGrantResult bulkGrantPets(AdminMascotBulkGrantRequest request) {
        if (request == null || request.userIds() == null || request.userIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one user.");
        }
        Set<UUID> distinctUsers = new LinkedHashSet<>();
        for (UUID uid : request.userIds()) {
            if (uid != null) {
                distinctUsers.add(uid);
            }
        }
        if (distinctUsers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one user.");
        }
        if (distinctUsers.size() > MAX_BULK_USERS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bulk grant supports at most " + MAX_BULK_USERS + " users at once.");
        }

        Set<String> distinctMascots = new LinkedHashSet<>();
        if (request.mascotIds() != null) {
            for (String raw : request.mascotIds()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String canonical = MascotCatalog.canonicalId(raw);
                if (MascotCatalog.STARTER_ID.equals(canonical)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Fox is the starter pet and is owned by default.");
                }
                if (MascotCatalog.forSale(canonical) == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mascot: " + raw);
                }
                distinctMascots.add(canonical);
            }
        }

        int crystalsDelta = request.crystalsDelta() == null ? 0 : request.crystalsDelta();
        int ticketsDelta = request.ticketsDelta() == null ? 0 : request.ticketsDelta();
        int pointsDelta = request.pointsDelta() == null ? 0 : request.pointsDelta();

        if (Math.abs(crystalsDelta) > MAX_CRYSTALS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Crystal delta must be within ±" + MAX_CRYSTALS + ".");
        }
        if (Math.abs(ticketsDelta) > MAX_TICKETS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ticket delta must be within ±" + MAX_TICKETS + ".");
        }
        if (Math.abs(pointsDelta) > MAX_POINTS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Points delta must be within ±" + MAX_POINTS + ".");
        }

        boolean hasResourceDelta = crystalsDelta != 0 || ticketsDelta != 0 || pointsDelta != 0;
        if (distinctMascots.isEmpty() && !hasResourceDelta) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Select at least one pet or enter a crystal, ticket, or point adjustment.");
        }

        int pricePaid = validatePricePaid(request.pricePaid());
        boolean equip = Boolean.TRUE.equals(request.equip());
        OffsetDateTime now = TimeUtil.nowInVietnam();

        int usersUpdated = 0;
        int petsGranted = 0;
        int skippedAlreadyOwned = 0;
        List<AdminUserMascotRow> updatedRows = new ArrayList<>();

        for (UUID userId : distinctUsers) {
            UserAccount user = requireUser(userId);
            boolean iceQueenEligible = isIceQueenEligible(user.getStudentCode(), user.getEmail());
            StudentStreak streak = streakService.lock(userId);
            Set<String> existingOwned = loadOwnedCanonicalIdsAndBackfillEquipped(userId, streak, iceQueenEligible);

            boolean streakDirty = false;
            if (crystalsDelta != 0) {
                int nextCrystals = clamp((long) streak.getCrystals() + crystalsDelta, 0, MAX_CRYSTALS);
                if (nextCrystals != streak.getCrystals()) {
                    streak.setCrystals(nextCrystals);
                    streakDirty = true;
                }
            }
            if (ticketsDelta != 0) {
                int nextTickets = clamp((long) streak.getGachaTickets() + ticketsDelta, 0, MAX_TICKETS);
                if (nextTickets != streak.getGachaTickets()) {
                    streak.setGachaTickets(nextTickets);
                    streakDirty = true;
                }
            }
            if (pointsDelta != 0) {
                int nextPoints = clamp((long) streak.getPoints() + pointsDelta, 0, MAX_POINTS);
                if (nextPoints != streak.getPoints()) {
                    streak.setPoints(nextPoints);
                    streak.setCrystalSourcedPoints(
                            Math.min(Math.max(0, streak.getCrystalSourcedPoints()), nextPoints));
                    streakDirty = true;
                }
            }

            String lastEquipCandidate = null;
            int grantedForUser = 0;
            for (String mascotId : distinctMascots) {
                if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(mascotId) && !iceQueenEligible) {
                    continue;
                }
                lastEquipCandidate = mascotId;
                if (existingOwned.contains(mascotId)) {
                    skippedAlreadyOwned++;
                    continue;
                }
                StudentMascotPurchase row = new StudentMascotPurchase();
                row.setId(new StudentMascotPurchaseKey(userId, mascotId));
                row.setPricePaid(pricePaid);
                row.setPurchasedAt(now);
                purchases.save(row);
                existingOwned.add(mascotId);
                grantedForUser++;
                petsGranted++;
            }

            if (equip && lastEquipCandidate != null) {
                streak.setEquippedMascot(lastEquipCandidate);
                streakDirty = true;
            }
            if (streakDirty) {
                streakService.save(streak);
            }
            if (grantedForUser > 0 || streakDirty) {
                usersUpdated++;
            }
            updatedRows.add(buildUserRow(user));
        }

        purchases.flush();
        return new AdminMascotBulkGrantResult(usersUpdated, petsGranted, skippedAlreadyOwned, updatedRows);
    }

    @Transactional
    public AdminUserMascotRow updatePet(UUID userId, String currentMascotId, AdminMascotUpdateRequest request) {
        UserAccount user = requireUser(userId);
        boolean iceQueenEligible = isIceQueenEligible(user.getStudentCode(), user.getEmail());
        MascotCatalog.Offer fromOffer = requireValidNonStarterMascot(currentMascotId, iceQueenEligible);

        StudentStreak streak = streakService.lock(userId);
        List<StudentMascotPurchase> allPurchases = purchases.findByIdUserId(userId);
        List<StudentMascotPurchase> matchingFrom = new ArrayList<>();
        Set<String> otherOwnedIds = new HashSet<>();

        for (StudentMascotPurchase row : allPurchases) {
            if (row == null || row.getId() == null) {
                continue;
            }
            String canonical = MascotCatalog.canonicalId(row.getId().getMascotId());
            if (fromOffer.id().equals(canonical)) {
                matchingFrom.add(row);
            } else if (canonical != null && !canonical.isBlank()) {
                otherOwnedIds.add(canonical);
            }
        }

        String currentEquipped = resolveEquipped(streak.getEquippedMascot(), iceQueenEligible);
        boolean wasEquipped = fromOffer.id().equals(currentEquipped);
        if (!MascotCatalog.STARTER_ID.equals(currentEquipped) && !wasEquipped) {
            otherOwnedIds.add(currentEquipped);
        }

        if (matchingFrom.isEmpty() && !wasEquipped) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User does not own " + fromOffer.name() + ".");
        }

        MascotCatalog.Offer targetOffer = (request != null
                && request.targetMascotId() != null
                && !request.targetMascotId().isBlank())
                ? requireValidNonStarterMascot(request.targetMascotId(), iceQueenEligible)
                : fromOffer;

        int existingPrice = matchingFrom.isEmpty() ? 0 : matchingFrom.get(0).getPricePaid();
        int nextPricePaid = (request != null && request.pricePaid() != null)
                ? validatePricePaid(request.pricePaid())
                : existingPrice;
        OffsetDateTime existingTime = matchingFrom.isEmpty() || matchingFrom.get(0).getPurchasedAt() == null
                ? TimeUtil.nowInVietnam()
                : matchingFrom.get(0).getPurchasedAt();

        if (!targetOffer.id().equals(fromOffer.id())) {
            if (otherOwnedIds.contains(targetOffer.id())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "User already owns " + targetOffer.name() + ".");
            }
            if (!matchingFrom.isEmpty()) {
                purchases.deleteAll(matchingFrom);
                purchases.flush();
            }
            StudentMascotPurchase replacement = new StudentMascotPurchase();
            replacement.setId(new StudentMascotPurchaseKey(userId, targetOffer.id()));
            replacement.setPricePaid(nextPricePaid);
            replacement.setPurchasedAt(existingTime);
            purchases.save(replacement);

            if (request != null && request.equip() != null) {
                if (request.equip()) {
                    streak.setEquippedMascot(targetOffer.id());
                } else if (wasEquipped) {
                    streak.setEquippedMascot(MascotCatalog.STARTER_ID);
                }
            } else if (wasEquipped) {
                streak.setEquippedMascot(targetOffer.id());
            }
        } else {
            if (!matchingFrom.isEmpty()) {
                purchases.deleteAll(matchingFrom);
                purchases.flush();
            }
            StudentMascotPurchase updated = new StudentMascotPurchase();
            updated.setId(new StudentMascotPurchaseKey(userId, fromOffer.id()));
            updated.setPricePaid(nextPricePaid);
            updated.setPurchasedAt(existingTime);
            purchases.save(updated);

            if (request != null && request.equip() != null) {
                if (request.equip()) {
                    streak.setEquippedMascot(fromOffer.id());
                } else if (wasEquipped) {
                    streak.setEquippedMascot(MascotCatalog.STARTER_ID);
                }
            }
        }

        streakService.save(streak);
        purchases.flush();
        return buildUserRow(user);
    }

    @Transactional
    public AdminUserMascotRow setEquippedMascot(UUID userId, AdminMascotEquipRequest request) {
        if (request == null || request.mascotId() == null || request.mascotId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mascot ID is required.");
        }
        UserAccount user = requireUser(userId);
        boolean iceQueenEligible = isIceQueenEligible(user.getStudentCode(), user.getEmail());
        String target = MascotCatalog.canonicalId(request.mascotId());

        if (!MascotCatalog.isSelectable(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mascot: " + request.mascotId());
        }
        if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(target) && !iceQueenEligible) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ice Queen is exclusive to authorized account 2331200057.");
        }

        StudentStreak streak = streakService.lock(userId);
        Set<String> owned = loadOwnedCanonicalIdsAndBackfillEquipped(userId, streak, iceQueenEligible);

        if (!MascotCatalog.STARTER_ID.equals(target) && !owned.contains(target)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User does not own " + MascotCatalog.displayName(target) + ".");
        }

        streak.setEquippedMascot(target);
        streakService.save(streak);
        purchases.flush();
        return buildUserRow(user);
    }

    @Transactional
    public AdminUserMascotRow deletePet(UUID userId, String mascotId) {
        UserAccount user = requireUser(userId);
        boolean iceQueenEligible = isIceQueenEligible(user.getStudentCode(), user.getEmail());
        String canonical = MascotCatalog.canonicalId(mascotId);
        if (canonical == null || canonical.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mascot ID is required.");
        }
        if (MascotCatalog.STARTER_ID.equals(canonical)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Starter Fox cannot be removed.");
        }

        StudentStreak streak = streakService.lock(userId);
        List<StudentMascotPurchase> matching = new ArrayList<>();
        for (StudentMascotPurchase row : purchases.findByIdUserId(userId)) {
            if (row != null && row.getId() != null
                    && canonical.equals(MascotCatalog.canonicalId(row.getId().getMascotId()))) {
                matching.add(row);
            }
        }

        String currentEquipped = resolveEquipped(streak.getEquippedMascot(), iceQueenEligible);
        boolean wasEquipped = canonical.equals(currentEquipped);
        if (matching.isEmpty() && !wasEquipped) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not own this pet.");
        }

        if (!matching.isEmpty()) {
            purchases.deleteAll(matching);
            purchases.flush();
        }
        if (wasEquipped || canonical.equals(MascotCatalog.canonicalId(streak.getEquippedMascot()))) {
            streak.setEquippedMascot(MascotCatalog.STARTER_ID);
            streakService.save(streak);
        }

        return buildUserRow(user);
    }

    private UserAccount requireUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required.");
        }
        return userAccounts.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private Set<String> loadOwnedCanonicalIdsAndBackfillEquipped(
            UUID userId, StudentStreak streak, boolean iceQueenEligible) {
        Set<String> owned = new HashSet<>();
        for (StudentMascotPurchase row : purchases.findByIdUserId(userId)) {
            if (row == null || row.getId() == null) {
                continue;
            }
            String canonical = MascotCatalog.canonicalId(row.getId().getMascotId());
            if (canonical != null && !canonical.isBlank()) {
                owned.add(canonical);
            }
        }
        String currentEquipped = resolveEquipped(streak.getEquippedMascot(), iceQueenEligible);
        if (!MascotCatalog.STARTER_ID.equals(currentEquipped) && !owned.contains(currentEquipped)) {
            StudentMascotPurchase backfilled = new StudentMascotPurchase();
            backfilled.setId(new StudentMascotPurchaseKey(userId, currentEquipped));
            backfilled.setPricePaid(0);
            backfilled.setPurchasedAt(TimeUtil.nowInVietnam());
            purchases.save(backfilled);
            owned.add(currentEquipped);
        }
        return owned;
    }

    private AdminUserMascotRow buildUserRow(UserAccount user) {
        UUID userId = user.getId();
        boolean iceQueenEligible = isIceQueenEligible(user.getStudentCode(), user.getEmail());
        StudentStreak streak = streakService.find(userId).orElse(null);
        String equipped = streak == null
                ? MascotCatalog.STARTER_ID
                : resolveEquipped(streak.getEquippedMascot(), iceQueenEligible);
        int points = streak == null ? 0 : streak.getPoints();
        int crystals = streak == null ? 0 : streak.getCrystals();
        int gachaTickets = streak == null ? 0 : streak.getGachaTickets();
        int gachaPityCount = streak == null ? 0 : streak.getGachaPityCount();
        int eggSmashPity = streak == null ? 0 : streak.getEggSmashPity();
        boolean inCurrentTerm = queryInCurrentTerm(userId);

        Map<String, AdminUserMascotRow.OwnedPetView> byMascot = new LinkedHashMap<>();
        List<StudentMascotPurchase> stored = purchases.findByIdUserId(userId);
        if (stored != null) {
            for (StudentMascotPurchase rp : stored) {
                if (rp == null || rp.getId() == null) {
                    continue;
                }
                String canonical = MascotCatalog.canonicalId(rp.getId().getMascotId());
                if (canonical == null
                        || MascotCatalog.STARTER_ID.equals(canonical)
                        || !MascotCatalog.isSelectable(canonical)) {
                    continue;
                }
                if (MascotCatalog.EXCLUSIVE_ICE_QUEEN_ID.equals(canonical) && !iceQueenEligible) {
                    continue;
                }
                if (byMascot.containsKey(canonical)) {
                    continue;
                }
                MascotCatalog.Offer offer = MascotCatalog.forSale(canonical);
                int catalogPrice = offer == null ? 0 : offer.price();
                byMascot.put(canonical, new AdminUserMascotRow.OwnedPetView(
                        canonical,
                        MascotCatalog.displayName(canonical),
                        catalogPrice,
                        rp.getPricePaid(),
                        TimeUtil.formatDisplayTimestamp(rp.getPurchasedAt()),
                        canonical.equals(equipped)));
            }
        }

        if (!MascotCatalog.STARTER_ID.equals(equipped) && !byMascot.containsKey(equipped)) {
            MascotCatalog.Offer offer = MascotCatalog.forSale(equipped);
            int catalogPrice = offer == null ? 0 : offer.price();
            byMascot.put(equipped, new AdminUserMascotRow.OwnedPetView(
                    equipped,
                    MascotCatalog.displayName(equipped),
                    catalogPrice,
                    0,
                    null,
                    true));
        }

        List<AdminUserMascotRow.OwnedPetView> ownedList = new ArrayList<>(byMascot.values());
        ownedList.sort(Comparator.comparing(AdminUserMascotRow.OwnedPetView::displayName, String.CASE_INSENSITIVE_ORDER));

        return new AdminUserMascotRow(
                userId,
                user.getStudentCode(),
                user.getTeacherCode(),
                user.getFullName(),
                user.getEmail(),
                user.getIsActive(),
                inCurrentTerm,
                iceQueenEligible,
                equipped,
                points,
                crystals,
                gachaTickets,
                gachaPityCount,
                eggSmashPity,
                ownedList);
    }

    private boolean queryInCurrentTerm(UUID userId) {
        if (userId == null) {
            return false;
        }
        Boolean enrolled = jdbcTemplate.query("""
                SELECT EXISTS (
                    SELECT 1
                    FROM term_enrollment te
                    JOIN term t ON t.id = te.term_id AND t.is_current = TRUE
                    WHERE te.user_id = ?
                )
                """, rs -> rs.next() && rs.getBoolean(1), userId);
        return Boolean.TRUE.equals(enrolled);
    }
}
