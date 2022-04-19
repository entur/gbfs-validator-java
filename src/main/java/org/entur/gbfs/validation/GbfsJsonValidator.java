package org.entur.gbfs.validation;

import org.entur.gbfs.validation.files.FileValidationResult;
import org.entur.gbfs.validation.files.FileValidator;
import org.entur.gbfs.validation.files.GBFSFeedName;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
            fileValidations.put(key, validateFile(key, feedMap.get(key), summary));
        });

        summary.setErrorsCount(
                fileValidations.values().stream()
                        .filter(Objects::nonNull)
                        .map(FileValidationResult::getErrorsCount)
                        .reduce(Integer::sum).orElse(0));

        result.setSummary(summary);
        result.setFiles(fileValidations);

        return result;
    }

    private FileValidationResult validateFile(String feedName, InputStream value, ValidationSummary summary) {

        if (value == null) {
            // TODO: deal with missing files
            return null;
        }

        JSONObject feed = extractJSONObject(value);

        // Assume no version means version 1.0
        String detectedVersion = "1.0";

        if (feed.has("version")) {
            detectedVersion = feed.getString("version");
        }

        if (summary.getVersion() == null) {
            summary.setVersion(detectedVersion);
        } else {
            // TODO: multiple versions is error or warning?
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
