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
import org.entur.gbfs.validation.versions.Version;
import org.entur.gbfs.validation.versions.VersionFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileValidator {
    private static final Logger logger = LoggerFactory.getLogger(FileValidator.class);
    private final Version version;
    private final Map<String, Schema> schemas;

    private static final Map<String, FileValidator> FILE_VALIDATORS = new ConcurrentHashMap<>();

    public static FileValidator getFileValidator(
            String detectedVersion
    ) {
        if (FILE_VALIDATORS.containsKey(detectedVersion)) {
            return FILE_VALIDATORS.get(detectedVersion);
        } else {
            Version version = VersionFactory.createVersion(detectedVersion);
            Map<String, Schema> schemas = FileValidator.getSchemas(version);
            FileValidator fileValidator = new FileValidator(version, schemas);
            FILE_VALIDATORS.put(detectedVersion, fileValidator);
            return fileValidator;
        }
    }

    private FileValidator(
            Version version,
            Map<String, Schema> schemas
    ) {
        this.version = version;
        this.schemas = schemas;
    }

    public FileValidationResult validate(String feedName, JSONObject feed) {
        if (schemas.containsKey(feedName)) {
            return validate(schemas.get(feedName), feed, feedName);
        }

        throw new UnsupportedOperationException("Unknown gbfs feed: " + feedName);
    }

    private FileValidationResult validate(Schema schema, JSONObject feed, String feedName) {
        FileValidationResult fileValidationResult = new FileValidationResult();
        fileValidationResult.setFile(feedName);
        fileValidationResult.setRequired(isRequired(feedName));
        fileValidationResult.setExists(feed != null);
        fileValidationResult.setSchema(schema.toString());
        fileValidationResult.setFileContents(Optional.ofNullable(feed).map(JSONObject::toString).orElse(null));
        fileValidationResult.setVersion(version.getVersion());

        try {
            schema.validate(feed);
        } catch (ValidationException validationException) {
            fileValidationResult.setErrors(mapToValidationErrors(validationException));
            fileValidationResult.setErrorsCount(validationException.getViolationCount());
        }

        return fileValidationResult;
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

    protected static Map<String, Schema> getSchemas(Version version) {
        Map<String, Schema> schemas = new HashMap<>();
        version.getFeeds().forEach(feed -> {
            Schema schema = loadSchema(version.getVersion(), feed);
            if (schema != null) {
                schemas.put(feed, schema);
            }
        });
        return schemas;
    }

    protected static Schema loadSchema(String version, String feedName) {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("schema/v"+version+"/"+feedName+".json");

        if (inputStream == null) {
            logger.warn("Unable to load schema version={} feedName={}", version, feedName);
            return null;
        }

        JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
        SchemaLoader schemaLoader = SchemaLoader.builder()
                .enableOverrideOfBuiltInFormatValidators()
                .addFormatValidator(new URIFormatValidator())
                .schemaJson(rawSchema)
                .build();

        return schemaLoader.load().build();
    }

    public void validateMissingFile(FileValidationResult fvr) {
        if (version.getFeeds().contains(fvr.getFile())) {
            fvr.setVersion(version.getVersion());
            fvr.setSchema(schemas.get(fvr.getFile()).toString());
            fvr.setRequired(version.isFileRequired(fvr.getFile()));
        }
    }
}
