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

package org.entur.gbfs.validation.validator;

import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class GbfsJsonValidatorTest {
    @Test
    void testEmptyDeliveryMapValidation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();
        Map<String, InputStream> deliveryMap = new HashMap<>();
        ValidationResult result = validator.validate(deliveryMap);
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV1_0Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

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
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v1.1/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v1.1/gbfs_versions.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v1.1/system_hours.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("1.1", result);

        Assertions.assertEquals("1.1", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV2_0Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.0/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v2.0/gbfs_versions.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v2.0/system_hours.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("2.0", result);

        Assertions.assertEquals("2.0", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV2_1Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.1/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v2.1/gbfs_versions.json"));
        deliveryMap.put("system_information", getFixture("fixtures/v2.1/system_information.json"));
        deliveryMap.put("vehicle_types", getFixture("fixtures/v2.1/vehicle_types.json"));
        deliveryMap.put("station_information", getFixture("fixtures/v2.1/station_information.json"));
        deliveryMap.put("station_status", getFixture("fixtures/v2.1/station_status.json"));
        deliveryMap.put("free_bike_status", getFixture("fixtures/v2.1/free_bike_status.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v2.1/system_hours.json"));
        deliveryMap.put("system_calendar", getFixture("fixtures/v2.1/system_calendar.json"));
        deliveryMap.put("system_regions", getFixture("fixtures/v2.1/system_regions.json"));
        deliveryMap.put("system_alerts", getFixture("fixtures/v2.1/system_alerts.json"));
        deliveryMap.put("geofencing_zones", getFixture("fixtures/v2.1/geofencing_zones.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("2.1", result);

        Assertions.assertEquals("2.1", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV2_2Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.2/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v2.2/gbfs_versions.json"));
        deliveryMap.put("system_information", getFixture("fixtures/v2.2/system_information.json"));
        deliveryMap.put("vehicle_types", getFixture("fixtures/v2.2/vehicle_types.json"));
        deliveryMap.put("station_information", getFixture("fixtures/v2.2/station_information.json"));
        deliveryMap.put("station_status", getFixture("fixtures/v2.2/station_status.json"));
        deliveryMap.put("free_bike_status", getFixture("fixtures/v2.2/free_bike_status.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v2.2/system_hours.json"));
        deliveryMap.put("system_calendar", getFixture("fixtures/v2.2/system_calendar.json"));
        deliveryMap.put("system_regions", getFixture("fixtures/v2.2/system_regions.json"));
        deliveryMap.put("system_pricing_plans", getFixture("fixtures/v2.2/system_pricing_plans.json"));
        deliveryMap.put("system_alerts", getFixture("fixtures/v2.2/system_alerts.json"));
        deliveryMap.put("geofencing_zones", getFixture("fixtures/v2.2/geofencing_zones.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("2.2", result);

        Assertions.assertEquals("2.2", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV2_3Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.3/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v2.3/gbfs_versions.json"));
        deliveryMap.put("system_information", getFixture("fixtures/v2.3/system_information.json"));
        deliveryMap.put("vehicle_types", getFixture("fixtures/v2.3/vehicle_types.json"));
        deliveryMap.put("station_information", getFixture("fixtures/v2.3/station_information.json"));
        deliveryMap.put("station_status", getFixture("fixtures/v2.3/station_status.json"));
        deliveryMap.put("free_bike_status", getFixture("fixtures/v2.3/free_bike_status.json"));
        deliveryMap.put("system_hours", getFixture("fixtures/v2.3/system_hours.json"));
        deliveryMap.put("system_calendar", getFixture("fixtures/v2.3/system_calendar.json"));
        deliveryMap.put("system_regions", getFixture("fixtures/v2.3/system_regions.json"));
        deliveryMap.put("system_pricing_plans", getFixture("fixtures/v2.3/system_pricing_plans.json"));
        deliveryMap.put("system_alerts", getFixture("fixtures/v2.3/system_alerts.json"));
        deliveryMap.put("geofencing_zones", getFixture("fixtures/v2.3/geofencing_zones.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("2.3", result);

        Assertions.assertEquals("2.3", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testSuccessfulV3_0_RCValidation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v3.0-RC/gbfs.json"));
        deliveryMap.put("gbfs_versions", getFixture("fixtures/v3.0-RC/gbfs_versions.json"));
        deliveryMap.put("system_information", getFixture("fixtures/v3.0-RC/system_information.json"));
        deliveryMap.put("vehicle_types", getFixture("fixtures/v3.0-RC/vehicle_types.json"));
        deliveryMap.put("station_information", getFixture("fixtures/v3.0-RC/station_information.json"));
        deliveryMap.put("station_status", getFixture("fixtures/v3.0-RC/station_status.json"));
        deliveryMap.put("vehicle_status", getFixture("fixtures/v3.0-RC/vehicle_status.json"));
        deliveryMap.put("manifest", getFixture("fixtures/v3.0-RC/manifest.json"));
        deliveryMap.put("system_regions", getFixture("fixtures/v3.0-RC/system_regions.json"));
        deliveryMap.put("system_pricing_plans", getFixture("fixtures/v3.0-RC/system_pricing_plans.json"));
        deliveryMap.put("system_alerts", getFixture("fixtures/v3.0-RC/system_alerts.json"));
        deliveryMap.put("geofencing_zones", getFixture("fixtures/v3.0-RC/geofencing_zones.json"));

        ValidationResult result = validator.validate(deliveryMap);

        printErrors("3.0-RC", result);

        Assertions.assertEquals("3.0-RC", result.getSummary().getVersion());
        Assertions.assertEquals(0, result.getSummary().getErrorsCount());
    }

    @Test
    void testFailed2_3Validation() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        InputStream freeBikeStatus = getFixture("fixtures/v2.3/free_bike_status_with_error.json");
        FileValidationResult result = validator.validateFile("free_bike_status", freeBikeStatus);

        Assertions.assertEquals("2.3", result.getVersion());
        Assertions.assertEquals(3, result.getErrorsCount());
    }

    @Test
    void testMissingRequiredFile() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.2/gbfs.json"));

        ValidationResult result = validator.validate(deliveryMap);

        Assertions.assertTrue(result.getFiles().get("system_information").isRequired());
        Assertions.assertFalse(result.getFiles().get("system_information").isExists());
    }

    @Test
    void testMissingDiscoveryFileAsOf20() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("system_hours", getFixture("fixtures/v2.0/system_hours.json"));

        ValidationResult result = validator.validate(deliveryMap);

        Assertions.assertTrue(result.getFiles().get("gbfs").isRequired());
        Assertions.assertFalse(result.getFiles().get("gbfs").isExists());
    }

    @Test
    void testMissingNotRequiredFile() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.2/gbfs.json"));

        ValidationResult result = validator.validate(deliveryMap);

        Assertions.assertFalse(result.getFiles().get("vehicle_types").isRequired());
        Assertions.assertFalse(result.getFiles().get("vehicle_types").isExists());
    }

    @Test
    void testSystemInformationTimeZones() {
        GbfsJsonValidator validator = new GbfsJsonValidator();

        Map<String, InputStream> deliveryMap = new HashMap<>();
        deliveryMap.put("gbfs", getFixture("fixtures/v2.2/system_information.json"));

        ValidationResult result = validator.validate(deliveryMap);
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
                    entry.getValue().getErrors().forEach(System.out::println);
                });
    }
}
