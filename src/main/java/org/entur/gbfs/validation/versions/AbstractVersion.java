package org.entur.gbfs.validation.versions;

import java.util.List;

public abstract class AbstractVersion implements Version {

    private final String version;
    private final List<String> feeds;
    private final boolean isDocked;
    private final boolean isFreeFloating;

    public static Version createVersion(String version, boolean isDocked, boolean isFreeFloating) {
        switch (version) {
            case "1.0":
                return new Version1_0(isDocked, isFreeFloating);
            case "1.1":
                return new Version1_1(isDocked, isFreeFloating);
            case "2.0":
                return new Version2_0(isDocked, isFreeFloating);
            case "2.1":
                return new Version2_1(isDocked, isFreeFloating);
            case "2.2":
                return new Version2_2(isDocked, isFreeFloating);
            case "2.3":
                return new Version2_3(isDocked, isFreeFloating);
            default:
                throw new UnsupportedOperationException("Version not implemented");
        }
    }

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
