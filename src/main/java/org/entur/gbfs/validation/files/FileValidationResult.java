package org.entur.gbfs.validation.files;

import org.everit.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class FileValidationResult {
    private String file;
    private boolean required;
    private boolean exists;
    private int errorsCount;
    private String schema;
    private String version;
    private ValidationException error;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ValidationException getError() {
        return error;
    }

    public void setError(ValidationException error) {
        this.error = error;
    }
}
