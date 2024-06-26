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

import java.util.Map;

/**
 * This record contains the result of validating a set of GBFS files
 * @param summary A summary of the validation
 * @param files A map of files that were validated
 */
public record ValidationResult(
     ValidationSummary summary,
     Map<String, FileValidationResult> files
) implements ValidationResultComponentIdentity<ValidationResult> {
    @Override
    public String toString() {
        return "ValidationResult{" +
                "summary=" + summary +
                ", files=" + files +
                '}';
    }

    @Override
    public boolean sameAs(ValidationResult other) {
        if (other == null) return false;
        if (!summary.sameAs(other.summary())) return false;
        return files.entrySet().stream().allMatch(entry -> other.files.get(entry.getKey()).sameAs(entry.getValue()));
    }
}
