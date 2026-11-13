package com.vmataj.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/planner")
class PlannerController {
    private final PlannerTaskRepository tasks;
    private final FocusSessionRepository sessions;

    PlannerController(PlannerTaskRepository tasks, FocusSessionRepository sessions) {
        this.tasks = tasks;
        this.sessions = sessions;
    }

    @GetMapping
    Board board(Principal user) {
        String owner = user.getName();
        return new Board(tasks.findByOwnerOrderByPositionAscIdAsc(owner).stream().map(Task::from).toList(),
            sessions.findByOwnerAndFinishedAtAfterOrderByFinishedAt(owner, Instant.now().minus(31, ChronoUnit.DAYS))
                .stream().map(session -> new Focus(session.minutes, session.finishedAt)).toList());
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    Task add(@RequestBody @Valid TaskInput input, Principal user) {
        var task = new PlannerTask(user.getName(), System.currentTimeMillis());
        task.title = input.title().strip();
        task.category = input.category().strip();
        task.dueDate = input.dueDate();
        return Task.from(tasks.save(task));
    }

    @PutMapping("/tasks/{id}")
    @Transactional
    Task edit(@PathVariable long id, @RequestBody @Valid TaskInput input, Principal user) {
        var task = find(id, user);
        task.title = input.title().strip();
        task.category = input.category().strip();
        task.dueDate = input.dueDate();
        return Task.from(task);
    }

    @PutMapping("/tasks/{id}/completion")
    @Transactional
    Task complete(@PathVariable long id, @RequestBody @Valid Completion input, Principal user) {
        var task = find(id, user);
        if (!input.done()) task.completedAt = null;
        else if (task.completedAt == null) task.completedAt = Instant.now();
        return Task.from(task);
    }

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void reorder(@RequestBody List<Long> ids, Principal user) {
        var ordered = tasks.findByOwnerOrderByPositionAscIdAsc(user.getName());
        var byId = ordered.stream().collect(Collectors.toMap(task -> task.id, Function.identity()));
        if (ids.size() != byId.size() || !new HashSet<>(ids).equals(byId.keySet())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reload the planner before reordering.");
        }
        for (int i = 0; i < ids.size(); i++) byId.get(ids.get(i)).position = i;
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id, Principal user) {
        tasks.delete(find(id, user));
    }

    @PutMapping("/focus/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logFocus(@PathVariable UUID id, @RequestBody @Valid FocusInput input, Principal user) {
        String key = user.getName() + ":" + id;
        if (!sessions.existsById(key)) sessions.save(new FocusSession(key, user.getName(), input.minutes()));
    }

    private PlannerTask find(long id, Principal user) {
        return tasks.findByIdAndOwner(id, user.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    record TaskInput(@NotBlank @Size(max = 120) String title,
                     @NotBlank @Size(max = 40) String category, LocalDate dueDate) {}
    record Completion(@NotNull Boolean done) {}
    record FocusInput(@Min(1) @Max(120) int minutes) {}
    record Focus(int minutes, Instant finishedAt) {}
    record Board(List<Task> tasks, List<Focus> sessions) {}
    record Task(long id, String title, String category, LocalDate dueDate, Instant completedAt) {
        static Task from(PlannerTask task) {
            return new Task(task.id, task.title, task.category, task.dueDate, task.completedAt);
        }
    }
}
