package org.entur.gbfs.validation.versions;

import java.util.List;

public abstract class AbstractVersion implements Version {

    private final String version;
    private final List<String> feeds;
    private final boolean isDocked;
    private final boolean isFreeFloating;

    protected AbstractVersion(String version, List<String> feeds, boolean isDocked, boolean isFreeFloating) {
        this.version = version;
        this.feeds = feeds;
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
                return isDocked();
            case "free_bike_status":
                return isFreeFloating();
            default:
                return false;
        }
    }

    protected boolean isDocked() {
        return isDocked;
    }

    protected boolean isFreeFloating() {
        return isFreeFloating;
    }
}
