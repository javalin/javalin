 # Javalin
This readme is for Javalin developers. 
If you are looking for the general readme, see [.github/README.md](.github/README.md).

## Getting started
```sh
gh repo clone javalin/javalin
cd javalin
./mvnw package   #(or `mvn package` if you have maven installed)
./mvnw test      #(or `mvn test` if you have maven installed)
```

## Running maven commands
We have Maven wrapper included in the project, which means that
you can run Maven commands without having Maven installed on your system.
Simply replace any `mvn goal` command with `./mvnw goal`.

## Deploy
The `sonatype-oss-release` profile is used for releasing the project artifacts to the [Maven Central Portal](https://central.sonatype.com). 
This is only used by tipsy to release the project.
