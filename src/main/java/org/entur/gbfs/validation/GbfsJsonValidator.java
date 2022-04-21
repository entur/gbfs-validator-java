package org.entur.gbfs.validation;

import org.entur.gbfs.validation.files.FileValidationResult;
import org.entur.gbfs.validation.files.FileValidator;
import org.entur.gbfs.validation.files.GBFSFeedName;
import org.entur.gbfs.validation.versions.Version;
import org.entur.gbfs.validation.versions.VersionFactory;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GbfsJsonValidator implements GbfsValidator {

    private static final String DEFAULT_VERSION = "2.3";

    @Override
    public ValidationResult validate(Map<String, InputStream> rawFeeds) {
        Map<String, JSONObject> feedMap = parseFeeds(rawFeeds);

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

        return  VersionFactory.createVersion(
                versions.stream().findFirst().orElse(DEFAULT_VERSION)
        );
    }

    private FileValidationResult validateFile(String feedName, JSONObject feed) {

        if (feed == null) {
            FileValidationResult result = new FileValidationResult();
            result.setFile(feedName);
            result.setExists(false);
            return result;
        }

        // Assume no version means version 1.0
        String detectedVersion = "1.0";

        if (feed.has("version")) {
            detectedVersion = feed.getString("version");
        }

        // find correct file validator
        FileValidator fileValidator = FileValidator.getFileValidator(detectedVersion);
        return fileValidator.validate(feedName, feed);
    }

    private Map<String, JSONObject> parseFeeds(Map<String, InputStream> rawFeeds) {
        Map<String, JSONObject> feedMap = new HashMap<>();
        rawFeeds.forEach((name, value) -> feedMap.put(name, parseFeed(value)));
        return feedMap;
    }

    private JSONObject parseFeed(InputStream raw) {
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
