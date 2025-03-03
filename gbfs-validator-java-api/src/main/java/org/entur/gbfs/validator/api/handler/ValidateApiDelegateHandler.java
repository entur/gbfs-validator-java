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

import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validator.api.gen.ValidateOption1ApiDelegate;
import org.entur.gbfs.validator.api.model.FileError;
import org.entur.gbfs.validator.api.model.FileLangOption1;
import org.entur.gbfs.validator.api.model.FileOption1;
import org.entur.gbfs.validator.api.model.ValidateOption1PostRequest;
import org.entur.gbfs.validator.api.model.ValidationResultOption1;
import org.entur.gbfs.validator.api.model.ValidationResultOption1Summary;
import org.entur.gbfs.validator.api.model.ValidationResultOption1SummaryFilesInner;
import org.entur.gbfs.validator.loader.Loader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ValidateApiDelegateHandler implements ValidateOption1ApiDelegate {

    @Override
    public ResponseEntity<ValidationResultOption1> validateOption1Post(ValidateOption1PostRequest validateOption1PostRequest) {
        Loader loader = new Loader();
        Map<String, InputStream> fileMap = null;
        try {
            fileMap = loader.load(validateOption1PostRequest.getFeedUrl());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
        return ResponseEntity.ok(
                mapValidationResult(validator.validate(fileMap))
        );
    }

    private ValidationResultOption1 mapValidationResult(ValidationResult validationResult) {
        ValidationResultOption1Summary validationResultOption1Summary = new ValidationResultOption1Summary();
        validationResultOption1Summary.setValidatorVersion("2.0.30-SNAPSHOT"); // TODO inject this value
        validationResultOption1Summary.setGbfsVersion(validationResult.summary().version());
        validationResultOption1Summary.setFiles(mapFiles(validationResult.files()));
        ValidationResultOption1 validationResultOption1 = new ValidationResultOption1();
        validationResultOption1.setSummary(validationResultOption1Summary);
        return validationResultOption1;
    }

    private List<ValidationResultOption1SummaryFilesInner> mapFiles(Map<String, FileValidationResult> files) {
        List<ValidationResultOption1SummaryFilesInner> summaryFiles = new ArrayList<>();
        files.entrySet().stream().forEach(entry -> {
            String fileName = entry.getKey();
            FileValidationResult fileValidationResult = entry.getValue();

            FileLangOption1 filesInner = new FileLangOption1();
            filesInner.setName(fileName);
            filesInner.setExists(fileValidationResult.exists());
            filesInner.setRequired(fileValidationResult.required());
            //filesInner.setRecommended(); // TODO not available
            filesInner.setSchema(fileValidationResult.schema());
            filesInner.setVersion(fileValidationResult.version());

            FileOption1 file  = new FileOption1();
            file.exists(fileValidationResult.exists());
            file.errors(
                    mapFileErrors(fileValidationResult.errors())
            );
            file.fileContent(fileValidationResult.fileContents());


            filesInner.setFiles(
                    List.of(file)
            );
            summaryFiles.add(filesInner);
        });
        return summaryFiles;
    }

    private List<FileError> mapFileErrors(List<FileValidationError> errors) {
        return errors.stream().map(error -> {
            var mapped = new FileError();
            mapped.setMessage(error.message());
            mapped.setInstancePath(error.violationPath());
            mapped.setSchemaPath(error.schemaPath());
            //mapped.setParams(error.); // TODO no source?
            //mapped.setKeyword(error.); // TODO no source?
            return mapped;
        }).toList();
    }
}
