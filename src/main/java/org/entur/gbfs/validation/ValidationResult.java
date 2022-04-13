package org.entur.gbfs.validation;

import org.entur.gbfs.validation.files.FileValidationResult;

import java.util.HashMap;
import java.util.Map;

public class ValidationResult {
    private ValidationSummary summary = new ValidationSummary();
    private Map<String, FileValidationResult> files = new HashMap<>();

    public ValidationSummary getSummary() {
        return summary;
    }

    public void setSummary(ValidationSummary summary) {
        this.summary = summary;
    }

    public Map<String, FileValidationResult> getFiles() {
        return files;
    }

    public void setFiles(Map<String, FileValidationResult> files) {
        this.files = files;
    }
}
