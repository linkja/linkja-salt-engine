package org.linkja.saltengine;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.linkja.core.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SaltEngine {
  public static final char SITE_FILE_DELIMITER = ',';
  public static final int SITE_FILE_COLUMN_COUNT = 3;

  public static final int MINIMUM_TOKEN_LENGTH = 13;
  public static final int DEFAULT_TOKEN_LENGTH = MINIMUM_TOKEN_LENGTH;
  public static final int MAXIMUM_TOKEN_LENGTH = 20;

  // Location of fields in the sites CSV file
  public static final int SITE_ID_INDEX = 0;
  public static final int SITE_NAME_INDEX = 1;
  public static final int SITE_KEY_FILE_INDEX = 2;

  private static final String ALLOWED_TOKEN_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int NUM_ALLOWED_TOKEN_CHARACTERS = ALLOWED_TOKEN_CHARACTERS.length();

  // Used across all modes
  private File sitesFile;

  // Used for generating a new project
  private String projectName;

  // Used for adding sites to an existing project
  private File privateKey;
  private File saltFile;

  private FileHelper fileHelper;
  //private CryptoHelper cryptoHelper = new CryptoHelper();

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

  public File getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(File privateKey) throws FileNotFoundException {
    if (!fileHelper.exists(privateKey)) {
      throw new FileNotFoundException(String.format("Unable to find your private key file %s", privateKey.toString()));
    }
    this.privateKey = privateKey;
  }

  public void setPrivateKey(String privateKey) throws FileNotFoundException {
    File file = new File(privateKey);
    setPrivateKey(file);
  }

  public File getSaltFile() {
    return saltFile;
  }

  public void setSaltFile(File saltFile) throws FileNotFoundException {
    if (!fileHelper.exists(saltFile)) {
      throw new FileNotFoundException(String.format("Unable to find your encrypted salt file %s", saltFile.toString()));
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
    existingFile.decrypt(this.saltFile, this.privateKey);
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
    File encryptedSaltFile = Paths.get(parentPath.toString(), file.getSaltFileName(file.getProjectName(), site.getSiteID())).toFile();
    file.encrypt(encryptedSaltFile, site.getPublicKeyFile());
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
    return generateToken(MINIMUM_TOKEN_LENGTH);
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

    SecureRandom random = new SecureRandom();
    char token[] = new char[tokenLength];
    for (int index = 0; index < tokenLength; index++) {
      token[index] = ALLOWED_TOKEN_CHARACTERS.charAt(random.nextInt(NUM_ALLOWED_TOKEN_CHARACTERS));
    }
    return new String(token);
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

    Path parentPath = siteFile.getAbsoluteFile().getParentFile().toPath();

    try (BufferedReader csvReader = new BufferedReader(new FileReader(siteFile))) {
      CSVParser parser = CSVParser.parse(csvReader, CSVFormat.DEFAULT.withDelimiter(SITE_FILE_DELIMITER));
      for (CSVRecord csvRecord : parser) {
        if (csvRecord.size() != SITE_FILE_COLUMN_COUNT) {
          throw new LinkjaException(String.format("Row %d has %d columns.  The site control file must have exactly %d columns on each row: Site ID, Site Name, Public Key",
                  csvRecord.getRecordNumber(), csvRecord.size(), SITE_FILE_COLUMN_COUNT));
        }

        File keyFile = new File(csvRecord.get(SITE_KEY_FILE_INDEX));
        if (!keyFile.isAbsolute()) {
          keyFile = Paths.get(parentPath.toAbsolutePath().toString(), keyFile.getPath()).toFile();
        }
        Site site = new Site(csvRecord.get(SITE_ID_INDEX), csvRecord.get(SITE_NAME_INDEX), keyFile, this.fileHelper);
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


      URI publicKeyURI = site.getPublicKeyFile().toURI();
      if (keyFiles.contains(publicKeyURI)) {
        throw new LinkjaException(String.format("Public keys for each site must be unique, but '%s' was used more than once.", publicKeyURI));
      }
      keyFiles.add(publicKeyURI);
    }
  }
}
