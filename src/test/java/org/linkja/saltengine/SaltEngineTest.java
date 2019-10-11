package org.linkja.saltengine;

import org.junit.jupiter.api.Test;
import org.linkja.core.CryptoHelper;
import org.linkja.core.FileHelper;
import org.linkja.core.LinkjaException;
import org.linkja.core.Site;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaltEngineTest {
  @Test
  void loadSites_FileNotFound() {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> false);

    SaltEngine engine = new SaltEngine(fileHelperMock);
    File file = new File("/test/path/assumed/invalid");
    assertThrows(FileNotFoundException.class, () -> engine.loadSites(file));
  }

  @Test
  void loadSites_EmptyFile() throws URISyntaxException, FileNotFoundException, LinkjaException {
    ClassLoader classLoader = getClass().getClassLoader();
    File sitesFile = new File(classLoader.getResource("empty-sites-file.csv").toURI());

    SaltEngine engine = new SaltEngine();
    List<Site> sites = engine.loadSites(sitesFile);
    assertEquals(0, sites.size());
  }

  @Test
  void loadSites_InvalidFile() throws URISyntaxException {
    ClassLoader classLoader = getClass().getClassLoader();
    File sitesFile = new File(classLoader.getResource("invalid-sites-file.csv").toURI());

    SaltEngine engine = new SaltEngine();
    assertThrows(LinkjaException.class, () -> engine.loadSites(sitesFile));
  }

  @Test
  void loadSites_ValidFile() throws URISyntaxException, LinkjaException, FileNotFoundException {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> true);
    ClassLoader classLoader = getClass().getClassLoader();
    File sitesFile = new File(classLoader.getResource("valid-sites-file.csv").toURI());

    SaltEngine engine = new SaltEngine(fileHelperMock);
    List<Site> sites = engine.loadSites(sitesFile);
    assertEquals(3, sites.size());
  }

  @Test
  void validateSites_Null() throws NoSuchFieldException {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> true);
    SaltEngine engine = new SaltEngine(fileHelperMock);
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.validateSites(null));
    assertEquals("You must specify at least one site", exception.getMessage());
  }

  @Test
  void validateSites_Empty() throws NoSuchFieldException {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> true);
    ArrayList<Site> sites = new ArrayList<Site>();
    SaltEngine engine = new SaltEngine(fileHelperMock);
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.validateSites(sites));
    assertEquals("You must specify at least one site", exception.getMessage());
  }

  @Test
  void validateSites_DuplicateID() throws NoSuchFieldException, FileNotFoundException {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> true);
    ArrayList<Site> sites = new ArrayList<Site>() {{
      add(new Site("1", "Test 1"));
      add(new Site("1", "Test 2"));
    }};
    SaltEngine engine = new SaltEngine(fileHelperMock);
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.validateSites(sites));
    assertEquals("Site IDs must be unique, but '1' was found more than once.", exception.getMessage());
  }

  @Test
  void validateSites_DuplicateName() throws NoSuchFieldException, FileNotFoundException {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> true);
    ArrayList<Site> sites = new ArrayList<Site>() {{
      add(new Site("1", "Test"));
      add(new Site("2", "Test"));
    }};
    SaltEngine engine = new SaltEngine(fileHelperMock);
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.validateSites(sites));
    assertEquals("Site names must be unique, but 'Test' was found more than once.", exception.getMessage());
  }

  @Test
  void setProjectName_NullEmpty() {
    SaltEngine engine = new SaltEngine();
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.setProjectName(null));
    assertTrue(exception.getMessage().equals("You must set a project name that is at least 1 non-whitespace character long"));

    exception = assertThrows(LinkjaException.class, () -> engine.setProjectName(""));
    assertTrue(exception.getMessage().equals("You must set a project name that is at least 1 non-whitespace character long"));

    exception = assertThrows(LinkjaException.class, () -> engine.setProjectName("  "));
    assertTrue(exception.getMessage().equals("You must set a project name that is at least 1 non-whitespace character long"));
  }

  @Test
  void setProjectName_Valid() throws LinkjaException {
    SaltEngine engine = new SaltEngine();
    engine.setProjectName("a");
    assertEquals("a", engine.getProjectName());
    engine.setProjectName("a  ");
    assertEquals("a", engine.getProjectName());
    engine.setProjectName("  a");
    assertEquals("a", engine.getProjectName());
    engine.setProjectName("abcdefghijklmnopqrstuvwxyz 0123456789");
    assertEquals("abcdefghijklmnopqrstuvwxyz 0123456789", engine.getProjectName());
  }

  @Test
  void generateToken_SizeViolation() {
    SaltEngine engine = new SaltEngine();
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.generateToken(-1));
    assertTrue(exception.getMessage().equals("The token must be between 32 and 1024 characters, but -1 were requested."));

    exception = assertThrows(LinkjaException.class, () -> engine.generateToken(SaltEngine.MINIMUM_TOKEN_LENGTH - 1));
    assertTrue(exception.getMessage().equals("The token must be between 32 and 1024 characters, but 31 were requested."));

    exception = assertThrows(LinkjaException.class, () -> engine.generateToken(SaltEngine.MAXIMUM_TOKEN_LENGTH + 1));
    assertTrue(exception.getMessage().equals("The token must be between 32 and 1024 characters, but 1025 were requested."));
  }

  @Test
  void generateToken_DefaultLength() throws LinkjaException {
    SaltEngine engine = new SaltEngine();
    String token = engine.generateToken();
    assertEquals(SaltEngine.DEFAULT_TOKEN_LENGTH * 2, token.length());
  }

  @Test
  void generateToken_NonDefaultLength() throws LinkjaException {
    SaltEngine engine = new SaltEngine();
    // Note that the expected length is 2x the input length
    assertEquals(74,  engine.generateToken(37).length());
    assertEquals(202,  engine.generateToken(101).length());
  }

  @Test
  void generateToken_ChangesMultipleIterations() throws LinkjaException {
    SaltEngine engine = new SaltEngine();

    // While this isn't an exhaustive validation of whether or not we get duplicate tokens, it provides simple assurance
    // that we don't have a bug that explicitly causes repeats to appear.
    HashSet<String> tokens = new HashSet<String>();
    for (int index = 0; index < 100; index++) {
      String token = engine.generateToken();
      assertFalse(tokens.contains(token));
      tokens.add(token);
    }
  }

  @Test
  void generateToken_ChangesMultipleInstances() throws LinkjaException {
    // While this isn't an exhaustive validation of whether or not we get duplicate tokens, it provides simple assurance
    // that we don't have a bug that explicitly causes repeats to appear each time a salt engine is created and we
    // get the first token.
    HashSet<String> tokens = new HashSet<String>();
    for (int index = 0; index < 100; index++) {
      SaltEngine engine = new SaltEngine();
      String token = engine.generateToken();
      assertFalse(tokens.contains(token));
      tokens.add(token);
    }
  }
}
