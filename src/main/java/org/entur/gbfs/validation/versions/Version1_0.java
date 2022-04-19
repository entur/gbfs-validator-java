package org.entur.gbfs.validation.versions;

import java.util.Arrays;
import java.util.List;

public class Version1_0 implements Version {
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

    private final boolean isDocked;
    private final boolean isFreeFloating;

    public Version1_0(boolean isDocked, boolean isFreeFloating) {
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
