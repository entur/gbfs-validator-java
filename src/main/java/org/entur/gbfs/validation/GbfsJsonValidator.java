package org.entur.gbfs.validation;

import org.entur.gbfs.validation.files.FileValidationResult;
import org.entur.gbfs.validation.files.FileValidator;
import org.entur.gbfs.validation.files.GBFSFeedName;
import org.entur.gbfs.validation.versions.AbstractVersion;
import org.entur.gbfs.validation.versions.Version;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GbfsJsonValidator implements GbfsValidator {

    private boolean isDocked;
    private boolean isFreeFloating;

    public GbfsJsonValidator(boolean isDocked, boolean isFreeFloating) {
        this.isDocked = isDocked;
        this.isFreeFloating = isFreeFloating;
    }

    @Override
    public ValidationResult validate(Map<String, InputStream> feedMap) {
        ValidationResult result = new ValidationResult();
        ValidationSummary summary = new ValidationSummary();
        Map<String, FileValidationResult> fileValidations = new HashMap<>();

        Arrays.stream(GBFSFeedName.values()).forEach(gbfsFeedName -> {
            String key = gbfsFeedName.toString();
            fileValidations.put(key, validateFile(key, feedMap.get(key)));
        });

        Version version = findVersion(fileValidations);

        handleMissingFiles(fileValidations, version);

        summary.setVersion(version.getVersion());
        summary.setErrorsCount(
                fileValidations.values().stream()
                        .filter(Objects::nonNull)
                        .map(FileValidationResult::getErrorsCount)
                        .reduce(Integer::sum).orElse(0));
        result.setSummary(summary);
        result.setFiles(fileValidations);

        return result;
    }

    private void handleMissingFiles(Map<String, FileValidationResult> fileValidations, Version version) {
        fileValidations.values().stream()
                        .filter(fvr -> !fvr.isExists())
                                .forEach(fvr -> {
                                    fvr.setVersion(version.getVersion());
                                    fvr.setRequired(version.isFileRequired(fvr.getFile()));
                                });
    }

    private Version findVersion(Map<String, FileValidationResult> fileValidations) {
        Set<String> versions = fileValidations.values().stream()
            .filter(Objects::nonNull)
            .map(FileValidationResult::getVersion)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (versions.size() > 1) {
            // TODO warn or error on multiple versions?
        }

        return  AbstractVersion.createVersion(
                versions.stream().findFirst().get(),
                isDocked,
                isFreeFloating
        );
    }

    private FileValidationResult validateFile(String feedName, InputStream value) {

        if (value == null) {
            FileValidationResult result = new FileValidationResult();
            result.setFile(feedName);
            result.setExists(false);
            return result;
        }

        JSONObject feed = extractJSONObject(value);

        // Assume no version means version 1.0
        String detectedVersion = "1.0";

        if (feed.has("version")) {
            detectedVersion = feed.getString("version");
        }

        // find correct file validator
        FileValidator fileValidator = FileValidator.getFileValidator(detectedVersion, isFreeFloating, isDocked);
        return fileValidator.validate(feedName, feed);
    }

    private JSONObject extractJSONObject(InputStream raw) {
        String asString = getFeedAsString(raw);
        return new JSONObject(asString);
    }

    private String getFeedAsString(InputStream rawFeed) {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(rawFeed))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            return null;
        }
        return stringBuilder.toString();
    }
}
