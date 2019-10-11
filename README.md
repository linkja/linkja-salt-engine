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
