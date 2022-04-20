package org.entur.gbfs.validation.versions;

import java.util.List;

public abstract class AbstractVersion implements Version {

    private final String version;
    private final List<String> feeds;

    protected AbstractVersion(String version, List<String> feeds) {
        this.version = version;
        this.feeds = feeds;
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
    public boolean isFileRequired(String file, boolean isDocked, boolean isFreeFloating) {
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
