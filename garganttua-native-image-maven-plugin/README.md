# Garganttua Native Image Maven Plugin

## Description

Garganttua Native Image Maven Plugin is a **Maven plugin that automates GraalVM Native Image configuration management** during the build process. It simplifies the creation of native executables by automatically discovering, copying, and validating native image configuration files from dependencies.

When building native images with GraalVM, all reflection and resource metadata must be explicitly declared in configuration files. This plugin automates the tedious task of managing these configurations by:
- Extracting native image configuration files from JAR dependencies
- Organizing them in the correct directory structure
- Validating and registering application resources for native image inclusion
- Integrating seamlessly with the Maven build lifecycle

**Key Features:**
- **Automatic Configuration Discovery** - Scans all project dependencies for `META-INF/native-image/` directories
- **Configuration Aggregation** - Collects and merges native image configurations from multiple libraries
- **Resource Validation** - Validates that declared resources exist before native image compilation
- **Resource Registration** - Automatically adds application resources to `resource-config.json`
- **Maven Lifecycle Integration** - Runs during `process-resources` phase automatically
- **Multi-Module Support** - Works seamlessly in multi-module Maven projects
- **Build-Time Optimization** - Prepares all native image metadata during compile time
- **Zero Configuration** - Works out-of-the-box with sensible defaults

## Installation

<!-- AUTO-GENERATED-START -->
### Installation with Maven
```xml
<dependency>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native-image-maven-plugin</artifactId>
    <version>2.0.0-ALPHA01</version>
</dependency>
```

### Actual version
2.0.0-ALPHA01

### Dependencies
 - `org.apache.maven.plugin-tools:maven-plugin-annotations:provided`
 - `org.apache.maven:maven-plugin-api:provided`
 - `org.apache.maven:maven-core`
 - `com.garganttua.core:garganttua-native`

<!-- AUTO-GENERATED-END -->

## Core Concepts

### Maven Plugin Architecture

This plugin extends `AbstractMojo` and implements a single goal: `copy-native-image`. It executes during the **process-resources** phase of the Maven lifecycle, ensuring all native image configurations are prepared before compilation.

**Execution Flow:**
1. **Dependency Scanning** - Iterates through all runtime dependencies
2. **Configuration Extraction** - Extracts `META-INF/native-image/` contents from JAR files
3. **Directory Organization** - Organizes configurations by groupId/artifactId/version
4. **Resource Validation** - Validates user-specified resources exist in `src/main/resources/`
5. **Resource Registration** - Adds validated resources to `resource-config.json`

### Copy Native Image Goal

The `copy-native-image` goal is the core functionality of this plugin. It performs two primary tasks:

**Task 1: Copy Native Image Configurations from Dependencies**

Scans all project dependencies (JARs) and extracts any files found in `META-INF/native-image/` directories. These files typically include:
- `reflect-config.json` - Reflection metadata
- `resource-config.json` - Resource inclusion patterns
- `jni-config.json` - JNI metadata
- `proxy-config.json` - Dynamic proxy configurations
- `serialization-config.json` - Serialization metadata

**Output Structure:**
```
target/classes/META-INF/native-image/
├── com.example/library-a/1.0.0/
│   ├── reflect-config.json
│   └── resource-config.json
├── com.example/library-b/2.1.0/
│   └── reflect-config.json
└── ...
```

**Task 2: Validate and Register Application Resources**

Validates that resources specified in the plugin configuration exist and registers them in the native image resource configuration file.

### Configuration Parameters

The plugin accepts the following configuration parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `buildDirectory` | File | `${project.build.directory}` | Maven build directory (usually `target/`) |
| `buildOutputDirectory` | String | `${project.build.outputDirectory}` | Compiled classes directory (usually `target/classes/`) |
| `artifacts` | Set<Artifact> | `${project.artifacts}` | All project dependencies (automatically populated) |
| `baseDir` | String | `${basedir}` | Project base directory |
| `resources` | List<String> | `null` | List of resource paths to include in native image |
| `dependencies` | List<Dependency> | `null` | Specific dependencies to process (optional filter) |

