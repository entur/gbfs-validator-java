package org.entur.gbfs.validation.versions;

import java.util.Arrays;
import java.util.List;

public class Version1_0 extends AbstractVersion {
    public static String version = "1.0";

    private static final List<String> feeds = Arrays.asList(
            "gbfs",
            "system_information",
            "station_information",
            "station_status",
            "free_bike_status",
            "system_calendar",
            "system_hours",
            "system_pricing_plans",
            "system_regions"
    );

    protected Version1_0(boolean isDocked, boolean isFreeFloating) {
        super(version, feeds, isDocked, isFreeFloating);
    }
}
