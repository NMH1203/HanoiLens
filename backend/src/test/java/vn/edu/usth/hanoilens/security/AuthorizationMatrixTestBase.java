package vn.edu.usth.hanoilens.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import vn.edu.usth.hanoilens.IntegrationTestBase;

/**
 * Reusable authorization matrix template (Task T13, SECURITY.md §4) for private and administrative endpoints.
 *
 * <p>Covers the 5 required actors/vectors:
 * <ul>
 *   <li>{@link Actor#OWNER} — legitimate resource owner ({@code 200/201/202/204})</li>
 *   <li>{@link Actor#OTHER_USER} — horizontal BOLA/IDOR attempt ({@code 404}) or role boundary ({@code 403})</li>
 *   <li>{@link Actor#ANONYMOUS} — unauthenticated caller ({@code 401})</li>
 *   <li>{@link Actor#ADMIN} — administrator role boundary & zero-trust private scoping</li>
 *   <li>{@link Actor#PARENT_ID_TAMPERING} — path/nested parent ID tampering ({@code 404/422})</li>
 * </ul>
 * Also enforces standard {@code ErrorResponse} envelopes on {@code 4xx/5xx} responses and verifies
 * response DTO safety (never leaking password/token hashes or raw internal storage keys).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AuthorizationMatrixTestBase extends IntegrationTestBase {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private static final Set<String> FORBIDDEN_DTO_FIELDS = Set.of(
            "passwordHash",
            "password_hash",
            "tokenHash",
            "token_hash",
            "objectKey",
            "object_key",
            "original_asset_url",
            "exported_asset_url"
    );

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    public enum Actor {
        OWNER,
        OTHER_USER,
        ANONYMOUS,
        ADMIN,
        PARENT_ID_TAMPERING
    }

    public record TestActorContext(
            TestUser owner,
            TestUser otherUser,
            TestUser admin
    ) {
        public String tokenFor(Actor actor) {
            return switch (actor) {
                case OWNER, PARENT_ID_TAMPERING -> owner.accessToken();
                case OTHER_USER -> otherUser.accessToken();
                case ADMIN -> admin.accessToken();
                case ANONYMOUS -> null;
            };
        }
    }

    public record TestUser(UUID id, String email, String accessToken) {}

    public record AuthzCase(
            String name,
            HttpMethod method,
            String urlTemplate,
            String jsonBody,
            Actor actor,
            int expectedStatus,
            Map<String, String> headers
    ) {
        public AuthzCase(
                String name,
                HttpMethod method,
                String urlTemplate,
                String jsonBody,
                Actor actor,
                int expectedStatus
        ) {
            this(name, method, urlTemplate, jsonBody, actor, expectedStatus, Map.of());
        }

        public AuthzCase {
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }

        @Override
        public String toString() {
            return name + " [" + method + " " + urlTemplate + " as " + actor + " -> " + expectedStatus + "]";
        }
    }

    /**
     * Subclasses provision domain fixtures (e.g., a Visit, Photo, or Recap belonging to {@code ctx.owner()})
     * and return the matrix of authorization cases to verify.
     */
    protected abstract List<AuthzCase> authorizationCases(TestActorContext ctx) throws Exception;

    /**
     * Convenience helper generating the standard 4-actor private resource matrix
     * ({@code OWNER -> ownerExpectedStatus}, {@code OTHER_USER -> 404}, {@code ADMIN -> 404}, {@code ANONYMOUS -> 401})
     * for user-scoped private endpoints.
     */
    protected static List<AuthzCase> privateResourceMatrix(
            String label,
            HttpMethod method,
            String ownerResourceUrl,
            String jsonBody,
            int ownerExpectedStatus
    ) {
        return List.of(
                new AuthzCase(label + ": owner allowed", method, ownerResourceUrl, jsonBody, Actor.OWNER, ownerExpectedStatus),
                new AuthzCase(label + ": other user denied with 404 (BOLA)", method, ownerResourceUrl, jsonBody, Actor.OTHER_USER, 404),
                new AuthzCase(label + ": admin denied on user-private resource with 404", method, ownerResourceUrl, jsonBody, Actor.ADMIN, 404),
                new AuthzCase(label + ": anonymous rejected with 401", method, ownerResourceUrl, jsonBody, Actor.ANONYMOUS, 401)
        );
    }

    /**
     * Convenience helper generating the role-boundary matrix for {@code /v1/admin/**} endpoints
     * ({@code ANONYMOUS -> 401}, {@code OTHER_USER -> 403}, {@code OWNER -> 403}, {@code ADMIN -> adminExpectedStatus}).
     */
    protected static List<AuthzCase> adminRouteMatrix(
            String label,
            HttpMethod method,
            String adminUrl,
            String jsonBody,
            int adminExpectedStatus
    ) {
        return List.of(
                new AuthzCase(label + ": anonymous rejected with 401", method, adminUrl, jsonBody, Actor.ANONYMOUS, 401),
                new AuthzCase(label + ": regular user (OTHER_USER) forbidden with 403", method, adminUrl, jsonBody, Actor.OTHER_USER, 403),
                new AuthzCase(label + ": regular user (OWNER) forbidden with 403", method, adminUrl, jsonBody, Actor.OWNER, 403),
                new AuthzCase(label + ": admin passes role boundary", method, adminUrl, jsonBody, Actor.ADMIN, adminExpectedStatus)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideMatrixCases")
    void enforceAuthorizationMatrix(AuthzCase testCase) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .request(testCase.method(), testCase.urlTemplate())
                .with(freshClient());

        String token = currentContext.tokenFor(testCase.actor());
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        if (isMutatingMethod(testCase.method()) && !testCase.headers().containsKey(IDEMPOTENCY_HEADER)) {
            request.header(IDEMPOTENCY_HEADER, "authz-" + UUID.randomUUID());
        }
        testCase.headers().forEach(request::header);

        if (testCase.jsonBody() != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(testCase.jsonBody());
        }

        ResultActions result = mvc.perform(request).andExpect(status().is(testCase.expectedStatus()));

        String responseBody = result.andReturn().getResponse().getContentAsString();
        if (testCase.expectedStatus() >= 400 && !responseBody.isBlank()) {
            result.andExpect(jsonPath("$.error.code").isNotEmpty())
                    .andExpect(jsonPath("$.error.message").isNotEmpty());
        }

        if (!responseBody.isBlank()) {
            assertDtoSafety(json.readTree(responseBody));
        }
    }

    private static boolean isMutatingMethod(HttpMethod method) {
        return HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.PATCH.equals(method)
                || HttpMethod.DELETE.equals(method);
    }

    private static void assertDtoSafety(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(field -> {
                assertThat(FORBIDDEN_DTO_FIELDS)
                        .as("Response DTO must never leak internal field '%s' (SECURITY.md §4)", field)
                        .doesNotContain(field);
                assertDtoSafety(node.get(field));
            });
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                assertDtoSafety(item);
            }
        }
    }

    private TestActorContext currentContext;

    protected Stream<AuthzCase> provideMatrixCases() throws Exception {
        if (currentContext == null) {
            currentContext = provisionActors();
        }
        return authorizationCases(currentContext).stream();
    }

    private TestActorContext provisionActors() throws Exception {
        TestUser owner = createUserWithRole("USER");
        TestUser otherUser = createUserWithRole("USER");
        TestUser admin = createUserWithRole("ADMIN");
        return new TestActorContext(owner, otherUser, admin);
    }

    protected TestUser createUserWithRole(String role) throws Exception {
        String email = "authz-" + role.toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String password = "correct-horse-battery";

        String regBody = mvc.perform(post("/v1/auth/register")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Test " + role + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID userId = UUID.fromString(json.readTree(regBody).at("/data/id").asText());

        if (!"USER".equals(role)) {
            jdbc.update("UPDATE users SET role = ? WHERE id = ?", role, userId);
        }

        String loginBody = mvc.perform(post("/v1/auth/login")
                        .with(freshClient())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode data = json.readTree(loginBody).get("data");
        return new TestUser(userId, email, data.get("accessToken").asText());
    }
}
