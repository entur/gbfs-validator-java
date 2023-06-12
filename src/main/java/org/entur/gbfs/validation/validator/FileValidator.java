/*
 *
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.gbfs.validation.validator;

import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.validator.versions.Version;
import org.entur.gbfs.validation.validator.versions.VersionFactory;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileValidator {
    private static final Logger logger = LoggerFactory.getLogger(FileValidator.class);
    private final Version version;
    private static final Map<String, FileValidator> FILE_VALIDATORS = new ConcurrentHashMap<>();


    public static FileValidator getFileValidator(
            String detectedVersion
    ) {
        if (FILE_VALIDATORS.containsKey(detectedVersion)) {
            return FILE_VALIDATORS.get(detectedVersion);
        } else {
            Version version = VersionFactory.createVersion(detectedVersion);

            FileValidator fileValidator = new FileValidator(version);
            FILE_VALIDATORS.put(detectedVersion, fileValidator);
            return fileValidator;
        }
    }

    protected FileValidator(
            Version version
    ) {
        this.version = version;
    }

    public FileValidationResult validate(String feedName, Map<String, JSONObject> feedMap) {
        if (version.getFileNames().contains(feedName)) {
            JSONObject feed = feedMap.get(feedName);
            FileValidationResult fileValidationResult = new FileValidationResult();
            fileValidationResult.setFile(feedName);
            fileValidationResult.setRequired(isRequired(feedName));
            fileValidationResult.setExists(feed != null);
            fileValidationResult.setSchema(version.getSchema(feedName, feedMap).toString());
            fileValidationResult.setFileContents(Optional.ofNullable(feed).map(JSONObject::toString).orElse(null));
            fileValidationResult.setVersion(version.getVersionString());

            try {
                version.validate(feedName, feedMap);
            } catch (ValidationException validationException) {
                fileValidationResult.setErrors(mapToValidationErrors(validationException));
                fileValidationResult.setErrorsCount(validationException.getViolationCount());
            }

            return fileValidationResult;
        }

        logger.warn("Schema not found for gbfs feed={} version={}", feedName, version.getVersionString());
        return null;
    }

    List<FileValidationError> mapToValidationErrors(ValidationException validationException) {
        if (validationException.getCausingExceptions().isEmpty()) {
            FileValidationError error = new FileValidationError();
            error.setSchemaPath(validationException.getSchemaLocation());
            error.setViolationPath(validationException.getPointerToViolation());
            error.setMessage(validationException.getMessage());
            return Collections.singletonList(error);
        } else {
            return validationException.getCausingExceptions().stream()
                    .map(this::mapToValidationErrors)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

    private boolean isRequired(String feedName) {
        return version.isFileRequired(feedName);
    }


    public void validateMissingFile(FileValidationResult fvr) {
        if (version.getFileNames().contains(fvr.getFile())) {
            fvr.setVersion(version.getVersionString());
            fvr.setSchema(version.getSchema(fvr.getFile()).toString());
            fvr.setRequired(version.isFileRequired(fvr.getFile()));
        }
    }
}
