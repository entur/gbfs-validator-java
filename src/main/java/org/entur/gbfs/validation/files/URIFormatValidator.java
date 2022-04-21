package org.entur.gbfs.validation.files;

import org.everit.json.schema.FormatValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class URIFormatValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String subject) {
        try {
            new URI(subject);
            return Optional.empty();
        } catch (URISyntaxException e) {
            if (e.getReason().equalsIgnoreCase("expected authority")) {
                return Optional.empty();
            }
        }
        return Optional.of("Invalid URI");
    }

    @Override
    public String formatName() {
        return "uri";
    }
}