### Resource Validation

The plugin validates resources declared in the `<resources>` configuration by:
1. Checking if each resource file exists in `src/main/resources/`
2. Throwing a build failure if any resource is missing
3. Adding valid resources to `META-INF/native-image/resource-config.json`

This prevents runtime errors where the native image expects resources that don't exist.

### Directory Structure Convention

Extracted native image configurations follow GraalVM's convention:

```
META-INF/native-image/
    <groupId>/
        <artifactId>/
            <version>/
                reflect-config.json
                resource-config.json
                ...
```

This structure ensures configurations from different libraries don't conflict.

## Usage

### 1. Basic Plugin Configuration

Minimal configuration to enable native image support:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.garganttua.core</groupId>
            <artifactId>garganttua-native-image-maven-plugin</artifactId>
            <version>2.0.0-ALPHA01</version>
            <executions>
                <execution>
                    <goals>
                        <goal>copy-native-image</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**What This Does:**
- Automatically runs during `process-resources` phase
- Scans all dependencies for native image configurations
- Copies discovered configurations to `target/classes/META-INF/native-image/`

### 2. Registering Application Resources

Declare resources that must be included in the native image:

```xml
<plugin>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native-image-maven-plugin</artifactId>
    <version>2.0.0-ALPHA01</version>
    <executions>
        <execution>
            <goals>
                <goal>copy-native-image</goal>
            </goals>
            <configuration>
                <resources>
                    <resource>application.properties</resource>
                    <resource>config/database.yml</resource>
                    <resource>templates/email.html</resource>
                    <resource>static/logo.png</resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Result:**
- Plugin validates all listed resources exist in `src/main/resources/`
- Adds each resource to `target/classes/META-INF/native-image/resource-config.json`
- Build fails if any resource is missing

### 3. Binding to Specific Build Phase

Explicitly control when the plugin executes:

```xml
<plugin>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native-image-maven-plugin</artifactId>
    <version>2.0.0-ALPHA01</version>
    <executions>
        <execution>
            <id>prepare-native-image</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>copy-native-image</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Common Build Phases:**
- `process-resources` (default) - Before compilation
- `prepare-package` - After compilation, before packaging
- `package` - During JAR/WAR creation

### 4. Multi-Module Project Configuration

Use in parent POM to apply to all modules:

```xml
<!-- Parent POM -->
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>com.garganttua.core</groupId>
                <artifactId>garganttua-native-image-maven-plugin</artifactId>
                <version>2.0.0-ALPHA01</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>copy-native-image</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

```xml
<!-- Child Module POM -->
<build>
    <plugins>
        <plugin>
            <groupId>com.garganttua.core</groupId>
            <artifactId>garganttua-native-image-maven-plugin</artifactId>
            <configuration>
                <resources>
                    <resource>module-specific-config.json</resource>
                </resources>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 5. Resource Pattern Registration

Register resources with different path patterns:

```xml
<configuration>
    <resources>
        <!-- Simple file -->
        <resource>banner.txt</resource>

        <!-- Nested directory -->
        <resource>i18n/messages_en.properties</resource>
        <resource>i18n/messages_fr.properties</resource>
        <resource>i18n/messages_de.properties</resource>

        <!-- Template files -->
        <resource>templates/invoice.pdf</resource>
        <resource>templates/receipt.html</resource>

        <!-- Configuration files -->
        <resource>config/application-dev.yml</resource>
        <resource>config/application-prod.yml</resource>

        <!-- Static assets -->
        <resource>static/css/styles.css</resource>
        <resource>static/js/app.js</resource>
    </resources>
</configuration>
```

### 6. Integration with GraalVM Native Image Maven Plugin

Combine with the official GraalVM plugin for complete native image builds:

