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
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.entur.gbfs.validator.loader.AuthType;
import org.entur.gbfs.validator.loader.Authentication;
import org.entur.gbfs.validator.loader.BasicAuth;
import org.entur.gbfs.validator.loader.BearerTokenAuth;
import org.entur.gbfs.validator.loader.OAuthClientCredentialsGrantAuth;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Loader {
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;

    // Helper method to extract filename from URI
    private String getFileName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return "unknown_file"; // Or handle as an error/default
        }
        return new File(path).getName();
    }

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

    public List<LoadedFile> load(String discoveryURIString) throws IOException {
        return load(discoveryURIString, null);
    }

    public List<LoadedFile> load(String discoveryURIString, Authentication auth) throws IOException {
        URI discoveryURI = URI.create(discoveryURIString);
        LoadedFile discoveryLoadedFile = loadFile(discoveryURI, auth);

        if (discoveryLoadedFile.fileContents() == null) {
            // If discovery file itself failed to load, return it with its system errors
            List<LoadedFile> loadedFiles = new ArrayList<>();
            loadedFiles.add(discoveryLoadedFile);
            return loadedFiles;
        }

        // Read the content of the discovery file for further processing
        ByteArrayOutputStream discoveryFileCopy = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(discoveryLoadedFile.fileContents(), discoveryFileCopy);
        byte[] discoveryFileBytes = discoveryFileCopy.toByteArray();
        // Close the original stream now that we have a copy
        discoveryLoadedFile.fileContents().close();


        JSONObject discoveryFileJson = new JSONObject(new JSONTokener(new ByteArrayInputStream(discoveryFileBytes)));
        String version = discoveryFileJson.getString("version"); // Use getString for mandatory fields

        List<LoadedFile> loadedFiles = new ArrayList<>();
        // Add the successfully loaded discovery file (with its original byte array)
        loadedFiles.add(new LoadedFile(
                discoveryLoadedFile.fileName(),
                discoveryLoadedFile.url(),
                new ByteArrayInputStream(discoveryFileBytes), // use the copied bytes
                discoveryLoadedFile.language(), // language might be null
                discoveryLoadedFile.systemErrors() // should be empty here
        ));


        if (version.matches("^3\\.\\d")) {
            loadedFiles.addAll(getV3Files(discoveryFileJson, loadedFiles.get(0), auth));
        } else {
            loadedFiles.addAll(getPreV3Files(discoveryFileJson, loadedFiles.get(0), auth));
        }

        return loadedFiles;
    }

    private List<LoadedFile> getV3Files(JSONObject discoveryFileJson, LoadedFile gbfsLoadedFile, Authentication auth) {
        List<LoadedFile> loadedFeedFiles = new ArrayList<>();

        // Load feed files in parallel using CompletableFuture
        List<CompletableFuture<LoadedFile>> futures = discoveryFileJson.getJSONObject("data").getJSONArray("feeds").toList().stream()
                .map(feed -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> feedMap = (Map<String, Object>) feed;
                    String url = (String) feedMap.get("url");
                    String name = (String) feedMap.get("name");

                    return CompletableFuture.supplyAsync(() -> {
                        LoadedFile loadedFile = loadFile(URI.create(url), auth);
                        // Ensure name and original url are from the discovery file, not overridden by potential redirects in loadFile
                        return new LoadedFile(name, url, loadedFile.fileContents(), loadedFile.language(), loadedFile.systemErrors());
                    }, executorService);
                })
                .toList();

        // Wait for all futures to complete and collect results
        loadedFeedFiles.addAll(futures.stream()
                .map(CompletableFuture::join)
                .toList());

        return loadedFeedFiles;
    }

    private List<LoadedFile> getPreV3Files(JSONObject discoveryFileJson, LoadedFile gbfsLoadedFile, Authentication auth) {
        List<LoadedFile> loadedFeedFiles = new ArrayList<>();
        List<CompletableFuture<LoadedFile>> futures = new ArrayList<>();

        String discoveryFileUrl = gbfsLoadedFile.url();
        // Add the discovery file itself for each language specified (pre-v3)
        // This part is tricky as the original structure added multiple copies of gbfs.json for each language.
        // For now, we've already added the main gbfsLoadedFile.
        // The feeds listed per language will be loaded below.

        discoveryFileJson.getJSONObject("data")
                .keys()
                .forEachRemaining(languageKey -> {
                    // Create CompletableFutures for each feed file within this language
                    discoveryFileJson.getJSONObject("data").getJSONObject(languageKey).getJSONArray("feeds").toList().forEach(feed -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> feedMap = (Map<String, Object>) feed;
                        String url = (String) feedMap.get("url");
                        String name = (String) feedMap.get("name");

                        futures.add(CompletableFuture.supplyAsync(() -> {
                            LoadedFile loadedFile = loadFile(URI.create(url), auth);
                            // Ensure name, original url, and language are from the discovery file
                            return new LoadedFile(name, url, loadedFile.fileContents(), languageKey, loadedFile.systemErrors());
                        }, executorService));
                    });
                });

        // Wait for all futures to complete and collect results
        loadedFeedFiles.addAll(futures.stream()
                .map(CompletableFuture::join)
                .toList());

        return loadedFeedFiles;
    }

    private LoadedFile loadFile(URI fileURI, Authentication auth) {
        String fileName = getFileName(fileURI);
        String url = fileURI.toString();

        if ("file".equals(fileURI.getScheme())) {
            try {
                InputStream stream = getFileInputStream(fileURI);
                return new LoadedFile(fileName, url, stream, null, new ArrayList<>());
            } catch (FileNotFoundException e) {
                List<SystemError> errors = new ArrayList<>();
                errors.add(new SystemError("FILE_NOT_FOUND", e.getMessage()));
                return new LoadedFile(fileName, url, null, null, errors);
            }
        } else if ("https".equals(fileURI.getScheme()) || "http".equals(fileURI.getScheme())) {
            try {
                InputStream stream = getHTTPInputStream(fileURI, auth);
                return new LoadedFile(fileName, url, stream, null, new ArrayList<>());
            } catch (IOException e) {
                List<SystemError> errors = new ArrayList<>();
                errors.add(new SystemError("CONNECTION_ERROR", e.getMessage()));
                return new LoadedFile(fileName, url, null, null, errors);
            } catch (ParseException e) { // Catch ParseException from getHTTPInputStream
                List<SystemError> errors = new ArrayList<>();
                errors.add(new SystemError("PARSE_ERROR", e.getMessage()));
                return new LoadedFile(fileName, url, null, null, errors);
            }
        }

        List<SystemError> errors = new ArrayList<>();
        errors.add(new SystemError("UNSUPPORTED_SCHEME", "Scheme not supported: " + fileURI.getScheme()));
        return new LoadedFile(fileName, url, null, null, errors);
    }

    private static FileInputStream getFileInputStream(URI fileURI) throws FileNotFoundException {
        return new FileInputStream(new File(fileURI));
    }

    private InputStream getHTTPInputStream(URI fileURI, Authentication auth) throws IOException, ParseException { // Added ParseException to signature
        HttpGet httpGet = new HttpGet(fileURI);
        httpGet.setHeader("Et-Client-Name", "entur-gbfs-validator");

        if (auth != null) {
            if (auth.getAuthType() == AuthType.BASIC) {
                BasicAuth basicAuth = (BasicAuth) auth;
                String authHeader = "Basic " + Base64.getEncoder().encodeToString((basicAuth.getUsername() + ":" + basicAuth.getPassword()).getBytes());
                httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            } else if (auth.getAuthType() == AuthType.BEARER_TOKEN) {
                BearerTokenAuth bearerAuth = (BearerTokenAuth) auth;
                httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerAuth.getToken());
            } else if (auth.getAuthType() == AuthType.OAUTH_CLIENT_CREDENTIALS) {
                OAuthClientCredentialsGrantAuth oauth = (OAuthClientCredentialsGrantAuth) auth;
                try {
                    String token = fetchOAuthToken(oauth.getTokenUrl(), oauth.getClientId(), oauth.getClientSecret());
                    httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                } catch (Exception e) { // Catch specific exceptions if possible
                    // Propagate as IOException or a custom auth exception, or add to SystemError list for the file
                    throw new IOException("OAuth token fetch failed: " + e.getMessage(), e);
                }
            }
        }

        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            // Check if the response code indicates an error (e.g., 4xx or 5xx)
            if (response.getCode() >= 300) {
                // Consume the entity to allow connection reuse, then throw an exception
                EntityUtils.consumeQuietly(response.getEntity());
                throw new IOException("HTTP error fetching file: " + response.getCode() + " " + response.getReasonPhrase());
            }
            String content = EntityUtils.toString(response.getEntity());
            return new ByteArrayInputStream(content.getBytes());
        }
        // Removed the catch for ParseException here, as it will be caught by loadFile
    }

    private String fetchOAuthToken(String tokenUrl, String clientId, String clientSecret) throws IOException, ParseException {
        HttpPost tokenRequest = new HttpPost(tokenUrl);
        tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
        String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
        tokenRequest.setEntity(new StringEntity(body));

        try (CloseableHttpResponse response = httpClient.execute(tokenRequest)) {
            if (response.getCode() >= 300) {
                EntityUtils.consumeQuietly(response.getEntity());
                throw new IOException("OAuth token request failed: " + response.getCode() + " " + response.getReasonPhrase());
            }
            String responseString = EntityUtils.toString(response.getEntity());
            JSONObject jsonResponse = new JSONObject(responseString);
            if (!jsonResponse.has("access_token")) {
                throw new IOException("OAuth token response did not contain access_token");
            }
            return jsonResponse.getString("access_token");
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
