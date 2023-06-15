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

package org.entur.gbfs.validation.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class FileValidationResultTest {

    @Test
    void testSameAsSucceeds() {
        Assertions.assertTrue(generateFileValidationResult(
                "message-1",
                "message-2"
        ).sameAs(generateFileValidationResult(
                "message-1",
                "message-2"
        )));
    }

    @Test
    void testSameAsFails() {
        Assertions.assertFalse(generateFileValidationResult(
                "message-1",
                "message-2"
        ).sameAs(generateFileValidationResult(
                "message-1",
                "message-3"
        )));
    }

    private FileValidationResult generateFileValidationResult(String message1, String message2) {
        var fileValidationResult = new FileValidationResult();

        fileValidationResult.setFile("gbfs");
        fileValidationResult.setExists(true);
        fileValidationResult.setRequired(true);
        fileValidationResult.setErrorsCount(2);
        fileValidationResult.setErrors(List.of(
                generateFileValidationError(message1),
                generateFileValidationError(message2)
        ));

        return fileValidationResult;
    }

    private FileValidationError generateFileValidationError(String message) {
        var fileValidationError = new FileValidationError();

        fileValidationError.setMessage(message);
        fileValidationError.setSchemaPath("schema/path");
        fileValidationError.setViolationPath("violation/path");

        return fileValidationError;
    }
}
