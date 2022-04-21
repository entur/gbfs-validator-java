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

package org.entur.gbfs.validation.files;

import org.entur.gbfs.validation.versions.Version;
import org.entur.gbfs.validation.versions.VersionFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileValidator {
    private final Version version;
    private final Map<String, Schema> schemas;

    public static FileValidator getFileValidator(
            String detectedVersion
    ) {
        return new FileValidator(VersionFactory.createVersion(detectedVersion));
    }

    private FileValidator(
            Version version
    ) {
        this.version = version;
        this.schemas = FileValidator.getSchemas(version);
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
        fileValidationResult.setVersion(version.getVersion());

        try {
            schema.validate(feed);
        } catch (ValidationException validationException) {
            fileValidationResult.setErrors(mapToValidationErrors(validationException));
            fileValidationResult.setErrorsCount(validationException.getViolationCount());
        }

        return fileValidationResult;
    }

    List<ValidationError> mapToValidationErrors(ValidationException validationException) {
        if (validationException.getCausingExceptions().isEmpty()) {
            ValidationError error = new ValidationError();
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
            schemas.put(feed, loadSchema(version.getVersion(), feed));
        });

        return schemas;
    }

    protected static Schema loadSchema(String version, String feedName) {
        try {
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("schema/v"+version+"/"+feedName+".json");
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            SchemaLoader schemaLoader = SchemaLoader.builder()
                    .enableOverrideOfBuiltInFormatValidators()
                    .addFormatValidator(new URIFormatValidator())
                    .schemaJson(rawSchema)
                    .build();

            return schemaLoader.load().build();
        } catch (Exception e) {
            System.out.println("Caught exception loading schema for " + feedName + " and version " + version);
            throw e;
        }
    }
}