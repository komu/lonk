## Downloading

!!! warning
Lonk has not yet had a stable release. Until then, there's no published artifact on Maven Central and the API
may change without notice.

Once published, the artifact will be available on Maven Central as `dev.komu.lonk:lonk`:

### Gradle

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.komu.lonk:lonk:<VERSION>")
}
```

### Maven

```xml

<dependency>
    <groupId>dev.komu.lonk</groupId>
    <artifactId>lonk</artifactId>
    <version>
        <VERSION>
    </version>
</dependency>
```

Replace `<VERSION>` with the latest published version.
