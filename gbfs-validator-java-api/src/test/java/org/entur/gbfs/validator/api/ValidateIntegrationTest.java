package org.entur.gbfs.validator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.entur.gbfs.validator.api.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
public class ValidateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testValidate_NoAuth_Success() throws Exception {
        // Create request with no auth
        ValidatePostRequest request = new ValidatePostRequest();
        request.setFeedUrl("http://example.com/gbfs.json");

        // Perform the test
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testValidate_BasicAuth_Success() throws Exception {
        // Create request with basic auth
        ValidatePostRequest request = new ValidatePostRequest();
        request.setFeedUrl("http://example.com/gbfs.json");

        BasicAuth basicAuth = new BasicAuth();
        basicAuth.setAuthType("basic");
        basicAuth.setUsername("user");
        basicAuth.setPassword("pass");
        request.setAuth(basicAuth);

        // Perform the test
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testValidate_BearerTokenAuth_Success() throws Exception {
        // Create request with bearer token auth
        ValidatePostRequest request = new ValidatePostRequest();
        request.setFeedUrl("http://example.com/gbfs.json");

        BearerTokenAuth bearerAuth = new BearerTokenAuth();
        bearerAuth.setAuthType("bearer");
        bearerAuth.setToken("token123");
        request.setAuth(bearerAuth);

        // Perform the test
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testValidate_OAuthClientCredentials_Success() throws Exception {
        // Create request with OAuth client credentials
        ValidatePostRequest request = new ValidatePostRequest();
        request.setFeedUrl("http://example.com/gbfs.json");

        OAuthClientCredentialsGrantAuth oauthAuth = new OAuthClientCredentialsGrantAuth();
        oauthAuth.setAuthType("oauth");
        oauthAuth.setTokenUrl("https://auth.example.com/token");
        oauthAuth.setClientId("client_id");
        oauthAuth.setClientSecret("client_secret");
        request.setAuth(oauthAuth);

        // Perform the test
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void testValidate_AuthFailure() throws Exception {
        // Create request with invalid auth
        ValidatePostRequest request = new ValidatePostRequest();
        request.setFeedUrl("http://example.com/gbfs.json");

        BasicAuth basicAuth = new BasicAuth();
        basicAuth.setAuthType("basic");
        basicAuth.setUsername("wrong_user");
        basicAuth.setPassword("wrong_password");
        request.setAuth(basicAuth);

        // Perform the test - we expect a 200 response with error details in the summary
        mockMvc.perform(post("/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.files[0].systemErrors").isNotEmpty());
    }
}
