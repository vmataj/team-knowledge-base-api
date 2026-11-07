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

    RunbookController(RunbookRepository repository) {
        this.repository = repository;
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

    private Runbook find(String workspace, long id) {
        return repository.findByIdAndWorkspace(id, workspace)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void authorize(String workspace, Authentication user, boolean write) {
        String prefix = switch (workspace) {
            case "support" -> "SUPPORT";
            case "billing" -> "BILLING";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
        boolean permitted = user.getAuthorities().stream().anyMatch(authority ->
            authority.getAuthority().equals(prefix + "_EDIT")
                || (!write && authority.getAuthority().equals(prefix + "_VIEW")));
        if (!permitted) {
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
