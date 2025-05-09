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

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Loader {
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;

    public Loader() {
        // Create connection pool manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // Set the maximum number of total connections
        // TODO configurable max total
        connectionManager.setMaxTotal(50);
        // Set the maximum number of connections per route
        // TODO configurable max per route
        connectionManager.setDefaultMaxPerRoute(20);

        // Configure request timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                // TODO configurable timeouts
                .setConnectTimeout(Timeout.of(5, TimeUnit.SECONDS))
                .setResponseTimeout(Timeout.of(5, TimeUnit.SECONDS))
                .build();

        // Build the HttpClient with connection pooling
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // Create a thread pool for parallel execution
        // TODO configurable pool size
        executorService = Executors.newFixedThreadPool(20);
    }

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

        // Load files in parallel using CompletableFuture
        List<CompletableFuture<LoadedFile>> futures = discoveryFile.getJSONObject("data").getJSONArray("feeds").toList().stream()
                .map(feed -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> feedMap = (Map<String, Object>) feed;
                    String url = (String) feedMap.get("url");
                    String name = (String) feedMap.get("name");

                    // Create a CompletableFuture for each file to load
                    return CompletableFuture.supplyAsync(() -> {
                        var file = loadFile(URI.create(url));
                        return new LoadedFile(name, url, file);
                    }, executorService);
                })
                .toList();

        // Wait for all futures to complete and collect results
        loadedFiles.addAll(futures.stream()
                .map(CompletableFuture::join)
                .toList());

        return loadedFiles;
    }

    private List<LoadedFile> getPreV3Files(JSONObject discoveryFile, String discoveryFileUrl, byte[] discoveryFileBytes) {
        List<LoadedFile> result = new ArrayList<>();
        List<CompletableFuture<LoadedFile>> futures = new ArrayList<>();

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

                    // Create CompletableFutures for each feed file
                    discoveryFile.getJSONObject("data").getJSONObject(key).getJSONArray("feeds").toList().forEach(feed -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> feedMap = (Map<String, Object>) feed;
                        String url = (String) feedMap.get("url");
                        String name = (String) feedMap.get("name");

                        // Create a CompletableFuture for each file to load
                        futures.add(CompletableFuture.supplyAsync(() -> {
                            var file = loadFile(URI.create(url));
                            return new LoadedFile(name, url, file, key);
                        }, executorService));
                    });
                });

        // Wait for all futures to complete and collect results
        result.addAll(futures.stream()
                .map(CompletableFuture::join)
                .toList());

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
        HttpGet httpGet = new HttpGet(fileURI);

        // TODO configurable headers
        httpGet.setHeader("Et-Client-Name", "entur-gbfs-validator");

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            String content = EntityUtils.toString(response.getEntity());
            return new ByteArrayInputStream(content.getBytes());
        } catch (ParseException e) {

            // Todo handle parse exception
            throw new RuntimeException(e);
        }
    }

    // Close the connection pool when the application shuts down
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }

        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // Wait for tasks to complete
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
