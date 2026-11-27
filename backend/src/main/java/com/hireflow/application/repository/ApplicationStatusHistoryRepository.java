package com.hireflow.application.repository;

import java.util.List;
import java.util.UUID;

import com.hireflow.application.dto.StatusHistoryResponse;
import com.hireflow.application.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    /** Oldest first. The caller has already checked the application belongs to its company. */
    @Query("""
            select new com.hireflow.application.dto.StatusHistoryResponse(
                h.id, h.oldStatus, h.newStatus, h.changedBy, u.name, h.reason, h.createdAt)
            from ApplicationStatusHistory h
            left join User u on u.id = h.changedBy
            where h.application.id = :applicationId
            order by h.createdAt, h.id
            """)
    List<StatusHistoryResponse> findTimeline(UUID applicationId);
}
