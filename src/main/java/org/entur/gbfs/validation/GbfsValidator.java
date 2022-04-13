package org.entur.gbfs.validation;

import java.io.InputStream;
import java.util.Map;

public interface GbfsValidator {

    public ValidationResult validate(Map<String, InputStream> feedMap);
}
