package com.vmataj.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface RunbookRepository extends JpaRepository<Runbook, Long> {
    List<Runbook> findByWorkspaceOrderById(String workspace);
    Optional<Runbook> findByIdAndWorkspace(long id, String workspace);
}
