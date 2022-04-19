package org.entur.gbfs.validation.versions;

import java.util.Arrays;
import java.util.List;

public class Version1_1 extends AbstractVersion {
    public static String version = "1.1";

    private static final List<String> feeds = Arrays.asList(
            "gbfs",
            "gbfs_versions",
            "system_information",
            "station_information",
            "station_status",
            "free_bike_status",
            "system_hours",
            "system_calendar",
            "system_regions",
            "system_pricing_plans",
            "system_alerts"
    );

    protected Version1_1(boolean isDocked, boolean isFreeFloating) {
        super(version, feeds, isDocked, isFreeFloating);
    }
}
