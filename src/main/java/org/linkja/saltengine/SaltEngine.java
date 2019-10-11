package org.linkja.saltengine;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.linkja.core.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;

public class SaltEngine {
  public static final char SITE_FILE_DELIMITER = ',';
  public static final int SITE_FILE_COLUMN_COUNT = 2;

  public static final int MINIMUM_TOKEN_LENGTH = 32;
  public static final int DEFAULT_TOKEN_LENGTH = MINIMUM_TOKEN_LENGTH;
  public static final int MAXIMUM_TOKEN_LENGTH = 1024;

  // Location of fields in the sites CSV file
  public static final int SITE_ID_INDEX = 0;
  public static final int SITE_NAME_INDEX = 1;

  // Used across all modes
  private File sitesFile;

  // Used for generating a new project
  private String projectName;

  // Used for adding sites to an existing project
  private File saltFile;

  private FileHelper fileHelper;

  public SaltEngine() {
    fileHelper = new FileHelper();
  }

  /**
   * Used for unit testing - use empty constructor otherwise
   * @param helper
   */
  public SaltEngine(FileHelper helper) {
    fileHelper = helper;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) throws LinkjaException {
    if (projectName == null || projectName.trim().equals("")) {
      throw new LinkjaException("You must set a project name that is at least 1 non-whitespace character long");
    }

    this.projectName = projectName.trim();
  }

  public File getSitesFile() {
    return sitesFile;
  }

  public void setSitesFile(File sitesFile) throws FileNotFoundException {
    if (!fileHelper.exists(sitesFile)) {
      throw new FileNotFoundException(String.format("Unable to find site configuration file %s", sitesFile.toString()));
    }
    this.sitesFile = sitesFile;
  }

  public void setSitesFile(String sitesFile) throws FileNotFoundException {
    File file = new File(sitesFile);
    setSitesFile(file);
  }

  public File getSaltFile() {
    return saltFile;
  }

  public void setSaltFile(File saltFile) throws FileNotFoundException {
    if (!fileHelper.exists(saltFile)) {
      throw new FileNotFoundException(String.format("Unable to find your salt file %s", saltFile.toString()));
    }
    this.saltFile = saltFile;
  }

  public void setSaltFile(String setSaltFile) throws FileNotFoundException {
    File file = new File(setSaltFile);
    setSaltFile(file);
  }

  /**
   * Generate the salts for the configured sites
   */
  public void generate() throws Exception {
    Path parentPath = getSiteFileParentPath();
    List<Site> sites = loadSites(this.sitesFile);
    validateSites(sites);
    String projectToken = generateToken();
    for (Site site : sites) {
      generateSaltFile(site, generateToken(), projectToken, this.projectName, parentPath);
    }
  }

  public void addSites() throws Exception {
    Path parentPath = getSiteFileParentPath();
    List<Site> sites = loadSites(this.sitesFile);
    validateSites(sites);

    SaltFile existingFile = new SaltFile();
    existingFile.load(this.saltFile);
    String projectToken = existingFile.getProjectSalt();
    String projectName = existingFile.getProjectName();

    for (Site site : sites) {
      generateSaltFile(site, generateToken(), projectToken, projectName, parentPath);
    }
  }

  private void generateSaltFile(Site site, String privateToken, String projectToken, String projectName, Path parentPath) throws Exception {
    SaltFile file = new SaltFile();
    file.setSite(site);
    file.setPrivateSalt(privateToken);
    file.setProjectSalt(projectToken);
    file.setProjectName(projectName);
  }

  /**
   * Utility method to return the parent path where the specified site file is located
   * @return
   * @throws LinkjaException
   */
  private Path getSiteFileParentPath() throws LinkjaException {
    // Get the directory where the configuration file was specified.  That is going to be our directory for the
    // generated results.
    File parent = this.sitesFile.getAbsoluteFile().getParentFile();
    if (!parent.isDirectory()) {
      throw new LinkjaException("Unexpected error with the directory containing the sites configuration file");
    }

    return parent.toPath();
  }

