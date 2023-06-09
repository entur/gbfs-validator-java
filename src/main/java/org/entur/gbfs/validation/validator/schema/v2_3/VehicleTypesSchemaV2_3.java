/*
 *
 *  *
 *  *
 *  *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  *  * You may not use this work except in compliance with the Licence.
 *  *  * You may obtain a copy of the Licence at:
 *  *  *
 *  *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the Licence for the specific language governing permissions and
 *  *  * limitations under the Licence.
 *  *
 *
 */

package org.entur.gbfs.validation.validator.schema.v2_3;

import com.jayway.jsonpath.JsonPath;
import org.entur.gbfs.validation.validator.schema.GBFSSchema;
import org.entur.gbfs.validation.versions.Version;
import org.everit.json.schema.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class VehicleTypesSchemaV2_3 extends GBFSSchema {
    public VehicleTypesSchemaV2_3(Version version, String feedName) {
        super(version, feedName);
    }

    @Override
    protected JSONObject injectCustomRules(JSONObject rawSchema, Map<String, JSONObject> feedMap) {
        JSONObject schema = rawSchema;
        if (feedMap.containsKey("system_pricing_plans")) {
            schema = addDefaultPricingPlanSchema(rawSchema, feedMap.get("system_pricing_plans"));
        }

        return schema;
    }

    /**
     * Adds an enum to vehicle_type's default_pricing_plan_id schema with the plan ids from the system_pricing_plan feed
     */
    private JSONObject addDefaultPricingPlanSchema(JSONObject rawSchema, JSONObject pricingPlansFeed) {
        JSONArray pricingPlanIds = JsonPath.parse(pricingPlansFeed).read("$.data.plans[*].plan_id");
        JSONObject defaultPricingPlanIdSchema = JsonPath.parse(rawSchema).read("$.properties.data.properties.vehicle_types.items.properties.default_pricing_plan_id");
        defaultPricingPlanIdSchema.put("enum", pricingPlanIds);
        return JsonPath.parse(rawSchema).set("$.properties.data.properties.vehicle_types.items.properties.default_pricing_plan_id", defaultPricingPlanIdSchema).json();
    }

}
