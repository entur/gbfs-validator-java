package org.entur.gbfs.validation.versions;

import java.util.List;

public interface Version {
    String getVersion();
    List<String> getFeeds();
    boolean isFileRequired(String file, boolean isDocked, boolean isFreeFloating);
}
