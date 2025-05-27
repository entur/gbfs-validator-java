package org.entur.gbfs.validator.api.handler;

import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult; // Internal validation result
import org.entur.gbfs.validator.api.model.GbfsFile;
import org.entur.gbfs.validator.api.model.SystemError as ApiSystemError; // API model
import org.entur.gbfs.validator.api.model.ValidatePostRequest;
import org.entur.gbfs.validator.api.model.ValidationResultSummary;
import org.entur.gbfs.validator.loader.LoadedFile;
import org.entur.gbfs.validator.loader.Loader;
import org.entur.gbfs.validator.loader.SystemError as LoaderSystemError; // Loader model
import org.entur.gbfs.validation.model.SystemError as ValidatorSystemError; // Validator model
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateApiDelegateHandlerTest {

    @Mock
    private Loader loader;

    @Mock
    private GbfsValidator gbfsValidator;

    @InjectMocks
    private ValidateApiDelegateHandler validateApiDelegateHandler;

    private ValidatePostRequest validatePostRequest;

    @BeforeEach
    void setUp() {
        validatePostRequest = new ValidatePostRequest();
        validatePostRequest.setFeedUrl("http://example.com/gbfs.json");
        // Mock the static factory method
        // This requires try-with-resources for the MockedStatic object or careful closing.
        // For simplicity in this context, I might not use it if direct injection of GbfsValidator works,
        // but ValidateApiDelegateHandler instantiates it via factory.
    }

    @Test
    void testLoaderErrorOnlyScenario() throws IOException {
        // --- Setup Loader Mock ---
        String feedUrl = "http://example.com/gbfs.json";
        LoadedFile gbfsLoadedFile = new LoadedFile(
                "gbfs.json",
                feedUrl,
                null, // No content due to error
                null, // Language
                List.of(new LoaderSystemError("FILE_NOT_FOUND", "Discovery file not found at " + feedUrl))
        );
        when(loader.load(feedUrl)).thenReturn(List.of(gbfsLoadedFile));

        // --- Setup Validator Mock ---
        // Validator receives no files or an empty map if discovery fails catastrophically
        ValidationResult internalValidationResult = new ValidationResult(
                new org.entur.gbfs.validation.model.ValidationSummary(null, 0L, 0),
                new HashMap<>()
        );
        // We need to mock the static GbfsValidatorFactory.getGbfsJsonValidator() call
        try (MockedStatic<GbfsValidatorFactory> mockedFactory = Mockito.mockStatic(GbfsValidatorFactory.class)) {
            mockedFactory.when(GbfsValidatorFactory::getGbfsJsonValidator).thenReturn(gbfsValidator);
            when(gbfsValidator.validate(any(Map.class))).thenReturn(internalValidationResult);

            // --- Execute ---
            ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> response =
                    validateApiDelegateHandler.validatePost(validatePostRequest);

            // --- Assertions ---
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            ValidationResultSummary summary = response.getBody().getSummary();
            assertNotNull(summary);
            assertNotNull(summary.getFiles());
            assertEquals(1, summary.getFiles().size());

            GbfsFile responseGbfsFile = summary.getFiles().get(0);
            assertEquals("gbfs.json", responseGbfsFile.getName());
            assertEquals(feedUrl, responseGbfsFile.getUrl());
            assertTrue(responseGbfsFile.getErrors().isEmpty(), "Validation errors should be empty.");
            assertNotNull(responseGbfsFile.getSystemErrors());
            assertEquals(1, responseGbfsFile.getSystemErrors().size());

            ApiSystemError systemError = responseGbfsFile.getSystemErrors().get(0);
            assertEquals("FILE_NOT_FOUND", systemError.getError());
            assertEquals("Discovery file not found at " + feedUrl, systemError.getMessage());
        }
    }

    @Test
    void testParserErrorOnlyScenario() throws IOException {
        // --- Setup Loader Mock ---
        String feedName = "system_information.json";
        String feedUrl = "http://example.com/" + feedName;
        String discoveryFeedUrl = "http://example.com/gbfs.json";
        String malformedJsonContent = "{ \"data\": { \"system_id\": \"test_system\""; // Unterminated

        LoadedFile gbfsDiscoveryFile = new LoadedFile(
                "gbfs.json",
                discoveryFeedUrl,
                new ByteArrayInputStream(("{ \"last_updated\": 1, \"ttl\": 1, \"version\": \"2.3\", \"data\": { \"feeds\": [ { \"name\": \"" + feedName + "\", \"url\": \"" + feedUrl + "\" } ] } }").getBytes(StandardCharsets.UTF_8)),
                null,
                new ArrayList<>()
        );

        LoadedFile systemInfoLoadedFile = new LoadedFile(
                feedName,
                feedUrl,
                new ByteArrayInputStream(malformedJsonContent.getBytes(StandardCharsets.UTF_8)),
                null, // Assuming v2.3+ where language is not in individual files
                new ArrayList<>()
        );
        when(loader.load(discoveryFeedUrl)).thenReturn(List.of(gbfsDiscoveryFile, systemInfoLoadedFile));


        // --- Setup Validator Mock ---
        // GbfsJsonValidator is now responsible for creating FileValidationResult with PARSE_ERROR
        Map<String, FileValidationResult> validatedFilesMap = new HashMap<>();
        FileValidationResult systemInfoFVR = new FileValidationResult(
                feedName,
                true, // required
                true, // exists
                0,    // errorsCount (validation errors)
                "v2.3/" + feedName + ".json", // schema
                malformedJsonContent, // fileContents
                null, // version (file specific, null if parse error)
                Collections.emptyList(), // validation errors
                List.of(new ValidatorSystemError("PARSE_ERROR", "Unterminated object at character offset...")) // System error from parser
        );
        // Also need one for gbfs.json, assuming it's fine
         FileValidationResult gbfsFVR = new FileValidationResult(
                "gbfs.json", true, true, 0,
                "v2.3/gbfs.json", gbfsDiscoveryFile.fileContents().toString(), // This is not ideal, but for test
                "2.3", Collections.emptyList(), Collections.emptyList()
        );
        validatedFilesMap.put(feedName, systemInfoFVR);
        validatedFilesMap.put("gbfs.json", gbfsFVR);


        ValidationResult internalValidationResult = new ValidationResult(
                new org.entur.gbfs.validation.model.ValidationSummary("2.3", 0L, 0),
                validatedFilesMap
        );

        try (MockedStatic<GbfsValidatorFactory> mockedFactory = Mockito.mockStatic(GbfsValidatorFactory.class)) {
            mockedFactory.when(GbfsValidatorFactory::getGbfsJsonValidator).thenReturn(gbfsValidator);
            // The validatorInputMap in ValidateApiDelegateHandler will contain system_information.json
            // The GbfsJsonValidator's 'validate' method will internally handle the parsing.
            // So, we mock the result of gbfsValidator.validate(...)
            when(gbfsValidator.validate(any(Map.class))).thenAnswer(invocation -> {
                // The input to validate() is Map<String, InputStream>.
                // We need to ensure our mocked FVRs are returned.
                // This is a bit simplified as the real GbfsJsonValidator would do the parsing.
                // For this test, we assume GbfsJsonValidator correctly produced systemInfoFVR.
                return internalValidationResult;
            });


            // --- Execute ---
            validatePostRequest.setFeedUrl(discoveryFeedUrl);
            ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> response =
                    validateApiDelegateHandler.validatePost(validatePostRequest);

            // --- Assertions ---
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            ValidationResultSummary summary = response.getBody().getSummary();
            assertNotNull(summary);
            assertNotNull(summary.getFiles());
            assertEquals(2, summary.getFiles().size()); // gbfs.json and system_information.json

            GbfsFile responseSystemInfoFile = summary.getFiles().stream()
                    .filter(f -> f.getName().equals(feedName))
                    .findFirst().orElse(null);

            assertNotNull(responseSystemInfoFile, "system_information.json should be in the response");
            assertEquals(feedName, responseSystemInfoFile.getName());
            assertEquals(feedUrl, responseSystemInfoFile.getUrl());
            assertTrue(responseSystemInfoFile.getErrors().isEmpty(), "Validation errors should be empty for a parser error.");
            assertNotNull(responseSystemInfoFile.getSystemErrors());
            assertEquals(1, responseSystemInfoFile.getSystemErrors().size());

            ApiSystemError systemError = responseSystemInfoFile.getSystemErrors().get(0);
            assertEquals("PARSE_ERROR", systemError.getError());
            assertTrue(systemError.getMessage().contains("Unterminated object"), "Message was: " + systemError.getMessage());
        }
    }

    @Test
    void testTwoFiles_OneLoaderError_OneParserError() throws IOException {
        // --- Setup Loader Mock ---
        String gbfsDiscoveryUrl = "http://example.com/gbfs.json";
        String versionsFeedName = "gbfs_versions.json";
        String versionsFeedUrl = "http://example.com/" + versionsFeedName;
        String sysInfoFeedName = "system_information.json";
        String sysInfoFeedUrl = "http://example.com/" + sysInfoFeedName;

        String malformedJsonContent = "{ \"data\": { \"system_id\": \"test_system\""; // Unterminated

        LoadedFile gbfsFile = new LoadedFile(
                "gbfs.json", gbfsDiscoveryUrl,
                new ByteArrayInputStream(("{ \"last_updated\": 1, \"ttl\": 1, \"version\": \"2.3\", \"data\": { \"feeds\": [ " +
                        "{ \"name\": \"" + versionsFeedName + "\", \"url\": \"" + versionsFeedUrl + "\" }, " +
                        "{ \"name\": \"" + sysInfoFeedName + "\", \"url\": \"" + sysInfoFeedUrl + "\" } " +
                        "] } }").getBytes(StandardCharsets.UTF_8)),
                null, new ArrayList<>()
        );
        LoadedFile versionsFileWithError = new LoadedFile(
                versionsFeedName, versionsFeedUrl, null, null,
                List.of(new LoaderSystemError("CONNECTION_ERROR", "Could not connect to " + versionsFeedUrl))
        );
        LoadedFile sysInfoFileToParseError = new LoadedFile(
                sysInfoFeedName, sysInfoFeedUrl,
                new ByteArrayInputStream(malformedJsonContent.getBytes(StandardCharsets.UTF_8)),
                null, new ArrayList<>()
        );
        when(loader.load(gbfsDiscoveryUrl)).thenReturn(List.of(gbfsFile, versionsFileWithError, sysInfoFileToParseError));

        // --- Setup Validator Mock ---
        Map<String, FileValidationResult> validatedFilesMap = new HashMap<>();
        validatedFilesMap.put("gbfs.json", new FileValidationResult("gbfs.json", true, true, 0, "v2.3/gbfs.json", "{}", "2.3", Collections.emptyList(), Collections.emptyList()));
        validatedFilesMap.put(sysInfoFeedName, new FileValidationResult(sysInfoFeedName, true, true, 0, "v2.3/" + sysInfoFeedName +".json", malformedJsonContent, null, Collections.emptyList(), List.of(new ValidatorSystemError("PARSE_ERROR", "Unterminated object"))));
        // versionsFileWithError will not be in validatedFilesMap as it's not passed to validator due to null content

        ValidationResult internalValidationResult = new ValidationResult(
                new org.entur.gbfs.validation.model.ValidationSummary("2.3", 0L, 0),
                validatedFilesMap
        );

        try (MockedStatic<GbfsValidatorFactory> mockedFactory = Mockito.mockStatic(GbfsValidatorFactory.class)) {
            mockedFactory.when(GbfsValidatorFactory::getGbfsJsonValidator).thenReturn(gbfsValidator);
            when(gbfsValidator.validate(any(Map.class))).thenReturn(internalValidationResult);

            // --- Execute ---
            validatePostRequest.setFeedUrl(gbfsDiscoveryUrl);
            ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> response =
                    validateApiDelegateHandler.validatePost(validatePostRequest);

            // --- Assertions ---
            assertEquals(HttpStatus.OK, response.getStatusCode());
            ValidationResultSummary summary = response.getBody().getSummary();
            assertNotNull(summary.getFiles());
            assertEquals(3, summary.getFiles().size()); // gbfs.json, gbfs_versions.json, system_information.json

            GbfsFile gbfsVersionsResponse = summary.getFiles().stream().filter(f -> f.getName().equals(versionsFeedName)).findFirst().orElseThrow();
            assertTrue(gbfsVersionsResponse.getErrors().isEmpty());
            assertEquals(1, gbfsVersionsResponse.getSystemErrors().size());
            assertEquals("CONNECTION_ERROR", gbfsVersionsResponse.getSystemErrors().get(0).getError());

            GbfsFile sysInfoResponse = summary.getFiles().stream().filter(f -> f.getName().equals(sysInfoFeedName)).findFirst().orElseThrow();
            assertTrue(sysInfoResponse.getErrors().isEmpty());
            assertEquals(1, sysInfoResponse.getSystemErrors().size());
            assertEquals("PARSE_ERROR", sysInfoResponse.getSystemErrors().get(0).getError());
        }
    }

    @Test
    void testValidationErrorOnlyScenario() throws IOException {
        String feedName = "system_information.json";
        String feedUrl = "http://example.com/" + feedName;
        String discoveryFeedUrl = "http://example.com/gbfs.json";
        String validJsonWithSchemaError = "{ \"data\": { \"language\": \"en\" } }"; // Missing system_id for v2.3

        LoadedFile gbfsDiscoveryFile = new LoadedFile("gbfs.json", discoveryFeedUrl, new ByteArrayInputStream(("{ \"last_updated\": 1, \"ttl\": 1, \"version\": \"2.3\", \"data\": { \"feeds\": [ { \"name\": \"" + feedName + "\", \"url\": \"" + feedUrl + "\" } ] } }").getBytes(StandardCharsets.UTF_8)), null, new ArrayList<>());
        LoadedFile sysInfoLoadedFile = new LoadedFile(feedName, feedUrl, new ByteArrayInputStream(validJsonWithSchemaError.getBytes(StandardCharsets.UTF_8)), null, new ArrayList<>());
        when(loader.load(discoveryFeedUrl)).thenReturn(List.of(gbfsDiscoveryFile, sysInfoLoadedFile));

        Map<String, FileValidationResult> validatedFilesMap = new HashMap<>();
        validatedFilesMap.put("gbfs.json", new FileValidationResult("gbfs.json", true, true, 0, "v2.3/gbfs.json", "{}", "2.3", Collections.emptyList(), Collections.emptyList()));
        FileValidationResult sysInfoFVR = new FileValidationResult(
                feedName, true, true, 1, "v2.3/" + feedName + ".json", validJsonWithSchemaError, "2.3",
                List.of(new org.entur.gbfs.validation.model.FileValidationError("required", "/data", "/data", "system_id is required")),
                Collections.emptyList() // No system errors
        );
        validatedFilesMap.put(feedName, sysInfoFVR);
        ValidationResult internalValidationResult = new ValidationResult(new org.entur.gbfs.validation.model.ValidationSummary("2.3", 0L, 1), validatedFilesMap);

        try (MockedStatic<GbfsValidatorFactory> mockedFactory = Mockito.mockStatic(GbfsValidatorFactory.class)) {
            mockedFactory.when(GbfsValidatorFactory::getGbfsJsonValidator).thenReturn(gbfsValidator);
            when(gbfsValidator.validate(any(Map.class))).thenReturn(internalValidationResult);

            validatePostRequest.setFeedUrl(discoveryFeedUrl);
            ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> response = validateApiDelegateHandler.validatePost(validatePostRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            ValidationResultSummary summary = response.getBody().getSummary();
            GbfsFile responseSystemInfoFile = summary.getFiles().stream().filter(f -> f.getName().equals(feedName)).findFirst().orElseThrow();
            assertTrue(responseSystemInfoFile.getSystemErrors().isEmpty());
            assertEquals(1, responseSystemInfoFile.getErrors().size());
            assertEquals("system_id is required", responseSystemInfoFile.getErrors().get(0).getMessage());
        }
    }

    @Test
    void testNoErrorsScenario() throws IOException {
        String feedName = "system_information.json";
        String feedUrl = "http://example.com/" + feedName;
        String discoveryFeedUrl = "http://example.com/gbfs.json";
        String validJson = "{ \"last_updated\": 123, \"ttl\": 60, \"version\": \"2.3\", \"data\": { \"system_id\": \"test_sid\", \"language\": \"en\", \"name\": \"Test System\" } }";

        LoadedFile gbfsDiscoveryFile = new LoadedFile("gbfs.json", discoveryFeedUrl, new ByteArrayInputStream(("{ \"last_updated\": 1, \"ttl\": 1, \"version\": \"2.3\", \"data\": { \"feeds\": [ { \"name\": \"" + feedName + "\", \"url\": \"" + feedUrl + "\" } ] } }").getBytes(StandardCharsets.UTF_8)), null, new ArrayList<>());
        LoadedFile sysInfoLoadedFile = new LoadedFile(feedName, feedUrl, new ByteArrayInputStream(validJson.getBytes(StandardCharsets.UTF_8)), null, new ArrayList<>());
        when(loader.load(discoveryFeedUrl)).thenReturn(List.of(gbfsDiscoveryFile, sysInfoLoadedFile));

        Map<String, FileValidationResult> validatedFilesMap = new HashMap<>();
         validatedFilesMap.put("gbfs.json", new FileValidationResult("gbfs.json", true, true, 0, "v2.3/gbfs.json", "{}", "2.3", Collections.emptyList(), Collections.emptyList()));
        FileValidationResult sysInfoFVR = new FileValidationResult(
                feedName, true, true, 0, "v2.3/" + feedName + ".json", validJson, "2.3",
                Collections.emptyList(), Collections.emptyList()
        );
        validatedFilesMap.put(feedName, sysInfoFVR);
        ValidationResult internalValidationResult = new ValidationResult(new org.entur.gbfs.validation.model.ValidationSummary("2.3", 0L, 0), validatedFilesMap);

        try (MockedStatic<GbfsValidatorFactory> mockedFactory = Mockito.mockStatic(GbfsValidatorFactory.class)) {
            mockedFactory.when(GbfsValidatorFactory::getGbfsJsonValidator).thenReturn(gbfsValidator);
            when(gbfsValidator.validate(any(Map.class))).thenReturn(internalValidationResult);

            validatePostRequest.setFeedUrl(discoveryFeedUrl);
            ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> response = validateApiDelegateHandler.validatePost(validatePostRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            ValidationResultSummary summary = response.getBody().getSummary();
            GbfsFile responseSystemInfoFile = summary.getFiles().stream().filter(f -> f.getName().equals(feedName)).findFirst().orElseThrow();
            assertTrue(responseSystemInfoFile.getSystemErrors().isEmpty());
            assertTrue(responseSystemInfoFile.getErrors().isEmpty());
        }
    }
}
