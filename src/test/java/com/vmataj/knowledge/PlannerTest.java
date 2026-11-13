package com.vmataj.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PlannerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void accountsOwnTheirTasksAndFocusHistory() throws Exception {
        mvc.perform(get("/api/planner")).andExpect(status().isUnauthorized());
        var alice = login("alice");
        var bob = login("bob");
        var token = token(alice);
        var task = mvc.perform(post("/api/planner/tasks").session(alice)
                .header(token.csrfHeader(), token.csrfToken()).contentType("application/json")
                .content("{\"title\":\"Read the notes\",\"category\":\"Study\"}"))
            .andExpect(status().isCreated()).andReturn();
        long id = json.readTree(task.getResponse().getContentAsString()).get("id").asLong();
        var other = token(bob);
        mvc.perform(delete("/api/planner/tasks/{id}", id).session(bob)
                .header(other.csrfHeader(), other.csrfToken())).andExpect(status().isNotFound());
        mvc.perform(put("/api/planner/order").session(bob).header(other.csrfHeader(), other.csrfToken())
                .contentType("application/json").content("[" + id + "]")).andExpect(status().isConflict());
        mvc.perform(put("/api/planner/tasks/{id}/completion", id).session(alice)
                .header(token.csrfHeader(), token.csrfToken()).contentType("application/json")
                .content("{\"done\":true}")).andExpect(jsonPath("$.completedAt").isNotEmpty());
        String focus = "/api/planner/focus/0c3afb32-87b9-4a9a-9ecb-00928bbb378a";
        for (int i = 0; i < 2; i++) mvc.perform(put(focus).session(alice)
                .header(token.csrfHeader(), token.csrfToken()).contentType("application/json")
                .content("{\"minutes\":25}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/planner").session(alice)).andExpect(jsonPath("$.sessions.length()").value(1));
        mvc.perform(get("/api/planner").session(bob)).andExpect(jsonPath("$.tasks").isEmpty())
            .andExpect(jsonPath("$.sessions").isEmpty());
    }

    private MockHttpSession login(String username) throws Exception {
        var session = new MockHttpSession();
        var token = token(session);
        mvc.perform(post("/account/signup").session(session).header(token.csrfHeader(), token.csrfToken())
                .contentType("application/json").content(json.writeValueAsString(
                    new AccountController.Signup(username, "test-only-password")))).andExpect(status().isCreated());
        return (MockHttpSession) mvc.perform(post("/login").session(session).param("_csrf", token.csrfToken())
                .param("username", username).param("password", "test-only-password"))
            .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private AccountController.Session token(MockHttpSession session) throws Exception {
        return json.readValue(mvc.perform(get("/account").session(session)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(), AccountController.Session.class);
    }
}
