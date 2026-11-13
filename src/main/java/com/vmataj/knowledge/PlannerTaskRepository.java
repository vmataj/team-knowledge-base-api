package com.vmataj.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlannerTaskRepository extends JpaRepository<PlannerTask, Long> {
    List<PlannerTask> findByOwnerOrderByPositionAscIdAsc(String owner);
    Optional<PlannerTask> findByIdAndOwner(long id, String owner);
}
