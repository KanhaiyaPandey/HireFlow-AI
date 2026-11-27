package com.hireflow.application.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hireflow.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only application lookups for the recruitment module. Depends on nothing outside this module. */
@Service
@RequiredArgsConstructor
public class ApplicationQueryService {

    private final ApplicationRepository applicationRepository;

    /** Application count per job, in one query. Jobs without applications are absent from the map. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> countByJob(Collection<UUID> jobIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (jobIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : applicationRepository.countByJobIds(jobIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }
}
