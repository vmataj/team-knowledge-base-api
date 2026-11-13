package com.vmataj.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunbookAuthorizationTest {
    @Mock RunbookRepository articles;
    @Mock MembershipRepository memberships;
    @Mock WorkspaceRepository workspaces;

    @Test
    void aNonmemberCannotSearchWorkspaceArticles() {
        when(workspaces.existsById("qa")).thenReturn(true);
        var controller = new RunbookController(articles, memberships, workspaces);

        var error = assertThrows(ResponseStatusException.class,
            () -> controller.list("qa", "release", new TestingAuthenticationToken("outsider", "")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(articles);
    }
}
