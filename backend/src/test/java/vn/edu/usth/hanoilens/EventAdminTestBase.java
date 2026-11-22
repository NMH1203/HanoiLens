package vn.edu.usth.hanoilens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Shared helpers for the administrative event tests (ingestion T28, moderation T29): USER and ADMIN tokens. */
abstract class EventAdminTestBase extends IntegrationTestBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ObjectMapper json;

    protected String userToken() throws Exception {
        return login(register());
    }

    /** Registers a normal user, promotes it directly in the database (the API can never do this), logs in. */
    protected String adminToken() throws Exception {
        String email = register();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        return login(email);
    }

    protected UUID userIdOf(String token) throws Exception {
        String sub = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        return UUID.fromString(json.readTree(sub).get("sub").asText());
    }

    private String register() throws Exception {
        String email = "ev-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/v1/auth/register").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Tester\",\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/v1/auth/login").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).at("/data/accessToken").asText();
    }
}
