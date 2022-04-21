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
    public boolean isFileRequired(String file) {
        if ("system_information".equals(file)) {
            return true;
        }
        return false;
    }
}
