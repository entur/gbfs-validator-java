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

package org.entur.gbfs.validator.api.handler;

import org.entur.gbfs.validator.loader.Loader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoaderConfiguration {

    @Bean
    public Loader loader(LoaderProperties properties) {
        return new Loader(
            properties.getHttp().getMaxTotalConnections(),
            properties.getHttp().getMaxConnectionsPerRoute(),
            properties.getHttp().getConnectTimeoutSeconds(),
            properties.getHttp().getResponseTimeoutSeconds(),
            properties.getThreadPool().getSize(),
            properties.getHttp().getHeaders()
        );
    }
}
