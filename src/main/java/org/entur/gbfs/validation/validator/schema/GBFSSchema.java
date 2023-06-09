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

package org.entur.gbfs.validation.validator.schema;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.entur.gbfs.validation.validator.FileValidator;
import org.entur.gbfs.validation.validator.URIFormatValidator;
import org.entur.gbfs.validation.validator.rules.CustomRuleSchemaPatcher;
import org.entur.gbfs.validation.validator.schema.v2_3.VehicleTypesSchemaV2_3;
import org.entur.gbfs.validation.versions.Version;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GBFSSchema {
    private final Logger logger = LoggerFactory.getLogger(GBFSSchema.class);

    private JSONObject rawSchema = null;

    private final Version version;
    private final String feedName;

    protected GBFSSchema(Version version, String feedName) {
        this.version = version;
        this.feedName = feedName;
        configureJsonPath();
    }

    private void configureJsonPath() {
        Configuration.setDefaults(new Configuration.Defaults() {
            JsonProvider jsonProvider = new JsonOrgJsonProvider();
            MappingProvider mappingProvider = new JsonOrgMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }
        });
    }

    public static GBFSSchema getGBFSchema(Version version, String feedName) {
        if (feedName.equals("vehicle_types") && version.getVersion().equals("2.3")) {
            return new VehicleTypesSchemaV2_3(version, feedName);
        }
        return new GBFSSchema(version, feedName);
    }

    public Version getVersion() {
        return version;
    }

    public String getFeedName() {
        return feedName;
    }

    public Schema getSchema(String feedName, Map<String, JSONObject> feedMap) {
        if (rawSchema == null) {
            rawSchema = loadRawSchema(version.getVersion(), feedName);
        }
        return loadSchema(applyCustomRules(rawSchema, feedMap));
    }

    private JSONObject applyCustomRules(JSONObject rawSchema, Map<String, JSONObject> feedMap) {
        List<CustomRuleSchemaPatcher> customRules = getCustomRules();

        // Risky use of reduce?
        return customRules.stream().reduce(rawSchema, (schema, patcher) -> applyRule(schema, patcher, feedMap), (a, b) -> a);
    }

    protected JSONObject applyRule(JSONObject schema, CustomRuleSchemaPatcher patcher, Map<String, JSONObject> feedMap) {
        return patcher.addRule(schema, feedMap);
    }

    protected List<CustomRuleSchemaPatcher> getCustomRules() {
        return List.of();
    }

    protected JSONObject loadRawSchema(String version, String feedName) {
        InputStream inputStream = FileValidator.class.getClassLoader().getResourceAsStream("schema/v"+version+"/"+feedName+".json");

        if (inputStream == null) {
            logger.warn("Unable to load schema version={} feedName={}", version, feedName);
            return null;
        }

        return new JSONObject(new JSONTokener(inputStream));
    }

    protected org.everit.json.schema.Schema loadSchema(JSONObject rawSchema) {
        SchemaLoader schemaLoader = SchemaLoader.builder()
                .enableOverrideOfBuiltInFormatValidators()
                .addFormatValidator(new URIFormatValidator())
                .schemaJson(rawSchema)
                .build();

        return schemaLoader.load().build();
    }
}