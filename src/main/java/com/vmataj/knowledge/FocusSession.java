package com.vmataj.knowledge;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
class FocusSession {
    @Id
    String id;
    @Column(nullable = false)
    String owner;
    int minutes;
    Instant finishedAt;

    protected FocusSession() {}

    FocusSession(String id, String owner, int minutes) {
        this.id = id;
        this.owner = owner;
        this.minutes = minutes;
        this.finishedAt = Instant.now();
    }
}
