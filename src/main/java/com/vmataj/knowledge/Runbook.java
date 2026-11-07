package com.vmataj.knowledge;

import jakarta.persistence.*;

@Entity
class Runbook {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String workspace;
    @Column(nullable = false)
    String title;
    @Column(nullable = false, length = 4000)
    String body;

    protected Runbook() {}

    Runbook(String workspace, String title, String body) {
        this.workspace = workspace;
        this.title = title;
        this.body = body;
    }
}
