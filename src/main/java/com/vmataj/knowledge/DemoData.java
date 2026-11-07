package com.vmataj.knowledge;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DemoData {
    @Bean
    ApplicationRunner seedRunbook(RunbookRepository repository) {
        return args -> {
            if (repository.findByWorkspaceOrderById("support").isEmpty()) {
                repository.save(new Runbook("support", "Restore a delayed notification queue",
                    "Confirm the backlog in the monitoring dashboard. Pause new retries, inspect the oldest failed event, and resume processing after the downstream service recovers."));
            }
        };
    }
}
