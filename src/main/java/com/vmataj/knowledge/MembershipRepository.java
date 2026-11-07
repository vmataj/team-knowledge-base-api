package com.vmataj.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface MembershipRepository extends JpaRepository<WorkspaceMembership, Long> {
    Optional<WorkspaceMembership> findByWorkspaceAndUsername(String workspace, String username);
    List<WorkspaceMembership> findByUsernameOrderByWorkspace(String username);
    List<WorkspaceMembership> findByWorkspaceOrderByUsername(String workspace);
}
