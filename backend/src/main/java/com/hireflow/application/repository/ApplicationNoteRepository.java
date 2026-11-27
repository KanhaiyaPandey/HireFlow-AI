package com.hireflow.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hireflow.application.dto.NoteResponse;
import com.hireflow.application.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, UUID> {

    String SELECT_RESPONSE = """
            select new com.hireflow.application.dto.NoteResponse(n.id, n.authorId, u.name, n.body, n.createdAt)
            from ApplicationNote n
            left join User u on u.id = n.authorId
            """;

    /** Newest first. The caller has already checked the application belongs to its company. */
    @Query(SELECT_RESPONSE + " where n.application.id = :applicationId order by n.createdAt desc, n.id")
    List<NoteResponse> findForApplication(UUID applicationId);

    @Query(SELECT_RESPONSE + " where n.id = :id")
    Optional<NoteResponse> findResponse(UUID id);
}
