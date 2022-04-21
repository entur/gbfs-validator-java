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

package org.entur.gbfs.validation.files;

public enum GBFSFeedName {
    GBFS("gbfs"),
    GBFSVersions("gbfs_versions"),
    SystemInformation("system_information"),
    VehicleTypes("vehicle_types"),
    StationInformation("station_information"),
    StationStatus("station_status"),
    FreeBikeStatus("free_bike_status"),
    SystemHours("system_hours"),
    SystemAlerts("system_alerts"),
    SystemCalendar("system_calendar"),
    SystemRegions("system_regions"),
    SystemPricingPlans("system_pricing_plans"),
    GeofencingZones("geofencing_zones");

    private String value;

    private GBFSFeedName(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
