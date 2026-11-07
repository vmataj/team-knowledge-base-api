package com.vmataj.knowledge;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace", "username"}))
class WorkspaceMembership {
    enum Role { OWNER, EDITOR, VIEWER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 60)
    String workspace;
    @Column(nullable = false)
    String username;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    Role role;

    protected WorkspaceMembership() {}

    WorkspaceMembership(String workspace, String username, Role role) {
        this.workspace = workspace;
        this.username = username;
        this.role = role;
    }
}
