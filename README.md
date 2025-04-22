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

If you run `test` before `package`, you will get an error in the OSGI artifact:

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-dependency-plugin:2.8:unpack-dependencies (unpack-everything) on project javalin-osgi: Artifact has not been packaged yet. When used on reactor artifact, unpack should be executed after packaging: see MDEP-98. -> [Help 1]
```

## Running maven commands
We have Maven wrapper included in the project, which means that
you can run Maven commands without having Maven installed on your system.
Simply replace any `mvn goal` command with `./mvnw goal`.

## Deploy
The `sonatype-oss-release` profile is used for releasing the project artifacts to Sonatype OSSRH (OSS Repository Hosting). 
This is only used by tipsy to release the project.
