package com.vmataj.knowledge;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class DemoInitialization {
    @Id
    String fixture;

    protected DemoInitialization() {}

    DemoInitialization(String fixture) {
        this.fixture = fixture;
    }
}
