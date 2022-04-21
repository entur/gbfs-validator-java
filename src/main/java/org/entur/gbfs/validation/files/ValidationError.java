package org.entur.gbfs.validation.files;

public class ValidationError {
    private String schemaPath;
    private String violationPath;
    private String message;

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public String getViolationPath() {
        return violationPath;
    }

    public void setViolationPath(String violationPath) {
        this.violationPath = violationPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "schemaPath='" + schemaPath + '\'' +
                ", violationPath='" + violationPath + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
