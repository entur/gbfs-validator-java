package org.entur.gbfs.validator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.entur.gbfs.validator.api.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ValidateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private String wiremockBaseUrl;

    private final String gbfsDiscoveryJson = "{\"version\": \"2.3\", \"data\": {\"en\": {\"feeds\": []}}}"; // v2.3 for simplicity, no feed list

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        wiremockBaseUrl = wireMockServer.baseUrl();
        // In a real scenario, you might need to override properties
        // for the application's HTTP client to use wiremockBaseUrl.
        // For this test, we assume the Loader is picking up full URLs.
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testValidate_NoAuth_Success() throws Exception {
        String feedPath = "/gbfs-noauth.json";
        String fullFeedUrl = wiremockBaseUrl + feedPath;

        stubFor(get(urlEqualTo(feedPath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gbfsDiscoveryJson)));

        ValidatePostRequest requestBody = new ValidatePostRequest();
        requestBody.setFeedUrl(fullFeedUrl);
        // No auth object set

        mockMvc.perform(post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.files[0].fileName").value("gbfs-noauth.json"))
                .andExpect(jsonPath("$.summary.files[0].systemErrors").isEmpty());

        verify(getRequestedFor(urlEqualTo(feedPath))
                .withoutHeader("Authorization"));
    }

    @Test
    void testValidate_BasicAuth_Success() throws Exception {
        String feedPath = "/gbfs-basic.json";
        String fullFeedUrl = wiremockBaseUrl + feedPath;
        String username = "testuser";
        String password = "testpassword";
        String expectedAuthHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        stubFor(get(urlEqualTo(feedPath))
                .withHeader("Authorization", equalTo(expectedAuthHeader))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gbfsDiscoveryJson)));

        ValidatePostRequest requestBody = new ValidatePostRequest();
        requestBody.setFeedUrl(fullFeedUrl);

        BasicAuth basicAuth = new BasicAuth();
        basicAuth.setUsername(username);
        basicAuth.setPassword(password);
        basicAuth.setAuthType("BASIC"); // Set discriminator

        ValidatePostRequestAuth requestAuth = new ValidatePostRequestAuth(basicAuth);
        requestBody.setAuth(requestAuth);

        mockMvc.perform(post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.files[0].fileName").value("gbfs-basic.json"))
                .andExpect(jsonPath("$.summary.files[0].systemErrors").isEmpty());

        verify(getRequestedFor(urlEqualTo(feedPath))
                .withHeader("Authorization", equalTo(expectedAuthHeader)));
    }

    @Test
    void testValidate_BearerTokenAuth_Success() throws Exception {
        String feedPath = "/gbfs-bearer.json";
        String fullFeedUrl = wiremockBaseUrl + feedPath;
        String token = "test_bearer_token";
        String expectedAuthHeader = "Bearer " + token;

        stubFor(get(urlEqualTo(feedPath))
                .withHeader("Authorization", equalTo(expectedAuthHeader))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gbfsDiscoveryJson)));

        ValidatePostRequest requestBody = new ValidatePostRequest();
        requestBody.setFeedUrl(fullFeedUrl);

        BearerTokenAuth bearerAuth = new BearerTokenAuth();
        bearerAuth.setToken(token);
        bearerAuth.setAuthType("BEARER_TOKEN"); // Set discriminator

        ValidatePostRequestAuth requestAuth = new ValidatePostRequestAuth(bearerAuth);
        requestBody.setAuth(requestAuth);

        mockMvc.perform(post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.files[0].fileName").value("gbfs-bearer.json"))
                .andExpect(jsonPath("$.summary.files[0].systemErrors").isEmpty());

        verify(getRequestedFor(urlEqualTo(feedPath))
                .withHeader("Authorization", equalTo(expectedAuthHeader)));
    }

    @Test
    void testValidate_OAuthClientCredentials_Success() throws Exception {
        String feedPath = "/gbfs-oauth.json";
        String tokenPath = "/oauth/token";
        String fullFeedUrl = wiremockBaseUrl + feedPath;
        String fullTokenUrl = wiremockBaseUrl + tokenPath;

        String clientId = "test_client_id";
        String clientSecret = "test_client_secret";
        String accessToken = "oauth_integration_test_token";

        // 1. Mock OAuth token endpoint
        stubFor(post(urlEqualTo(tokenPath))
                .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("client_id=" + clientId))
                .withRequestBody(containing("client_secret=" + clientSecret))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\": \"" + accessToken + "\", \"token_type\": \"Bearer\"}")));

        // 2. Mock GBFS feed endpoint
        stubFor(get(urlEqualTo(feedPath))
                .withHeader("Authorization", equalTo("Bearer " + accessToken))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(gbfsDiscoveryJson)));

        ValidatePostRequest requestBody = new ValidatePostRequest();
        requestBody.setFeedUrl(fullFeedUrl);

        OAuthClientCredentialsGrantAuth oauthAuth = new OAuthClientCredentialsGrantAuth();
        oauthAuth.setClientId(clientId);
        oauthAuth.setClientSecret(clientSecret);
        oauthAuth.setTokenUrl(java.net.URI.create(fullTokenUrl));
        oauthAuth.setAuthType("OAUTH_CLIENT_CREDENTIALS"); // Set discriminator

        ValidatePostRequestAuth requestAuth = new ValidatePostRequestAuth(oauthAuth);
        requestBody.setAuth(requestAuth);

        mockMvc.perform(post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.files[0].fileName").value("gbfs-oauth.json"))
                .andExpect(jsonPath("$.summary.files[0].systemErrors").isEmpty());

        verify(postRequestedFor(urlEqualTo(tokenPath)));
        verify(getRequestedFor(urlEqualTo(feedPath))
                .withHeader("Authorization", equalTo("Bearer " + accessToken)));
    }

    @Test
    void testValidate_BasicAuth_Failure() throws Exception {
        String feedPath = "/gbfs-basic-fail.json";
        String fullFeedUrl = wiremockBaseUrl + feedPath;
        String username = "wronguser";
        String password = "wrongpassword";

        // WireMock will return 404 by default if no stub matches.
        // To specifically test 401, we can make it more explicit,
        // or rely on the fact that a request with *any* basic auth header
        // that is not the "correct" one (if we had a success stub) would not match.
        // For this test, we'll assume any request to this path without a *specific matching*
        // auth header (which we don't provide a success case for) will effectively be an auth failure from client's POV.
        // Or, more robustly, stub a 401 for any Basic Auth.
        stubFor(get(urlEqualTo(feedPath))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized by test")));


        ValidatePostRequest requestBody = new ValidatePostRequest();
        requestBody.setFeedUrl(fullFeedUrl);

        BasicAuth basicAuth = new BasicAuth();
        basicAuth.setUsername(username);
        basicAuth.setPassword(password);
        basicAuth.setAuthType("BASIC");

        ValidatePostRequestAuth requestAuth = new ValidatePostRequestAuth(basicAuth);
        requestBody.setAuth(requestAuth);

        mockMvc.perform(post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk()) // API itself returns 200
                .andExpect(jsonPath("$.summary.files[0].fileName").value("gbfs-basic-fail.json"))
                .andExpect(jsonPath("$.summary.files[0].systemErrors").isNotEmpty())
                .andExpect(jsonPath("$.summary.files[0].systemErrors[0].error").value("CONNECTION_ERROR"))
                .andExpect(jsonPath("$.summary.files[0].systemErrors[0].message").value("HTTP error fetching file: 401 Unauthorized by test"));

        verify(getRequestedFor(urlEqualTo(feedPath))
                .withHeader("Authorization", containing("Basic "))); // Verify some basic auth header was sent
    }
}
