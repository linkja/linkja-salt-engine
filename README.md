# linkja-salt-engine

Anticipated Date: 04/25/2019

## Building
linkja-salt-engine was built using Java JDK 10 (specifically [OpenJDK](https://openjdk.java.net/)).  It can be opened from within an IDE like Eclipse or IntelliJ IDEA and compiled, or compiled from the command line using [Maven](https://maven.apache.org/).

You can build linkja-salt-engine via Maven:

`mvn clean package`

This will compile the code, run all unit tests, and create an executable JAR file under the .\target folder with all dependency JARs included.  The JAR will be named something like `SaltEngine-1.0-jar-with-dependencies.jar`.

## Program Use
You can run the executable JAR file using the standard Java command:
`java -jar SaltEngine-1.0-jar-with-dependencies.jar `

The program has two modes that can be invoked - the primary one (enabled with `--generateProject`) will create a set of salt files for multiple sites in a project.  This is assumed to be done by the project manager.