```xml
<build>
    <plugins>
        <!-- Step 1: Prepare native image configurations -->
        <plugin>
            <groupId>com.garganttua.core</groupId>
            <artifactId>garganttua-native-image-maven-plugin</artifactId>
            <version>2.0.0-ALPHA01</version>
            <executions>
                <execution>
                    <goals>
                        <goal>copy-native-image</goal>
                    </goals>
                    <configuration>
                        <resources>
                            <resource>application.properties</resource>
                            <resource>logback.xml</resource>
                        </resources>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Step 2: Build native image -->
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
            <version>0.10.0</version>
            <executions>
                <execution>
                    <id>build-native</id>
                    <goals>
                        <goal>compile-no-fork</goal>
                    </goals>
                    <phase>package</phase>
                </execution>
            </executions>
            <configuration>
                <imageName>${project.artifactId}</imageName>
                <mainClass>com.example.Main</mainClass>
                <buildArgs>
                    <buildArg>--no-fallback</buildArg>
                    <buildArg>--verbose</buildArg>
                </buildArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Build Command:**
```bash
mvn clean package -Pnative
```

### 7. Conditional Execution with Maven Profiles

Enable native image preparation only for specific profiles:

```xml
<profiles>
    <profile>
        <id>native</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>com.garganttua.core</groupId>
                    <artifactId>garganttua-native-image-maven-plugin</artifactId>
                    <version>2.0.0-ALPHA01</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>copy-native-image</goal>
                            </goals>
                            <configuration>
                                <resources>
                                    <resource>application-native.properties</resource>
                                </resources>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

**Activate Profile:**
```bash
mvn clean package -Pnative
```

### 8. Debugging Plugin Execution

Enable verbose logging to troubleshoot configuration issues:

```bash
# Maven debug mode
mvn clean install -X

# Plugin-specific logging
mvn clean install -Dorg.slf4j.simpleLogger.log.com.garganttua=DEBUG
```

**Log Output Analysis:**
```
[INFO] Looking for Native-image files into 45 artefacts
[INFO] Looking for Native-image files into: jackson-databind
[INFO] Native-image files detected into: jackson-databind
[INFO] Looking for Native-image files into: spring-boot-autoconfigure
[INFO] Native-image files detected into: spring-boot-autoconfigure
[INFO] Native-image resource added application.properties
[INFO] Native-image resource added logback.xml
[INFO] Native-image files copied successfully to: target/classes/META-INF/native-image
```

### 9. Spring Boot Integration

Configure for Spring Boot applications:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <version>3.2.0</version>
        </plugin>

        <plugin>
            <groupId>com.garganttua.core</groupId>
            <artifactId>garganttua-native-image-maven-plugin</artifactId>
            <version>2.0.0-ALPHA01</version>
            <executions>
                <execution>
                    <goals>
                        <goal>copy-native-image</goal>
                    </goals>
                    <configuration>
                        <resources>
                            <resource>application.yml</resource>
                            <resource>application-prod.yml</resource>
                            <resource>static/index.html</resource>
                            <resource>templates/home.html</resource>
                        </resources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 10. Quarkus Integration

Configure for Quarkus applications:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-maven-plugin</artifactId>
            <version>3.6.0</version>
        </plugin>

        <plugin>
            <groupId>com.garganttua.core</groupId>
            <artifactId>garganttua-native-image-maven-plugin</artifactId>
            <version>2.0.0-ALPHA01</version>
            <executions>
                <execution>
                    <goals>
                        <goal>copy-native-image</goal>
                    </goals>
                    <configuration>
                        <resources>
                            <resource>application.properties</resource>
                            <resource>META-INF/resources/index.html</resource>
                        </resources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Advanced Patterns

### Automated Resource Discovery

Create a custom Maven extension to automatically discover resources:

```xml
<plugin>
    <groupId>com.garganttua.core</groupId>
    <artifactId>garganttua-native-image-maven-plugin</artifactId>
    <version>2.0.0-ALPHA01</version>
    <executions>
        <execution>
            <goals>
                <goal>copy-native-image</goal>
            </goals>
            <configuration>
                <resources>
                    <!-- Use Ant-style patterns via custom extension -->
                    ${discovered.resources}
                </resources>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>resource-discovery-extension</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

### Configuration File Merging

When multiple dependencies provide conflicting configurations, GraalVM merges them. Organize by priority:

```
target/classes/META-INF/native-image/
├── com.yourcompany/your-app/1.0.0/          (Highest priority)
│   └── reflect-config.json
├── com.library/lib-a/2.0.0/                  (Medium priority)
│   └── reflect-config.json
└── com.library/lib-b/1.5.0/                  (Lowest priority)
    └── reflect-config.json
