package com.vmataj.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RunbookAccessTest {
    private static final String PATH = "/api/workspaces/support/runbooks";
    private static final String PAYLOAD = """
        {"title":"Service recovery","body":"Inspect failed jobs before retrying."}
        """;

    @Autowired MockMvc mvc;
    @Autowired RunbookRepository repository;

    @Test
    void workspaceMembershipControlsReadAndWrite() throws Exception {
        long id = repository.findByWorkspaceOrderById("support").getFirst().id;
        mvc.perform(get(PATH + "/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH + "/" + id).header("Authorization", credentials("viewer")))
            .andExpect(status().isOk());
        mvc.perform(put(PATH + "/" + id).header("Authorization", credentials("viewer"))
                .contentType("application/json").content(PAYLOAD))
            .andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/" + id).header("Authorization", credentials("outsider")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/workspaces/billing/runbooks/" + id)
                .header("Authorization", credentials("outsider")))
            .andExpect(status().isNotFound());
        mvc.perform(post(PATH).header("Authorization", credentials("editor"))
                .contentType("application/json").content(PAYLOAD))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Service recovery"));
    }

    private String credentials(String username) {
        return "Basic " + Base64.getEncoder().encodeToString(
            (username + ":demo-pass").getBytes(StandardCharsets.UTF_8));
    }
}
