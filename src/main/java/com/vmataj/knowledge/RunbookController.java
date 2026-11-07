package com.vmataj.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workspaces/{workspace}/runbooks")
class RunbookController {
    private final RunbookRepository repository;
    private final MembershipRepository memberships;
    private final WorkspaceRepository workspaces;

    RunbookController(RunbookRepository repository, MembershipRepository memberships, WorkspaceRepository workspaces) {
        this.repository = repository;
        this.memberships = memberships;
        this.workspaces = workspaces;
    }

    @GetMapping
    List<RunbookView> list(@PathVariable String workspace, Authentication user) {
        authorize(workspace, user, false);
        return repository.findByWorkspaceOrderById(workspace).stream().map(RunbookView::from).toList();
    }

    @GetMapping("/{id}")
    RunbookView read(@PathVariable String workspace, @PathVariable long id, Authentication user) {
        authorize(workspace, user, false);
        return RunbookView.from(find(workspace, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RunbookView create(@PathVariable String workspace, @Valid @RequestBody RunbookInput input,
                       Authentication user) {
        authorize(workspace, user, true);
        return RunbookView.from(repository.save(new Runbook(workspace, input.title(), input.body())));
    }

    @PutMapping("/{id}")
    @Transactional
    RunbookView update(@PathVariable String workspace, @PathVariable long id,
                       @Valid @RequestBody RunbookInput input, Authentication user) {
        authorize(workspace, user, true);
        Runbook runbook = find(workspace, id);
        runbook.title = input.title();
        runbook.body = input.body();
        return RunbookView.from(runbook);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void delete(@PathVariable String workspace, @PathVariable long id, Authentication user) {
        authorize(workspace, user, true);
        repository.delete(find(workspace, id));
    }

    private Runbook find(String workspace, long id) {
        return repository.findByIdAndWorkspace(id, workspace)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void authorize(String workspace, Authentication user, boolean write) {
        if (!workspaces.existsById(workspace)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var membership = memberships.findByWorkspaceAndUsername(workspace, user.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (write && membership.role == WorkspaceMembership.Role.VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    record RunbookInput(@NotBlank @Size(max = 255) String title,
                        @NotBlank @Size(max = 4000) String body) {}

    record RunbookView(long id, String title, String body) {
        static RunbookView from(Runbook runbook) {
            return new RunbookView(runbook.id, runbook.title, runbook.body);
        }
    }
}
