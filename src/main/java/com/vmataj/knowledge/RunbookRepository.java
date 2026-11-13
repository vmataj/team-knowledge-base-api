package com.vmataj.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface RunbookRepository extends JpaRepository<Runbook, Long> {
    List<Runbook> findByWorkspaceOrderById(String workspace);
    @Query("""
        select r from Runbook r
        where r.workspace = :workspace
          and (locate(lower(:term), lower(r.title)) > 0 or locate(lower(:term), lower(r.body)) > 0)
        order by r.id
        """)
    List<Runbook> search(String workspace, String term);

    Optional<Runbook> findByIdAndWorkspace(long id, String workspace);
}
