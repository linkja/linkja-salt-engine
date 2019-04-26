package org.linkja.saltengine;

import org.apache.commons.cli.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileNotFoundException;
import java.security.Security;

public class Runner {
  public static void main(String[] args) {
    Options options = setUpCommandLine();
    CommandLine cmd = parseCommandLine(options, args);

    // If the command line initialization failed, we will stop the program now.  Displaying instructions to the user
    // is already handled by the setup and parser methods.
    if (cmd == null) {
      System.exit(1);
    }

    long startTime = System.nanoTime();

    // Perform some initialization - purposefully done after timing has started.
    Security.addProvider(new BouncyCastleProvider());

    SaltEngine engine = new SaltEngine();
    try {
      engine.setProjectName(cmd.getOptionValue("projectName"));
      engine.setSitesFile(cmd.getOptionValue("siteFile"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    }

    try {
      engine.generate();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    long endTime = System.nanoTime();

    double elapsedSeconds = (double)(endTime - startTime) / 1_000_000_000.0;
    System.out.printf("Total execution time: %2f sec\n", elapsedSeconds);
  }

  /**
   * Helper method to prepare our command line options
   * @return Collection of options to be used at the command line
   */
  public static Options setUpCommandLine() {
    Options options = new Options();

    Option projectNameOpt = new Option("pn", "projectName", true, "The name of the project to create");
    projectNameOpt.setRequired(true);
    options.addOption(projectNameOpt);

    Option siteFileOpt = new Option("sf", "siteFile", true, "Control file with the salts to load");
    siteFileOpt.setRequired(true);
    options.addOption(siteFileOpt);

    return options;
  }

  /**
   * Helper method to wrap parsing the command line parameters and reconcile them against the required and optional
   * command line options.
   * @param options Allowed options
   * @param args Actual command line arguments
   * @return Reconciled CommandLine container, or null if unable to process
   */
  public static CommandLine parseCommandLine(Options options, String[] args) {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      displayUsage();
      System.out.println();
      System.out.println(e.getMessage());
      return null;
    }

    return cmd;
  }

  /**
   * Helper method to display expected command line parameters
   */
  public static void displayUsage() {
    System.out.println();
    System.out.println("Usage: java -jar SaltEngine.jar");
    System.out.println();
    System.out.println("Required parameters:");
    System.out.println("  -pn,--projectName <arg>           The name of the project to create");
    System.out.println("  -sf,--siteFile <arg>              The path to a file containing the site definitions");
  }
}