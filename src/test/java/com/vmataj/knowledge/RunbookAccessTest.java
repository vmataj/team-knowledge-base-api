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
        long created = repository.findByWorkspaceOrderById("support").getLast().id;
        mvc.perform(put(PATH + "/" + created).header("Authorization", credentials("editor"))
                .contentType("application/json").content("""
                    {"title":"Updated recovery","body":"Retry after checking dependencies."}
                    """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated recovery"));
        mvc.perform(delete(PATH + "/" + created).header("Authorization", credentials("viewer")))
            .andExpect(status().isForbidden());
        mvc.perform(delete(PATH + "/" + created).header("Authorization", credentials("editor")))
            .andExpect(status().isNoContent());
        mvc.perform(get(PATH + "/" + created).header("Authorization", credentials("editor")))
            .andExpect(status().isNotFound());
    }

    private String credentials(String username) {
        return "Basic " + Base64.getEncoder().encodeToString(
            (username + ":demo-pass").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void searchMatchesTitlesAndInstructionsOnlyInsideTheSelectedWorkspace() throws Exception {
        repository.save(new Runbook("support", "Release checklist", "Verify permissions."));
        repository.save(new Runbook("support", "Smoke testing", "Verify the release version."));
        repository.save(new Runbook("billing", "Release reconciliation", "Check the ledger."));

        mvc.perform(get(PATH).param("q", " ReLeAsE ").header("Authorization", credentials("viewer")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.containsInAnyOrder(
                "Release checklist", "Smoke testing")));
        mvc.perform(get(PATH).param("q", "release").header("Authorization", credentials("outsider")))
            .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanCreateWorkspaceGrantAndRevokeAccess() throws Exception {
        String workspace = "/api/workspaces/platform";
        mvc.perform(post("/api/workspaces").header("Authorization", credentials("editor"))
                .contentType("application/json").content("""
                    {"slug":"platform"}
                    """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("OWNER"));
        mvc.perform(put(workspace + "/members/viewer").header("Authorization", credentials("editor"))
                .contentType("application/json").content("""
                    {"role":"VIEWER"}
                    """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("VIEWER"));
        mvc.perform(get(workspace + "/runbooks").header("Authorization", credentials("viewer")))
            .andExpect(status().isOk());
        mvc.perform(post(workspace + "/runbooks").header("Authorization", credentials("viewer"))
                .contentType("application/json").content(PAYLOAD))
            .andExpect(status().isForbidden());
        mvc.perform(put(workspace + "/members/outsider").header("Authorization", credentials("viewer"))
                .contentType("application/json").content("""
                    {"role":"EDITOR"}
                    """))
            .andExpect(status().isForbidden());
        mvc.perform(delete(workspace + "/members/viewer").header("Authorization", credentials("editor")))
            .andExpect(status().isNoContent());
        mvc.perform(get(workspace + "/runbooks").header("Authorization", credentials("viewer")))
            .andExpect(status().isForbidden());
    }
}
