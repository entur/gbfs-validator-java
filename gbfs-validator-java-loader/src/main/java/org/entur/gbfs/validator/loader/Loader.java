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

package org.entur.gbfs.validator.loader;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Loader {

    public List<LoadedFile> load(String discoveryURI) throws IOException {
        InputStream discoveryFileStream = loadFile(URI.create(discoveryURI));

        ByteArrayOutputStream discoveryFileCopy = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(discoveryFileStream, discoveryFileCopy);
        byte[] discoveryFileBytes = discoveryFileCopy.toByteArray();

        JSONObject discoveryFile = new JSONObject(new JSONTokener(new ByteArrayInputStream(discoveryFileBytes)));

        String version = (String) discoveryFile.get("version");

        List<LoadedFile> loadedFiles = new ArrayList<>();



        if (version.matches("^3\\.\\d")) {
            loadedFiles.addAll(getV3Files(discoveryFile, discoveryURI, discoveryFileBytes));
        } else {
            loadedFiles.addAll(getPreV3Files(discoveryFile, discoveryURI, discoveryFileBytes));
        }

        return loadedFiles;
    }

    private List<LoadedFile> getV3Files(JSONObject discoveryFile, String discoveryFileUrl, byte[] discoveryFileBytes) {
        List<LoadedFile> loadedFiles = new ArrayList<>();
        loadedFiles.add(
                new LoadedFile(
                        "gbfs",
                        discoveryFileUrl,
                        new ByteArrayInputStream(discoveryFileBytes)
                ));

        loadedFiles.addAll(
                discoveryFile.getJSONObject("data").getJSONArray("feeds").toList().stream().map(feed -> {
            var feedObj = (HashMap) feed;
            var url = (String) feedObj.get("url");
            var file = loadFile(URI.create(url));
            return new LoadedFile(
                    (String) feedObj.get("name"),
                    url,
                    file
            );
        }).toList());

        return loadedFiles;
    }

    private List<LoadedFile> getPreV3Files(JSONObject discoveryFile, String discoveryFileUrl, byte[] discoveryFileBytes) {
        List<LoadedFile> result = new ArrayList<>();
        discoveryFile.getJSONObject("data")
                .keys()
                .forEachRemaining(key -> {
                    result.add(
                            new LoadedFile(
                                    "gbfs",
                                    discoveryFileUrl,
                                    new ByteArrayInputStream(discoveryFileBytes),
                                    key
                            )
                    );
                    discoveryFile.getJSONObject("data").getJSONObject(key).getJSONArray("feeds").toList().forEach(feed -> {
                        var feedObj = (HashMap) feed;
                        var url = (String) feedObj.get("url");
                        var file = loadFile(URI.create(url));
                        result.add(
                                new LoadedFile(
                                        (String) feedObj.get("name"),
                                        url,
                                        file,
                                        key
                                )
                        );
                    });
                });

        return result;
    }

    private InputStream loadFile(URI fileURI) {
        if (fileURI.getScheme().equals("file")) {
            try {
                return getFileInputStream(fileURI);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else if (fileURI.getScheme().equals("https") || fileURI.getScheme().equals("http")) {
            try {
                return getHTTPInputStream(fileURI);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Scheme not supported");
    }

    private static FileInputStream getFileInputStream(URI fileURI) throws FileNotFoundException {
        return new FileInputStream(new File(fileURI));
    }

    private InputStream getHTTPInputStream(URI fileURI) throws IOException {
        URL url = fileURI.toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty("Et-Client-Name", "entur-gbfs-validator");
        con.connect();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        return new ByteArrayInputStream(content.toString().getBytes());
    }
}
