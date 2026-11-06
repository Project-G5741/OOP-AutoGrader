package com.eiu.capstone.backend.desktop;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.repository.MasterDataRepository;

@Component
@Profile("desktop")
public class DesktopMasterDataResolver {

    private static final int DEFAULT_START_ID = 1000;

    private final MasterDataRepository masterDataRepository;
    private final AtomicInteger nextId;
    private final Map<String, Integer> cache = new HashMap<>();

    public DesktopMasterDataResolver(MasterDataRepository masterDataRepository) {
        this.masterDataRepository = masterDataRepository;
        int maxExistingId = DEFAULT_START_ID - 1;
        for (MasterData row : masterDataRepository.findAll()) {
            cache.put(cacheKey(row.getCategory(), row.getName()), row.getId());
            if (row.getId() != null) {
                maxExistingId = Math.max(maxExistingId, row.getId());
            }
        }
        // File H2 retains master_data across restarts; never reuse those PKs (save→merge overwrites).
        this.nextId = new AtomicInteger(maxExistingId + 1);
    }

    public Integer scopeId(String name) {
        return resolve("SCOPE", name);
    }

    public Integer declaringTypeId(String name) {
        return resolve("DECLARING_TYPE", name);
    }

    public Integer relationTypeId(String name) {
        return resolve("RELATION_TYPE", name);
    }

    private Integer resolve(String category, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Missing master data name for " + category);
        }
        String key = cacheKey(category, name);
        Integer hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        MasterData created = new MasterData();
        created.setId(nextId.getAndIncrement());
        created.setCategory(category);
        created.setName(name.trim());
        created = masterDataRepository.save(created);
        cache.put(key, created.getId());
        return created.getId();
    }

    private static String cacheKey(String category, String name) {
        return category.toUpperCase(Locale.ROOT) + '\0' + name.trim().toUpperCase(Locale.ROOT);
    }
}
