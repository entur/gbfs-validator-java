package org.entur.gbfs.validator.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoaderTest {

    private Loader loader;

    @BeforeEach
    void setUp() {
        loader = new Loader();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void testLoadFileNotFound(@TempDir Path tempDir) throws IOException {
        // Attempt to load a file that does not exist
        String nonExistentFilePath = tempDir.resolve("nonexistent.json").toUri().toString();
        List<LoadedFile> loadedFiles = loader.load(nonExistentFilePath);

        assertNotNull(loadedFiles);
        assertEquals(1, loadedFiles.size());

        LoadedFile loadedFile = loadedFiles.get(0);
        assertEquals("nonexistent.json", loadedFile.fileName());
        assertNull(loadedFile.fileContents(), "File contents should be null for a non-existent file.");
        assertFalse(loadedFile.systemErrors().isEmpty(), "System errors should be present.");

        SystemError systemError = loadedFile.systemErrors().get(0);
        assertEquals("FILE_NOT_FOUND", systemError.error());
        assertTrue(systemError.message().contains(loadedFile.fileName()));
    }

    @Test
    void testLoadUnsupportedScheme() throws IOException {
        String ftpUrl = "ftp://example.com/gbfs.json";
        // The loader's load method expects gbfs.json to be a discovery file.
        // If it fails to load gbfs.json itself, it returns a list with that one LoadedFile.
        List<LoadedFile> loadedFiles = loader.load(ftpUrl);

        assertNotNull(loadedFiles);
        assertEquals(1, loadedFiles.size());

        LoadedFile loadedFile = loadedFiles.get(0);
        assertEquals("gbfs.json", loadedFile.fileName()); // Filename is derived from URI path
        assertNull(loadedFile.fileContents(), "File contents should be null for an unsupported scheme.");
        assertFalse(loadedFile.systemErrors().isEmpty(), "System errors should be present.");

        SystemError systemError = loadedFile.systemErrors().get(0);
        assertEquals("UNSUPPORTED_SCHEME", systemError.error());
        assertTrue(systemError.message().contains("Scheme not supported: ftp"));
    }

    @Test
    void testLoadConnectionError() throws IOException {
        // This attempts to connect to a likely non-routable address or a blackhole.
        // Timeout is 5 seconds, so this test might be a bit slow.
        String invalidHttpUrl = "https://nonexistentdomain1234567890.com/gbfs.json";
        List<LoadedFile> loadedFiles = loader.load(invalidHttpUrl);

        assertNotNull(loadedFiles);
        assertEquals(1, loadedFiles.size());

        LoadedFile loadedFile = loadedFiles.get(0);
        assertEquals("gbfs.json", loadedFile.fileName());
        assertNull(loadedFile.fileContents(), "File contents should be null on connection error.");
        assertFalse(loadedFile.systemErrors().isEmpty(), "System errors should be present.");

        SystemError systemError = loadedFile.systemErrors().get(0);
        assertEquals("CONNECTION_ERROR", systemError.error());
        // Message can vary, so check for a part of it.
        assertTrue(systemError.message().toLowerCase().contains("nonexistentdomain1234567890.com") ||
                   systemError.message().toLowerCase().contains("resolve host") ||
                   systemError.message().toLowerCase().contains("timed out"),
                   "Error message " + systemError.message() + " did not contain expected text.");
    }

    @Test
    void testLoadValidDiscoveryFileWithFeedConnectionError(@TempDir Path tempDir) throws IOException {
        // Create a valid gbfs.json discovery file
        File gbfsJsonFile = tempDir.resolve("gbfs.json").toFile();
        try (PrintWriter writer = new PrintWriter(gbfsJsonFile)) {
            writer.println("{");
            writer.println("  \"last_updated\": 1600000000,");
            writer.println("  \"ttl\": 3600,");
            writer.println("  \"version\": \"2.3\",");
            writer.println("  \"data\": {");
            writer.println("    \"feeds\": [");
            writer.println("      {");
            writer.println("        \"name\": \"system_information\",");
            writer.println("        \"url\": \"https://nonexistentdomain1234567890.com/system_information.json\"");
            writer.println("      }");
            writer.println("    ]");
            writer.println("  }");
            writer.println("}");
        }

        List<LoadedFile> loadedFiles = loader.load(gbfsJsonFile.toURI().toString());
        assertNotNull(loadedFiles);
        assertEquals(2, loadedFiles.size(), "Should contain gbfs.json and the one feed file.");

        LoadedFile gbfsFileResult = loadedFiles.stream().filter(f -> f.fileName().equals("gbfs.json")).findFirst().orElse(null);
        assertNotNull(gbfsFileResult);
        assertTrue(gbfsFileResult.systemErrors().isEmpty(), "gbfs.json itself should have no system errors.");
        assertNotNull(gbfsFileResult.fileContents(), "gbfs.json content should be loaded.");
        gbfsFileResult.fileContents().close(); // Close the stream

        LoadedFile systemInfoResult = loadedFiles.stream().filter(f -> f.fileName().equals("system_information.json")).findFirst().orElse(null);
        assertNotNull(systemInfoResult);
        assertNull(systemInfoResult.fileContents(), "system_information.json content should be null due to connection error.");
        assertFalse(systemInfoResult.systemErrors().isEmpty(), "system_information.json should have system errors.");
        assertEquals("CONNECTION_ERROR", systemInfoResult.systemErrors().get(0).error());
    }
}
