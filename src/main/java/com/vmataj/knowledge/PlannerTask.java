package com.vmataj.knowledge;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
class PlannerTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String owner;
    @Column(nullable = false, length = 120)
    String title;
    @Column(nullable = false, length = 40)
    String category;
    LocalDate dueDate;
    Instant completedAt;
    long position;

    protected PlannerTask() {}

    PlannerTask(String owner, long position) {
        this.owner = owner;
        this.position = position;
    }
}