  /**
   * Creates a random token string of the default length
   * @return
   * @throws LinkjaException
   */
  public String generateToken() throws LinkjaException {
    return generateToken(DEFAULT_TOKEN_LENGTH);
  }

  /**
   * Creates a random token string of the specified length
   * @param tokenLength
   * @return
   * @throws LinkjaException
   */
  public String generateToken(int tokenLength) throws LinkjaException {
    if (tokenLength < MINIMUM_TOKEN_LENGTH || tokenLength > MAXIMUM_TOKEN_LENGTH) {
      throw new LinkjaException(String.format("The token must be between %d and %d characters, but %d were requested.",
              MINIMUM_TOKEN_LENGTH, MAXIMUM_TOKEN_LENGTH, tokenLength));
    }

    byte[] randomBytes = new byte[tokenLength];
    SecureRandom random = new SecureRandom();
    random.nextBytes(randomBytes);
    return Base64.getEncoder().encodeToString(randomBytes);
  }

  /**
   * Load the site information from the provided file (assumed to be a CSV)
   * @param siteFile
   * @return
   * @throws LinkjaException
   * @throws FileNotFoundException
   */
  public List<Site> loadSites(File siteFile) throws LinkjaException, FileNotFoundException {
    List<Site> sites = new ArrayList<Site>();
    if (!fileHelper.exists(siteFile)) {
      throw new FileNotFoundException(String.format("Unable to find the site control file %s", siteFile.toString()));
    }

    try (BufferedReader csvReader = new BufferedReader(new FileReader(siteFile))) {
      CSVParser parser = CSVParser.parse(csvReader, CSVFormat.DEFAULT.withDelimiter(SITE_FILE_DELIMITER));
      for (CSVRecord csvRecord : parser) {
        if (csvRecord.size() != SITE_FILE_COLUMN_COUNT) {
          throw new LinkjaException(String.format("Row %d has %d columns.  The site control file must have exactly %d columns on each row: Site ID, Site Name",
                  csvRecord.getRecordNumber(), csvRecord.size(), SITE_FILE_COLUMN_COUNT));
        }

        Site site = new Site(csvRecord.get(SITE_ID_INDEX), csvRecord.get(SITE_NAME_INDEX));
        if (site.getSiteID().equals("")) {
          throw new LinkjaException(String.format("Row %d a blank site ID, which is not allowed.",
            csvRecord.getRecordNumber()));
        }
        if (site.getSiteName().equals("")) {
          throw new LinkjaException(String.format("Row %d a blank site name, which is not allowed.",
            csvRecord.getRecordNumber()));
        }
        sites.add(site);
      }
    } catch (IOException e) {
      throw new LinkjaException("There was an error loading the sites configuration file.  Please make sure the file exists, and that it is a valid CSV file.");
    }

    return sites;
  }

  /**
   * Perform validation of the list of sites that have been loaded.
   * @throws LinkjaException
   */
  public void validateSites(List<Site> sites) throws LinkjaException {
    // We consider an absence of sites to be invalid when this check is performed.  In normal initialization it's okay
    // if the sites is null/empty, but when we are explicitly validating we expect there to be something.
    if (sites == null || sites.size() == 0) {
      throw new LinkjaException("You must specify at least one site");
    }

    // Ensure that all of the site IDs and key files are distinct
    HashSet<String> ids = new HashSet<String>();
    HashSet<String> names = new HashSet<String>();
    HashSet<URI> keyFiles = new HashSet<URI>();
    for(Site site : sites) {
      if (ids.contains(site.getSiteID())) {
        throw new LinkjaException(String.format("Site IDs must be unique, but '%s' was found more than once.", site.getSiteID()));
      }
      ids.add(site.getSiteID());

      if (names.contains(site.getSiteName())) {
        throw new LinkjaException(String.format("Site names must be unique, but '%s' was found more than once.", site.getSiteName()));
      }
      names.add(site.getSiteName());
    }
  }
}
