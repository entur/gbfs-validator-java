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
