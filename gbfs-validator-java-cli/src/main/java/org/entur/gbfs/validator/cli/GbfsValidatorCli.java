package org.entur.gbfs.validator.cli;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validator.cli.formatter.ConsoleReportFormatter;
import org.entur.gbfs.validator.cli.formatter.JsonReportFormatter;
import org.entur.gbfs.validator.cli.formatter.ReportFormatter;
import org.entur.gbfs.validator.loader.LoadedFile;
import org.entur.gbfs.validator.loader.Loader;
import org.entur.gbfs.validator.loader.auth.Authentication;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  name = "gbfs-validator",
  mixinStandardHelpOptions = true,
  version = "2.0.52-SNAPSHOT", // TODO this needs to be injected
  description = "Validate GBFS feeds against JSON schemas",
  headerHeading = "Usage:%n%n",
  synopsisHeading = "%n",
  descriptionHeading = "%nDescription:%n%n",
  parameterListHeading = "%nParameters:%n",
  optionListHeading = "%nOptions:%n"
)
public class GbfsValidatorCli implements Callable<Integer> {

  @Option(
    names = { "-u", "--url" },
    description = "URL of the GBFS feed (discovery endpoint)",
    required = true
  )
  private String feedUrl;

  @Option(
    names = { "-s", "--save-report" },
    description = "Local path to output report file"
  )
  private File reportFile;

  @Option(
    names = { "-pr", "--print-report" },
    description = "Print report to standard output (yes/no, default: yes)",
    defaultValue = "yes"
  )
  private String printReport;

  @Option(
    names = { "-vb", "--verbose" },
    description = "Verbose mode - show detailed error information"
  )
  private boolean verbose;

  @Option(
    names = { "--format" },
    description = "Output format: text or json (default: text)",
    defaultValue = "text"
  )
  private String format;

  @ArgGroup(exclusive = false, heading = "%nAuthentication Options:%n")
  private AuthOptions authOptions;

  @Override
  public Integer call() throws Exception {
    Loader loader = null;

    try {
      // 1. Build authentication
      Authentication auth = AuthenticationHandler.buildAuthentication(
        authOptions
      );

      // 2. Create loader and load feeds
      loader = new Loader();
      List<LoadedFile> loadedFiles = loader.load(feedUrl, auth);

      // 3. Check for fatal loader errors
      boolean hasFatalLoaderErrors = loadedFiles
        .stream()
        .anyMatch(file ->
          file.loaderErrors() != null && !file.loaderErrors().isEmpty()
        );

      if (
        hasFatalLoaderErrors &&
        loadedFiles.stream().noneMatch(file -> file.fileContents() != null)
      ) {
        System.err.println("ERROR: Failed to load any feeds from " + feedUrl);
        return 2; // System error
      }

      // 4. Convert loaded files to validator input format
      Map<String, InputStream> fileMap = new HashMap<>();
      for (LoadedFile file : loadedFiles) {
        if (file.fileContents() != null) {
          fileMap.put(file.fileName(), file.fileContents());
        }
      }

      // 5. Validate
      GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
      ValidationResult result = validator.validate(fileMap);

      // 6. Format report
      ReportFormatter formatter = createFormatter(format);
      String report = formatter.format(result, loadedFiles, verbose);

      // 7. Output report
      if ("yes".equalsIgnoreCase(printReport)) {
        System.out.println(report);
      }

      if (reportFile != null) {
        ReportWriter.writeReport(reportFile, report);
        if (!"yes".equalsIgnoreCase(printReport)) {
          System.out.println(
            "Report saved to: " + reportFile.getAbsolutePath()
          );
        }
      }

      // 8. Determine exit code
      if (hasFatalLoaderErrors) {
        return 2; // System error
      } else if (result.summary().errorsCount() > 0) {
        return 1; // Validation failure
      } else {
        return 0; // Success
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e.getMessage());
      if (verbose) {
        e.printStackTrace(System.err);
      }
      return 2;
    } finally {
      if (loader != null) {
        loader.close();
      }
    }
  }

  private ReportFormatter createFormatter(String format) {
    return switch (format.toLowerCase()) {
      case "json" -> new JsonReportFormatter();
      default -> new ConsoleReportFormatter();
    };
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new GbfsValidatorCli()).execute(args);
    System.exit(exitCode);
  }
}
