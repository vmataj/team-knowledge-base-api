package com.vmataj.knowledge;

import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
class DemoData {
    @Bean
    ApplicationRunner seedRunbook(RunbookRepository repository, WorkspaceRepository workspaces,
                                  MembershipRepository memberships, EntityManager entities,
                                  TransactionTemplate transactions) {
        return args -> transactions.executeWithoutResult(status -> {
            if (entities.find(DemoInitialization.class, "workspaces") == null) {
                workspaces.save(new Workspace("support"));
                workspaces.save(new Workspace("billing"));
                memberships.save(new WorkspaceMembership("support", "editor", WorkspaceMembership.Role.OWNER));
                memberships.save(new WorkspaceMembership("support", "viewer", WorkspaceMembership.Role.VIEWER));
                memberships.save(new WorkspaceMembership("billing", "outsider", WorkspaceMembership.Role.OWNER));
                entities.persist(new DemoInitialization("workspaces"));
            }
            if (entities.find(DemoInitialization.class, "runbooks") != null) {
                return;
            }
            if (repository.findByWorkspaceOrderById("support").isEmpty()) {
                repository.save(new Runbook("support", "Restore a delayed notification queue",
                    "Confirm the backlog in the monitoring dashboard. Pause new retries, inspect the oldest failed event, and resume processing after the downstream service recovers."));
            }
            entities.persist(new DemoInitialization("runbooks"));
        });
    }
}
