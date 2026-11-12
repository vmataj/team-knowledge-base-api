package com.vmataj.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:private-accounts;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("private")
class PrivateAccountTest {
    private static final String PASSWORD = "a-local-test-password";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void registeredUsersCanOnlyReadWorkspacesTheyBelongTo() throws Exception {
        MockHttpSession owner = signupAndLogin("alice");
        MockHttpSession outsider = signupAndLogin("bob");
        var ownerToken = token(owner);

        mvc.perform(post("/api/workspaces").session(owner)
                .contentType("application/json").content("{\"slug\":\"private-operations\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/workspaces").session(owner)
                .header(ownerToken.csrfHeader(), ownerToken.csrfToken())
                .contentType("application/json").content("{\"slug\":\"private-operations\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/workspaces").session(outsider))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/workspaces/private-operations/runbooks").session(outsider))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/workspaces/private-operations/members/bob").session(owner)
                .header(ownerToken.csrfHeader(), ownerToken.csrfToken())
                .contentType("application/json").content("{\"role\":\"VIEWER\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/workspaces/private-operations/runbooks").session(outsider))
            .andExpect(status().isOk());
    }

    private MockHttpSession signupAndLogin(String username) throws Exception {
        MockHttpSession session = new MockHttpSession();
        var token = token(session);
        mvc.perform(post("/account/signup").session(session)
                .header(token.csrfHeader(), token.csrfToken()).contentType("application/json")
                .content(json.writeValueAsString(new AccountController.Signup(username, PASSWORD))))
            .andExpect(status().isCreated());
        return (MockHttpSession) mvc.perform(post("/login").session(session)
                .param("_csrf", token.csrfToken()).param("username", username).param("password", PASSWORD))
            .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private AccountController.Session token(MockHttpSession session) throws Exception {
        byte[] body = mvc.perform(get("/account").session(session))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return json.readValue(body, AccountController.Session.class);
    }
}
