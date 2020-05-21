# linkja-salt-engine

Anticipated Date: 05/03/2019

## Building
linkja-salt-engine was built using Java JDK 10 (specifically [OpenJDK](https://openjdk.java.net/)).  It can be opened from within an IDE like Eclipse or IntelliJ IDEA and compiled, or compiled from the command line using [Maven](https://maven.apache.org/).

You can build linkja-salt-engine via Maven:

`mvn clean package`

This will compile the code, run all unit tests, and create an executable JAR file under the .\target folder with all dependency JARs included.  The JAR will be named something like `SaltEngine-1.0-jar-with-dependencies.jar`.

## Program Use
You can run the executable JAR file using the standard Java command:
`java -jar SaltEngine-1.0-jar-with-dependencies.jar `

The program has two modes that can be invoked - the primary one (enabled with `--generateProject`) will create a set of salt files for multiple sites in a project.
The second mode allows you to add one or more sites to an existing project (enabled with `--addSites`).

For hashing, linkja-salt-engine uses a special C library (.dll/.dylib/.so, depending on your operating system).  You will need to tell Java where to find this library when you try to run the program.  Otherwise, you will get an error:

```
Exception in thread "main" java.lang.UnsatisfiedLinkError: no linkjacrypto in java.library.path:
```

The library may be placed in any directory found by the Java library path.  If you would like to specify the library, you can include the `-Djava.library.path=` option when running the program.
This can be the same directory as the linkja-salt-engine JAR file (e.g., `-Djava.library.path=.`).

If you specify `--version`, the program will display the application version.  

Note that where files are used for input, they can be specified as a relative or absolute path.


### Generate Project
Usage: `java -jar SaltEngine.jar --generateProject`

The program is expecting a minimum of two parameters:

```
 -sf,--siteFile <arg>              The path to a file containing the site definitions
 -pn,--projectName <arg>           The name of the project to create
```

### Add Sites to Existing Project
Usage: `java -jar SaltEngine.jar --addSites`

The program is expecting a minimum of three parameters:

```
 -sf,--siteFile <arg>              The path to a file containing the site definitions
 -salt,--saltFile <arg>            The path to your encrypted salt file for the existing project
```


**Examples:**

Display the version information

```
java -Djava.library.path=. -jar SaltEngine-1.0-jar-with-dependencies.jar --version
```

Create salt files for a project named 'Test Project', for all sites specified in a
file named `sites.csv`.  The CSV file is located in a subdirectory (`data`) relative to 
where the JAR is located.

```
java -Djava.library.path=. -jar SaltEngine-1.0-jar-with-dependencies.jar --generateProject
  --siteFile data/sites.csv --projectName "Test Project"
```
