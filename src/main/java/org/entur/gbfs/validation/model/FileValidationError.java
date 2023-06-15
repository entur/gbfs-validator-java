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

public class FileValidationError implements ValidationResultComponentIdentity<FileValidationError> {
    private String schemaPath;
    private String violationPath;
    private String message;

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public String getViolationPath() {
        return violationPath;
    }

    public void setViolationPath(String violationPath) {
        this.violationPath = violationPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "schemaPath='" + schemaPath + '\'' +
                ", violationPath='" + violationPath + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean sameAs(FileValidationError other) {
        if (!Objects.equals(schemaPath, other.schemaPath)) return false;
        if (!Objects.equals(violationPath, other.violationPath))
            return false;
        return Objects.equals(message, other.message);
    }
}
