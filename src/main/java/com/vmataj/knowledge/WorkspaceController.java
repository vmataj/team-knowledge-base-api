package com.vmataj.knowledge;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.vmataj.knowledge.WorkspaceMembership.Role;

@RestController
@RequestMapping("/api/workspaces")
class WorkspaceController {
    private final WorkspaceRepository workspaces;
    private final MembershipRepository memberships;
    private final UserDetailsManager users;
    private final EntityManager entities;

    WorkspaceController(WorkspaceRepository workspaces, MembershipRepository memberships,
                        UserDetailsManager users, EntityManager entities) {
        this.workspaces = workspaces;
        this.memberships = memberships;
        this.users = users;
        this.entities = entities;
    }

    @GetMapping
    List<Access> list(Authentication actor) {
        return memberships.findByUsernameOrderByWorkspace(actor.getName()).stream()
            .map(member -> new Access(member.workspace, member.role)).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    Access create(@RequestBody @Valid CreateWorkspace input, Authentication actor) {
        if (workspaces.existsById(input.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workspace already exists");
        }
        entities.persist(new Workspace(input.slug()));
        memberships.save(new WorkspaceMembership(input.slug(), actor.getName(), Role.OWNER));
        return new Access(input.slug(), Role.OWNER);
    }

    @GetMapping("/{workspace}/members")
    List<Member> members(@PathVariable String workspace, Authentication actor) {
        requireOwner(workspace, actor);
        return memberships.findByWorkspaceOrderByUsername(workspace).stream()
            .map(member -> new Member(member.username, member.role)).toList();
    }

    @PutMapping("/{workspace}/members/{username}")
    @Transactional
    Member assign(@PathVariable String workspace, @PathVariable String username,
                  @RequestBody @Valid AssignRole input, Authentication actor) {
        requireOwner(workspace, actor);
        if (!users.userExists(username)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown account");
        WorkspaceMembership member = memberships.findByWorkspaceAndUsername(workspace, username)
            .orElseGet(() -> new WorkspaceMembership(workspace, username, input.role()));
        if (member.role == Role.OWNER || input.role() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner role cannot be reassigned");
        }
        member.role = input.role();
        memberships.save(member);
        return new Member(username, member.role);
    }

    @DeleteMapping("/{workspace}/members/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void remove(@PathVariable String workspace, @PathVariable String username, Authentication actor) {
        requireOwner(workspace, actor);
        WorkspaceMembership member = memberships.findByWorkspaceAndUsername(workspace, username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (member.role == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot be removed");
        }
        memberships.delete(member);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    void conflict() {}

    private void requireOwner(String workspace, Authentication actor) {
        boolean owner = memberships.findByWorkspaceAndUsername(workspace, actor.getName())
            .map(member -> member.role == Role.OWNER).orElse(false);
        if (!owner) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    record CreateWorkspace(@NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
                           @Size(max = 60) String slug) {}
    record AssignRole(@NotNull Role role) {}
    record Access(String workspace, Role role) {}
    record Member(String username, Role role) {}
}
