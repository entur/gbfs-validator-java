package org.entur.gbfs.validation.files;

import org.entur.gbfs.validation.versions.Version;
import org.entur.gbfs.validation.versions.VersionFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class FileValidator {
    private final Version version;
    private final Map<String, Schema> schemas;

    public static FileValidator getFileValidator(
            String detectedVersion,
            boolean isDocked,
            boolean isFreeFloating
    ) {
        return new FileValidator(VersionFactory.createVersion(detectedVersion, isDocked, isFreeFloating));
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
            fileValidationResult.setError(validationException);
            fileValidationResult.setErrorsCount(validationException.getViolationCount());
        }

        return fileValidationResult;
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
            return SchemaLoader.load(rawSchema);
        } catch (Exception e) {
            System.out.println("Caught exception loading schema for " + feedName + " and version " + version);
            throw e;
        }

    }
}
