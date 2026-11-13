package com.vmataj.knowledge;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface FocusSessionRepository extends JpaRepository<FocusSession, String> {
    List<FocusSession> findByOwnerAndFinishedAtAfterOrderByFinishedAt(String owner, Instant cutoff);
}
