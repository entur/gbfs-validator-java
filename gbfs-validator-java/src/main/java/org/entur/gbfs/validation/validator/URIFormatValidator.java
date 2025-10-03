/*
 *
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.gbfs.validation.validator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import org.everit.json.schema.FormatValidator;

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
