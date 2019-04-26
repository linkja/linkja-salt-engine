package org.linkja.saltengine;

import org.junit.jupiter.api.Test;
import org.linkja.core.FileHelper;
import org.linkja.core.LinkjaException;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaltEngineTest {

  @Test
  void setProjectName_NullEmpty() {
    SaltEngine engine = new SaltEngine();

    // Document the assumption that we will allow a null project name to be set
    engine.setProjectName(null);
    assertNull(engine.getProjectName());

    // Document the assumption that we will allow an empty project name to be set
    engine.setProjectName("");
    assertEquals("", engine.getProjectName());
  }

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
      add(new Site("1", "Test 1", new File("Test1.key"), fileHelperMock));
      add(new Site("1", "Test 2", new File("Test2.key"), fileHelperMock));
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
      add(new Site("1", "Test", new File("Test1.key"), fileHelperMock));
      add(new Site("2", "Test", new File("Test2.key"), fileHelperMock));
    }};
    SaltEngine engine = new SaltEngine(fileHelperMock);
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.validateSites(sites));
    assertEquals("Site names must be unique, but 'Test' was found more than once.", exception.getMessage());
  }

  @Test
  void validateSites_DuplicateKeyFile() throws NoSuchFieldException, FileNotFoundException {
    FileHelper fileHelperMock = Mockito.mock(FileHelper.class);
    Mockito.when(fileHelperMock.exists(Mockito.any(File.class))).thenAnswer(invoke -> true);
    ArrayList<Site> sites = new ArrayList<Site>() {{
      add(new Site("1", "Test 1", new File("Test1.key"), fileHelperMock));
      add(new Site("2", "Test 2", new File("Test1.key"), fileHelperMock));
    }};
    SaltEngine engine = new SaltEngine(fileHelperMock);
    LinkjaException exception = assertThrows(LinkjaException.class, () -> engine.validateSites(sites));
    assertTrue(exception.getMessage().contains("Public keys for each site must be unique"));
    assertTrue(exception.getMessage().contains("Test1.key"));
  }
}