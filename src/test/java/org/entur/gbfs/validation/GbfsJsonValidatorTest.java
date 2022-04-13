package org.entur.gbfs.validation;

import org.assertj.core.api.Assert;
import org.entur.gbfs.validation.files.FileValidationResult;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GbfsJsonValidatorTest {
    @Test
    void testEmptyDeliveryMapValidation() {
        GbfsJsonValidator validator = new GbfsJsonValidator(false, true);
        Map<String, InputStream> deliveryMap = new HashMap<>();
        ValidationResult result = validator.validate(deliveryMap);
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV1_0Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator(false, true);

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v1.0/gbfs.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v1.0/system_hours.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("1.0", result);

        Assertions.assertEquals("1.0", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV1_1Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator(false, true);

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v1.1/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v1.1/gbfs_versions.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v1.1/system_hours.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("1.1", result);

        Assertions.assertEquals("1.1", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    private InputStream getFixture(String name) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        return inputStream;
    }

    private void printErrors(String version, ValidationResult result) {
        result.getFiles().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> entry.getValue().getErrorsCount() > 0)
                .forEach(entry -> {
                    System.out.println("Version " + version + " - File: " + entry.getKey());
                    System.out.println(entry.getValue().getError().toJSON().toString(2));
                });
    }
}
