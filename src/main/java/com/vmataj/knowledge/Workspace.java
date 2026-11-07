package com.vmataj.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class Workspace {
    @Id @Column(length = 60)
    String slug;

    protected Workspace() {}

    Workspace(String slug) {
        this.slug = slug;
    }
}
