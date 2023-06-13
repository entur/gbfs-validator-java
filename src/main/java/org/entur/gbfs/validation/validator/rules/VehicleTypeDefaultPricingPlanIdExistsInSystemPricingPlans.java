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

package org.entur.gbfs.validation.validator.rules;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * A vehicle's default_pricing_plan_id must exist in the system's system_pricing_plan file
 */
public class VehicleTypeDefaultPricingPlanIdExistsInSystemPricingPlans implements CustomRuleSchemaPatcher {

    public static final String DEFAULT_PRICING_PLAN_ID_SCHEMA_PATH = "$.properties.data.properties.vehicle_types.items.properties.default_pricing_plan_id";

    /**
     * Adds an enum to vehicle_type's default_pricing_plan_id schema with the plan ids from the system_pricing_plan feed
     */
    @Override
    public DocumentContext addRule(DocumentContext rawSchemaDocumentContext, Map<String, JSONObject> feeds) {
        JSONObject pricingPlansFeed = feeds.get("system_pricing_plans");
        JSONArray pricingPlanIds = JsonPath.parse(pricingPlansFeed).read("$.data.plans[*].plan_id");
        JSONObject defaultPricingPlanIdSchema = rawSchemaDocumentContext.read(DEFAULT_PRICING_PLAN_ID_SCHEMA_PATH);
        defaultPricingPlanIdSchema.put("enum", pricingPlanIds);
        return rawSchemaDocumentContext.set(DEFAULT_PRICING_PLAN_ID_SCHEMA_PATH, defaultPricingPlanIdSchema);
    }
}
