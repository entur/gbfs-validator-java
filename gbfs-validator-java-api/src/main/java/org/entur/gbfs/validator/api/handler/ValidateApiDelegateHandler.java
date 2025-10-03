/*
 *
 *  *
 *  *
 *  *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  *  * You may not use this work except in compliance with the Licence.
 *  *  * You may obtain a copy of the Licence at:
 *  *  *
 *  *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the Licence for the specific language governing permissions and
 *  *  * limitations under the Licence.
 *  *
 *
 */

package org.entur.gbfs.validator.api.handler;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jakarta.annotation.PreDestroy;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validator.api.gen.ValidateApiDelegate;
import org.entur.gbfs.validator.api.model.BasicAuth;
import org.entur.gbfs.validator.api.model.BearerTokenAuth;
import org.entur.gbfs.validator.api.model.FileError;
import org.entur.gbfs.validator.api.model.GbfsFile;
import org.entur.gbfs.validator.api.model.OAuthClientCredentialsGrantAuth;
import org.entur.gbfs.validator.api.model.SystemError;
import org.entur.gbfs.validator.api.model.ValidatePostRequest;
import org.entur.gbfs.validator.api.model.ValidationResultSummary;
import org.entur.gbfs.validator.loader.LoadedFile;
import org.entur.gbfs.validator.loader.Loader;
import org.entur.gbfs.validator.loader.auth.Authentication;
import org.entur.gbfs.validator.loader.LoaderError;
import org.entur.gbfs.validation.model.ValidatorError;
import org.openapitools.jackson.nullable.JsonNullable;
import org.entur.gbfs.validator.api.model.ValidatePostRequestAuth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidateApiDelegateHandler implements ValidateApiDelegate {
    private static final Logger logger = LoggerFactory.getLogger(ValidateApiDelegateHandler.class);

    private final Loader loader;

    public ValidateApiDelegateHandler(Loader loader) {
        this.loader = loader;
    }

    @PreDestroy
    public void destroy() {
        try {
            if (loader != null) {
                loader.close();
            }
        } catch (IOException e) {
            logger.error("Error closing Loader", e);
        }
    }

    @Override
    public ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> validatePost(ValidatePostRequest validatePostRequest) {
        logger.debug("Received request for url: {}", validatePostRequest.getFeedUrl());
        try {
            Authentication loaderAuth = null;
            ValidatePostRequestAuth apiAuth = validatePostRequest.getAuth();

            if (apiAuth != null) {
                if (apiAuth instanceof BasicAuth basic) {
                    if (basic.getUsername() != null && basic.getPassword() != null) {
                        loaderAuth = new org.entur.gbfs.validator.loader.auth.BasicAuth(basic.getUsername(), basic.getPassword());
                    }
                } else if (apiAuth instanceof BearerTokenAuth bearer) {
                    if (bearer.getToken() != null) {
                        loaderAuth = new org.entur.gbfs.validator.loader.auth.BearerTokenAuth(bearer.getToken());
                    }
                } else if (apiAuth instanceof OAuthClientCredentialsGrantAuth oauth) {
                    if (oauth.getClientId() != null && oauth.getClientSecret() != null && oauth.getTokenUrl() != null) {
                        loaderAuth = new org.entur.gbfs.validator.loader.auth.OAuthClientCredentialsGrantAuth(oauth.getClientId(), oauth.getClientSecret(), oauth.getTokenUrl().toString());
                    }
                }
            }

            List<LoadedFile> allLoadedFiles = loader.load(validatePostRequest.getFeedUrl(), loaderAuth);

            logger.debug("Loaded files: {}", allLoadedFiles.size());

            // Group loaded files by language. For files without a language (e.g. gbfs.json, or if loader doesn't set it),
            // use a default key or handle them as appropriate. loader.load() should populate language for pre-v3.
            // For v3+, language is typically null at the LoadedFile stage for gbfs.json itself.
            Multimap<String, LoadedFile> filesByLanguage = MultimapBuilder.hashKeys().arrayListValues().build();
            for (LoadedFile loadedFile : allLoadedFiles) {
                // Use a placeholder if language is null to ensure they are processed.
                // gbfs.json (discovery file) itself might have null language.
                String langKey = loadedFile.language() != null ? loadedFile.language() : "default_lang";
                filesByLanguage.put(langKey, loadedFile);
            }

            GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
            List<org.entur.gbfs.validator.api.model.ValidationResult> resultsPerLanguage = new ArrayList<>();

            filesByLanguage.asMap().forEach((languageKey, loadedFilesForLang) -> {
                logger.debug("Processing language group: {}", languageKey);
                Map<String, InputStream> validatorInputMap = new HashMap<>();
                // Keep track of LoadedFile objects for this language group to pass to mapping
                List<LoadedFile> currentLanguageLoadedFiles = new ArrayList<>(loadedFilesForLang);

                for (LoadedFile file : currentLanguageLoadedFiles) {
                    // Only try to validate files that have content.
                    // Files with system errors from loader might have null fileContents.
                    if (file.fileContents() != null) {
                        validatorInputMap.put(file.fileName(), file.fileContents());
                    }
                    // Note: urlMap is not used in the refined plan for mapFiles directly,
                    // as LoadedFile itself contains the URL.
                }

                ValidationResult internalValidationResult = validator.validate(validatorInputMap);

                resultsPerLanguage.add(
                        mapValidationResult(
                                internalValidationResult,
                                currentLanguageLoadedFiles, // Pass the list of LoadedFile for this language
                                "default_lang".equals(languageKey) ? null : languageKey // Pass actual language, or null
                        )
                );
                logger.debug("Processed {} files for language group: {}", currentLanguageLoadedFiles.size(), languageKey);
            });

            // merge the list of ValidationResult into a single validation result
            return ResponseEntity.ok(
                    mergeValidationResults(resultsPerLanguage)
            );

        } catch (IOException e) {
            // Consider mapping IOExceptions from loader to a SystemError in the response too
            logger.error("IOException during validation process", e);
            // Depending on desired API behavior, could return a 500 with a SystemError
            throw new RuntimeException(e); // Or handle more gracefully
        }
    }

    private org.entur.gbfs.validator.api.model.ValidationResult mergeValidationResults(List<org.entur.gbfs.validator.api.model.ValidationResult> results) {
        if (results.isEmpty()) {
            // Handle case with no results, perhaps due to total load failure of discovery file
            org.entur.gbfs.validator.api.model.ValidationResult emptyApiResult = new org.entur.gbfs.validator.api.model.ValidationResult();
            ValidationResultSummary emptySummary = new ValidationResultSummary();
            emptySummary.setValidatorVersion("2.0.30-SNAPSHOT"); // TODO: Inject this
            emptySummary.setFiles(new ArrayList<>());
            emptyApiResult.setSummary(emptySummary);
            return emptyApiResult;
        }

        org.entur.gbfs.validator.api.model.ValidationResult mergedResult = new org.entur.gbfs.validator.api.model.ValidationResult();
        ValidationResultSummary summary = new ValidationResultSummary();
        // Assuming validatorVersion is consistent or taking from the first result
        summary.setValidatorVersion(results.get(0).getSummary().getValidatorVersion());
        List<GbfsFile> allFiles = new ArrayList<>();
        results.forEach(result -> {
            if (result.getSummary() != null && result.getSummary().getFiles() != null) {
                allFiles.addAll(result.getSummary().getFiles());
            }
        });

        // Dedup files if necessary, though current logic iterates by language group,
        // so gbfs.json might appear multiple times if language key was 'default_lang' for it.
        // For now, simple aggregation. A more sophisticated merge might be needed if files are duplicated across "language" groups.
        // A simple distinct by URL or name could be:
        // List<GbfsFile> distinctFiles = allFiles.stream().filter(distinctByKey(GbfsFile::getUrl)).toList();
        // summary.setFiles(distinctFiles);
        summary.setFiles(allFiles); // Using simple aggregation for now.

        mergedResult.setSummary(summary);
        return mergedResult;
    }
    /*
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
    */


    private org.entur.gbfs.validator.api.model.ValidationResult mapValidationResult(
            ValidationResult internalValidationResult,
            List<LoadedFile> loadedFilesForLanguage, // Now includes LoadedFile list
            String language // Actual language string, can be null
    ) {
        ValidationResultSummary validationResultSummary = new ValidationResultSummary();
        validationResultSummary.setValidatorVersion("2.0.30-SNAPSHOT"); // TODO inject this value

        validationResultSummary.setFiles(
                mapFiles(loadedFilesForLanguage, internalValidationResult.files(), language)
        );

        org.entur.gbfs.validator.api.model.ValidationResult apiResult = new org.entur.gbfs.validator.api.model.ValidationResult();
        apiResult.setSummary(validationResultSummary);
        return apiResult;
    }

    private List<GbfsFile> mapFiles(
            List<LoadedFile> loadedFilesForLanguage,
            Map<String, FileValidationResult> validatedFileResultsMap,
            String language // Actual language string, can be null for gbfs.json
    ) {
        List<GbfsFile> apiGbfsFiles = new ArrayList<>();

        for (LoadedFile loadedFile : loadedFilesForLanguage) {
            GbfsFile apiFile = new GbfsFile();
            apiFile.setName(loadedFile.fileName());
            apiFile.setUrl(loadedFile.url());

            List<SystemError> combinedApiSystemErrors = new ArrayList<>();

            // System errors from loader
            List<LoaderError> loaderSystemErrors = loadedFile.loaderErrors();
            if (loaderSystemErrors != null && !loaderSystemErrors.isEmpty()) {
                combinedApiSystemErrors.addAll(mapLoaderSystemErrorsToApi(loaderSystemErrors));
            }

            FileValidationResult validationResult = validatedFileResultsMap.get(loadedFile.fileName());

            if (validationResult != null) {
                // File was processed by validator
                apiFile.setSchema(validationResult.schema());
                apiFile.setVersion(validationResult.version());
                apiFile.setErrors(mapFileValidationErrors(validationResult.errors()));

                // Add system errors from validator (parsing errors)
                List<ValidatorError> validatorSystemErrors = validationResult.validatorErrors();
                if (validatorSystemErrors != null && !validatorSystemErrors.isEmpty()) {
                    combinedApiSystemErrors.addAll(mapValidatorSystemErrorsToApi(validatorSystemErrors));
                }
            } else {
                // File was not processed by validator (e.g. content was null due to load error)
                // or validator skipped it. Schema/Version might be unknown.
                // Validation errors are empty.
                apiFile.setErrors(new ArrayList<>());
            }

            apiFile.setSystemErrors(combinedApiSystemErrors);

            // Set language - gbfs.json (discovery file) typically has no language.
            // Other files get the language of their group.
            if (loadedFile.fileName().equals("gbfs.json") || loadedFile.fileName().equals("gbfs")) {
                 apiFile.setLanguage(null); // Explicitly null for discovery
            } else {
                 apiFile.setLanguage(JsonNullable.of(language));
            }
            apiGbfsFiles.add(apiFile);
        }
        return apiGbfsFiles;
    }

    private List<SystemError> mapLoaderSystemErrorsToApi(List<LoaderError> loaderSystemErrors) {
        if (loaderSystemErrors == null) {
            return new ArrayList<>();
        }
        return loaderSystemErrors.stream().map(loaderError -> {
            SystemError apiError = new SystemError();
            apiError.setError(loaderError.error());
            apiError.setMessage(loaderError.message());
            return apiError;
        }).toList();
    }

    private List<SystemError> mapValidatorSystemErrorsToApi(List<ValidatorError> validatorSystemErrors) {
        if (validatorSystemErrors == null) {
            return new ArrayList<>();
        }
        return validatorSystemErrors.stream().map(validatorError -> {
            SystemError apiError = new SystemError();
            apiError.setError(validatorError.error());
            apiError.setMessage(validatorError.message());
            return apiError;
        }).toList();
    }

    private List<FileError> mapFileValidationErrors(List<FileValidationError> errors) {
        if (errors == null) {
            return new ArrayList<>();
        }
        return errors.stream().map(error -> {
            var mapped = new FileError();
            mapped.setMessage(error.message());
            mapped.setInstancePath(error.violationPath());
            mapped.setSchemaPath(error.schemaPath());
            mapped.setKeyword(error.keyword());
            return mapped;
        }).toList();
    }
}
