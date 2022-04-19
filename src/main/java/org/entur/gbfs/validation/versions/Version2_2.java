package org.entur.gbfs.validation.versions;

import java.util.Arrays;
import java.util.List;

public class Version2_2 extends AbstractVersion {
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

    protected Version2_2(boolean isDocked, boolean isFreeFloating) {
        super(version, feeds, isDocked, isFreeFloating);
    }
}
