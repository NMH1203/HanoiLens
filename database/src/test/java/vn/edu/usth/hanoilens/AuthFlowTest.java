package vn.edu.usth.hanoilens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class AuthFlowTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private String email() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private void register(String email, String password) throws Exception {
        mvc.perform(post("/v1/auth/register").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Minh\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mvc.perform(post("/v1/auth/login").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("data");
    }

    @Test
    void registerLoginAndReadProfile() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        JsonNode tokens = login(email, "correct-horse-battery");

        mvc.perform(get("/v1/me").header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.defaultLocationPrecision").value("DESTINATION"));
    }

    @Test
    void registrationCannotChooseRole() throws Exception {
        String email = email();
        mvc.perform(post("/v1/auth/register").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Eve\",\"email\":\"" + email
                                + "\",\"password\":\"correct-horse-battery\",\"role\":\"ADMIN\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void duplicateEmailIsCaseInsensitive() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        mvc.perform(post("/v1/auth/register").with(freshClient()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Dup\",\"email\":\"" + email.toUpperCase()
                                + "\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void loginFailuresAreIndistinguishable() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        String wrongPassword = mvc.perform(post("/v1/auth/login").with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password-1\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = mvc.perform(post("/v1/auth/login").with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email() + "\",\"password\":\"wrong-password-1\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json.readTree(wrongPassword).at("/error/message"))
                .isEqualTo(json.readTree(unknownEmail).at("/error/message"));
    }

    @Test
    void refreshRotatesAndReuseRevokesTheFamily() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        String r1 = login(email, "correct-horse-battery").get("refreshToken").asText();

        String body = mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String r2 = json.readTree(body).at("/data/refreshToken").asText();

        // Replaying the rotated token is theft: 401 and the whole family dies, including the fresh token.
        mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + r2 + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokensArePersistedOnlyAsHashes() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        String raw = login(email, "correct-horse-battery").get("refreshToken").asText();
        Integer exposed = jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class, raw);
        org.assertj.core.api.Assertions.assertThat(exposed).isZero();
    }

    @Test
    void patchMeIgnoresRoleAndUpdatesAllowedFields() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        String token = login(email, "correct-horse-battery").get("accessToken").asText();
        mvc.perform(patch("/v1/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Hoang\",\"defaultLocationPrecision\":\"DISTRICT\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Hoang"))
                .andExpect(jsonPath("$.data.defaultLocationPrecision").value("DISTRICT"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void protectedEndpointsRejectMissingAndForgedTokens() throws Exception {
        mvc.perform(get("/v1/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/v1/me").header("Authorization", "Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiJ4In0."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCallAdminRoutes() throws Exception {
        String email = email();
        register(email, "correct-horse-battery");
        String token = login(email, "correct-horse-battery").get("accessToken").asText();
        mvc.perform(get("/v1/admin/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
