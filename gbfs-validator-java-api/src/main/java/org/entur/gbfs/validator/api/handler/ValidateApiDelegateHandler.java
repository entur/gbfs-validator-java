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
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validator.api.gen.ValidateApiDelegate;

import org.entur.gbfs.validator.api.model.FileError;
import org.entur.gbfs.validator.api.model.GbfsFile;
import org.entur.gbfs.validator.api.model.ValidatePostRequest;
import org.entur.gbfs.validator.api.model.ValidationResultSummary;
import org.entur.gbfs.validator.loader.LoadedFile;
import org.entur.gbfs.validator.loader.Loader;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidateApiDelegateHandler implements ValidateApiDelegate {

    @Override
    public ResponseEntity<org.entur.gbfs.validator.api.model.ValidationResult> validatePost(ValidatePostRequest validatePostRequest) {
        Loader loader = new Loader();
        try {
            List<LoadedFile> loadedFiles = loader.load(validatePostRequest.getFeedUrl());

            Multimap<String, LoadedFile> fileMap = MultimapBuilder.hashKeys().arrayListValues().build();
            for (LoadedFile loadedFile : loadedFiles) {
                fileMap.put(loadedFile.language(), loadedFile);
            }

            GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();

            // In order to support multiple languages (prior to GBFS 3.0), we need to validate each language separately
            // and then merge the results
            List<org.entur.gbfs.validator.api.model.ValidationResult> results = new ArrayList<>();
            fileMap.keySet().forEach(language -> {
                Map<String, InputStream> validatorInputMap = new HashMap<>();
                Map<String, String> urlMap = new HashMap<>();
                fileMap.get(language).forEach(file -> {
                    validatorInputMap.put(file.fileName(), file.fileContents());
                    urlMap.put(file.fileName(), file.url());
                });
                results.add(
                        mapValidationResult(
                                validator.validate(
                                        validatorInputMap
                                ),
                                urlMap,
                                language
                        )
                );
            });

            // merge the list of ValidationResult into a single validation result
            return ResponseEntity.ok(
                    mergeValidationResults(results)
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private org.entur.gbfs.validator.api.model.ValidationResult mergeValidationResults(List<org.entur.gbfs.validator.api.model.ValidationResult> results) {
        org.entur.gbfs.validator.api.model.ValidationResult mergedResult = new org.entur.gbfs.validator.api.model.ValidationResult();
        ValidationResultSummary summary = new ValidationResultSummary();
        summary.setValidatorVersion(results.get(0).getSummary().getValidatorVersion());
        summary.setFiles(new ArrayList<>());
        summary.getFiles().add(results.get(0).getSummary().getFiles().stream().filter(file -> file.getName().equals("gbfs")).findFirst().orElse(null));
        results.forEach(result -> summary.getFiles().addAll(result.getSummary().getFiles().stream().filter(file -> !file.getName().equals("gbfs")).toList()));
        mergedResult.setSummary(summary);
        return mergedResult;
    }

    private org.entur.gbfs.validator.api.model.ValidationResult mapValidationResult(ValidationResult validationResult, Map<String, String> urlMap, @Nullable String language) {
        ValidationResultSummary validationResultSummary = new ValidationResultSummary();
        validationResultSummary.setValidatorVersion("2.0.30-SNAPSHOT"); // TODO inject this value
        validationResultSummary.setFiles(mapFiles(validationResult.files(), urlMap, language));
        org.entur.gbfs.validator.api.model.ValidationResult validationResultOption1 = new org.entur.gbfs.validator.api.model.ValidationResult();
        validationResultOption1.setSummary(validationResultSummary);
        return validationResultOption1;
    }

    private List<GbfsFile> mapFiles(Map<String, FileValidationResult> files, Map<String, String> urlMap, @Nullable String language) {
        return files.entrySet().stream().map(entry -> {
            String fileName = entry.getKey();
            FileValidationResult fileValidationResult = entry.getValue();

            GbfsFile file = new GbfsFile();
            file.setName(fileName);
            file.setUrl(urlMap.get(fileName));
            file.setSchema(fileValidationResult.schema());
            file.setVersion(fileValidationResult.version());

            // The discovery file itself has no language, and is listed once per system
            file.setLanguage(!fileName.equals("gbfs") ? JsonNullable.of(language) : null);
            file.setErrors(mapFileErrors(fileValidationResult.errors()));
            return file;
        }).toList();

    }

    private List<FileError> mapFileErrors(List<FileValidationError> errors) {
        return errors.stream().map(error -> {
            var mapped = new FileError();
            mapped.setMessage(error.message());
            mapped.setInstancePath(error.violationPath());
            mapped.setSchemaPath(error.schemaPath());
            //mapped.setParams(error.); // TODO no source?
            mapped.setKeyword(error.keyword());
            return mapped;
        }).toList();
    }
}