```

GraalVM processes these in order and merges the configurations.

### Environment-Specific Resources

Use Maven profiles to include different resources per environment:

```xml
<profiles>
    <profile>
        <id>dev</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>com.garganttua.core</groupId>
                    <artifactId>garganttua-native-image-maven-plugin</artifactId>
                    <configuration>
                        <resources>
                            <resource>application-dev.properties</resource>
                            <resource>logback-dev.xml</resource>
                        </resources>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>

    <profile>
        <id>prod</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>com.garganttua.core</groupId>
                    <artifactId>garganttua-native-image-maven-plugin</artifactId>
                    <configuration>
                        <resources>
                            <resource>application-prod.properties</resource>
                            <resource>logback-prod.xml</resource>
                        </resources>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Build Native Image

on: [push]

jobs:
  build-native:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup GraalVM
        uses: graalvm/setup-graalvm@v1
        with:
          java-version: '21'
          distribution: 'graalvm'

      - name: Build with Maven
        run: mvn clean package -Pnative

      - name: Upload native image
        uses: actions/upload-artifact@v3
        with:
          name: native-executable
          path: target/my-app
```

## Performance

### Build Time Impact

The plugin adds minimal overhead to the Maven build:

- **Dependency Scanning**: ~100-500ms for typical projects (50-100 dependencies)
- **Configuration Extraction**: ~10-50ms per dependency with native image configs
- **Resource Validation**: ~1-5ms per resource file

**Total Overhead**: Usually < 2 seconds for most projects

### Optimization Strategies

1. **Limit Dependency Scanning**: Use the `<dependencies>` parameter to filter specific artifacts
2. **Cache Build Directory**: Maven build caches reduce repeated work
3. **Parallel Builds**: Use `mvn -T 4` for multi-threaded builds
4. **Profile Activation**: Only run native image preparation when needed (`-Pnative`)

### Native Image Build Performance

Proper configuration management improves native image compilation:

- **Reduced analysis time**: Pre-configured reflection reduces GraalVM analysis overhead
- **Smaller binaries**: Precise resource inclusion prevents bloat
- **Faster iterations**: Cached configurations speed up incremental builds

## Troubleshooting

### Common Issues

**Issue 1: Resources Not Found in Native Image**

```
Error: Resource application.properties not found at runtime
```

**Solution:**
```xml
<configuration>
    <resources>
        <resource>application.properties</resource>
    </resources>
</configuration>
```

**Issue 2: Build Fails with "Resource does not exist"**

```
[ERROR] Native-image resource config/missing.yml does not exist in src/main/resources/
```

**Solution:** Verify the resource path and ensure it exists in `src/main/resources/`:
```bash
ls -la src/main/resources/config/missing.yml
```

**Issue 3: Native Image Configurations Not Applied**

**Symptoms:** GraalVM reports missing reflection configurations despite plugin execution

**Solution:** Ensure plugin runs before native-image compilation:
```xml
<execution>
    <phase>process-resources</phase> <!-- Must run before package -->
    <goals>
        <goal>copy-native-image</goal>
    </goals>
