package org.entur.gbfs.validation.versions;

import java.util.Arrays;
import java.util.List;

public class Version2_2 implements Version {
    public static String version = "2.2";

    private static final List<String> feeds = Arrays.asList(
            "gbfs",
            "gbfs_versions",
            "system_information",
            "vehicle_types",
            "station_information",
            "station_status",
            "free_bike_status",
            "system_hours",
            "system_calendar",
            "system_regions",
            "system_pricing_plans",
            "system_alerts",
            "geofencing_zones"
    );

    private final boolean isDocked;
    private final boolean isFreeFloating;

    public Version2_2(boolean isDocked, boolean isFreeFloating) {
        this.isDocked = isDocked;
        this.isFreeFloating = isFreeFloating;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<String> getFeeds() {
        return feeds;
    }

    @Override
    public boolean isFileRequired(String file) {
        switch (file) {
            case "system_information":
                return true;
            case "station_information":
            case "station_status":
                return isDocked;
            case "free_bike_status":
                return isFreeFloating;
            default:
                return false;
        }
    }
}
