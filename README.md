[![CircleCI](https://circleci.com/gh/entur/gbfs-validator-java/tree/master.svg?style=svg)](https://circleci.com/gh/entur/gbfs-validator-java/tree/master)

# gbfs-validator-java

Validate GBFS feeds. Intended as Java native alternative to https://github.com/MobilityData/gbfs-validator.

Uses the official json schema to validate files.

## Additional validation rules

The interface `CustomRuleSchemaPatcher` enables adding additional rules dynamically by schema patching:

    JSONObject addRule(JSONObject rawSchema, Map<String, JSONObject> feeds);

The raw schema along with a map of the data feeds is passed to this method. The patched schema should be returned.

List of additional rules:

* `NoInvalidReferenceToPricingPlansInVehicleTypes`
* `NoInvalidReferenceToVehicleTypesInStationStatus`
* `NoMissingVehicleTypesAvailableWhenVehicleTypesExists`
* `NoMissingVehicleTypeIdInVehicleStatusWhenVehicleTypesExist`
* `NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles`

Planned rules:

* if free_bike_status / vehicle_status or station_information has rental uris then system_information must have store_uri in rental_apps (ios and / or android)

## Non-schema rules:

Some rules can't be validated with json schema:

Existing rules: 

* All version of gbfs require the system_information endpoint.
* In addition, gbfs endpoint is required as of v2.0.

Planned rules:

* Either station_information or station_status is required if the other is present
  * Can this be checked by cross-checking ids between files - using schema patching?
* vehicle_types is required if vehicle types are referenced in other files (already covered?)
* system_pricing_plans is required if pricing plans are referenced in other files

