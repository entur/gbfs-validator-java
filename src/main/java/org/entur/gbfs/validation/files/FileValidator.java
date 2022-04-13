package org.entur.gbfs.validation.files;

import org.entur.gbfs.validation.versions.Version;
import org.entur.gbfs.validation.versions.Version1_0;
import org.entur.gbfs.validation.versions.Version1_1;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileValidator {
    private final Version version;
    private final Map<String, Schema> schemas;

    public static FileValidator getFileValidator(
            String detectedVersion,
            boolean isDocked,
            boolean isFreeFloating
    ) {
        switch (detectedVersion) {
            case "1.0":
                return new FileValidator(new Version1_0(isDocked, isFreeFloating));
            case "1.1":
                return new FileValidator(new Version1_1(isDocked, isFreeFloating));
            case "2.0":
            case "2.1":
            case "2.2":
            case "2.3":
            default:
                throw new UnsupportedOperationException("Version not implemented");
        }
    }

    private FileValidator(
            Version version
    ) {
        this.version = version;
        this.schemas = FileValidator.getSchemas(version, version.getFeeds());
    }

    public FileValidationResult validate(String feedName, JSONObject feed) {
        if (schemas.containsKey(feedName)) {
            return validate(schemas.get(feedName), feed, feedName);
        }

        throw new UnsupportedOperationException("Unknown gbfs feed");
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
            fileValidationResult.setError(validationException);
            fileValidationResult.setErrorsCount(validationException.getViolationCount());
        }

        return fileValidationResult;
    }

    private boolean isRequired(String feedName) {
        return version.isFileRequired(feedName);
    }

    protected static Map<String, Schema> getSchemas(Version version, List<String> feeds) {
        Map<String, Schema> schemas = new HashMap<>();

        feeds.forEach(feed -> {
            schemas.put(feed, loadSchema(version, feed));
        });

        return schemas;
    }

    protected static Schema loadSchema(Version version, String feedName) {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("schema/"+version.getVersion()+"/"+feedName+".json");
        JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
        return SchemaLoader.load(rawSchema);
    }
}
