package com.vmataj.knowledge;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlannerOrderTest {
    @Test
    void rejectsAnIncompleteOrderWithoutMovingTasks() {
        var tasks = mock(PlannerTaskRepository.class);
        var controller = new PlannerController(tasks, mock(FocusSessionRepository.class));
        var task = new PlannerTask("alice", 10);
        task.id = 1L;
        when(tasks.findByOwnerOrderByPositionAscIdAsc("alice")).thenReturn(List.of(task));
        assertThrows(ResponseStatusException.class, () -> controller.reorder(List.of(2L), () -> "alice"));
        assertEquals(10, task.position);
    }
}