</execution>
```

**Issue 4: Conflicting Configurations from Dependencies**

**Symptoms:** Native image compilation warnings about duplicate entries

**Solution:** GraalVM automatically merges configurations. Use the most specific configuration last (application-level overrides library-level).

### Debugging Steps

1. **Verify Plugin Execution:**
   ```bash
   mvn clean install -X | grep "copy-native-image"
   ```

2. **Check Output Directory:**
   ```bash
   find target/classes/META-INF/native-image -type f
   ```

3. **Inspect Generated Configurations:**
   ```bash
   cat target/classes/META-INF/native-image/resource-config.json
   ```

4. **Validate Resource Paths:**
   ```bash
   ls -R src/main/resources/
   ```

## Tips and Best Practices

### Plugin Configuration

1. **Explicit Resource Declaration** - Always declare required resources explicitly to catch missing files early
2. **Use Relative Paths** - Resource paths should be relative to `src/main/resources/`
3. **Profile-Based Configuration** - Use Maven profiles to separate native and JVM builds
4. **Validate Build Output** - Check `target/classes/META-INF/native-image/` after build
5. **Version Pin Dependencies** - Pin plugin version for reproducible builds

### Resource Management

6. **Minimal Resource Inclusion** - Only include resources actually used at runtime (smaller binaries)
7. **Environment-Specific Resources** - Use profiles for dev/test/prod specific resources
8. **Resource Organization** - Group resources by type (config/, templates/, static/)
9. **Documentation** - Comment why specific resources are included in native image
10. **Testing** - Test native image with all expected resources accessible

### Build Performance

11. **Conditional Execution** - Use profiles to only run plugin for native builds
12. **Dependency Filtering** - If needed, filter specific dependencies to reduce scan time
13. **Clean Builds** - Use `mvn clean` before native image builds to avoid stale configs
14. **Parallel Builds** - Enable parallel Maven execution (`-T` flag) for multi-module projects
15. **CI/CD Caching** - Cache `target/` directory in CI to speed up builds

### Integration Patterns

16. **Combine with Garganttua Native** - Use programmatic config generation alongside this plugin
17. **GraalVM Plugin First** - Let official GraalVM plugin handle native compilation
18. **Spring Native** - Integrate with Spring's native image support tools
19. **Quarkus Extensions** - Use alongside Quarkus native build extensions
20. **Custom Mojos** - Extend plugin with custom goals for project-specific needs

### Multi-Module Projects

21. **Parent POM Configuration** - Define plugin in `<pluginManagement>` for consistency
22. **Module-Specific Resources** - Each module declares its own resources
23. **Aggregated Builds** - Run from parent to build all modules together
24. **Selective Execution** - Use `-pl` flag to build specific modules
25. **Shared Resources** - Place common resources in shared module

### Debugging and Troubleshooting

26. **Enable Debug Logging** - Use `mvn -X` to see detailed plugin execution
27. **Verify Configurations** - Manually inspect generated JSON files
28. **Test Incrementally** - Add resources one at a time when troubleshooting
29. **GraalVM Diagnostics** - Use GraalVM's verbose flags to debug missing configs
30. **Build Reproducibility** - Commit generated configs to version control for comparison

### Common Pitfalls to Avoid

31. **Don't Forget Resources** - Missing resources cause runtime failures in native images
32. **Don't Use Absolute Paths** - Resource paths must be relative to `src/main/resources/`
33. **Don't Skip Validation** - Always validate resources exist during build
34. **Don't Mix Configurations** - Keep native image configs separate from JVM-only configs
35. **Don't Over-Include** - Including too many resources bloats native image size
36. **Version Compatibility** - Ensure plugin version matches Garganttua Native library version

## Integration Examples

### Complete Spring Boot Example

```xml
<project>
    <properties>
        <graalvm.version>23.1.0</graalvm.version>
        <spring-boot.version>3.2.0</spring-boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>com.garganttua.core</groupId>
                <artifactId>garganttua-native-image-maven-plugin</artifactId>
                <version>2.0.0-ALPHA01</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>copy-native-image</goal>
                        </goals>
                        <configuration>
                            <resources>
                                <resource>application.yml</resource>
                                <resource>static/index.html</resource>
                                <resource>templates/error.html</resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <imageName>my-spring-app</imageName>
                    <mainClass>com.example.Application</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Complete Quarkus Example

```xml
<project>
    <properties>
        <quarkus.version>3.6.0</quarkus.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>com.garganttua.core</groupId>
                <artifactId>garganttua-native-image-maven-plugin</artifactId>
                <version>2.0.0-ALPHA01</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>copy-native-image</goal>
                        </goals>
                        <configuration>
                            <resources>
                                <resource>application.properties</resource>
                                <resource>import.sql</resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

## License

This module is distributed under the MIT License.
