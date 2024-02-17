 # Javalin
This readme is for Javalin developers. 
If you are looking for the general readme, see [.github/README.md](.github/README.md).

## Maven profiles

Our project uses Maven for build automation and dependency management. 
We have defined three profiles in our `pom.xml` for different scenarios.

### dev
This profile is used for development. GPG signing of the artifacts is skipped in this profile. 
You can activate it using the command `mvn <goals> -P dev`.

### publish-snapshot
This profile is used when publishing a snapshot version of the project. 
GPG signing of the artifacts is also skipped in this profile. 
You can activate it using the command `mvn <goals> -P publish-snapshot`.

### sonatype-oss-release
This profile is used for releasing the project artifacts to Sonatype OSSRH (OSS Repository Hosting). 
It uses the default configuration. This is only used by tipsy to release the project.

Replace `<goals>` with your desired Maven goals such as `clean install`.

## Running maven commands
We have Maven wrapper included in the project, which means that
you can run Maven commands without having Maven installed on your system.

For example, to run the tests for dev, use the following command:

```shell
./mvnw test -P dev
```
