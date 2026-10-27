# Native smoke app (draft)

This is a standalone app, not a reactor module. It uses the locally installed
Javalin artifact and dependency reachability metadata from the GraalVM repository.
When the core snapshot changes, pass `-Djavalin.version=<version>` to the fixture.

With GraalVM 21 in `JAVA_HOME`:

```sh
./mvnw -B -pl javalin -am clean install -DskipTests
./mvnw -B -f native-smoke/pom.xml package native:compile-no-fork
native-smoke/target/javalin-native-smoke &
pid=$!
trap 'kill "$pid" 2>/dev/null || true' EXIT
bash native-smoke/check.sh
```

The app checks reflective registration of Jackson's optional JavaTimeModule and
serves health, JSON, JSON-body, async and virtual-thread capability endpoints.
In a native runtime it also checks that classpath Vue scanning fails with a clear
external-directory instruction rather than trying to open a jar filesystem.

This fixture does not prove full native-image support. Before submitting, run it
against the unmodified baseline to capture the native failure, then run the patch.
Also extend the native matrix to cover Jackson 3, Kotlin JSON modules, external
Vue rendering and route overview. User-defined JSON model classes still need
application-owned metadata. The current workflow is a draft until its native run
passes. No CI run has been triggered for this draft.
