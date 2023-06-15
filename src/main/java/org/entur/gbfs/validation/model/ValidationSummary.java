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

package org.entur.gbfs.validation.model;

import java.util.Objects;

public class ValidationSummary implements ValidationResultComponentIdentity<ValidationSummary> {
    private String version;
    private long timestamp;
    private int errorsCount;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    @Override
    public String toString() {
        return "ValidationSummary{" +
                "version='" + version + '\'' +
                ", timestamp=" + timestamp +
                ", errorsCount=" + errorsCount +
                '}';
    }

    @Override
    public boolean sameAs(ValidationSummary other) {
        if (errorsCount != other.errorsCount) return false;
        return Objects.equals(version, other.version);
    }
}
