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

package org.entur.gbfs.validation.validator.versions;

import java.util.Arrays;
import java.util.List;

public class Version10 extends AbstractVersion {

  public static final String VERSION = "1.0";

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

  protected Version10() {
    super(VERSION, feeds);
  }
}
