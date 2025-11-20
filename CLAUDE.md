# Timer Application

A command-line timer application built with Java 25.

## Architecture

The application follows a layered architecture:

### Layers

1. **Model Layer** (`org.veenix.timer.model`)
   - Domain objects and entities
   - Pure data structures with business logic

2. **Service Layer** (`org.veenix.timer.service`)
   - Business logic
   - Orchestrates operations between CLI and persistence

3. **Persistence Layer** (`org.veenix.timer.persistence`)
   - Database access using SQLite
   - Plain JDBC (no JPA/ORM)
   - SQL queries defined as constants in separate files

4. **CLI Layer** (`org.veenix.timer.cli`)
   - Command-line interface using Picocli
   - User interaction handling

### Project Structure

```
timer2/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── org/
│   │           └── veenix/
│   │               └── timer/
│   │                   ├── Main.java
│   │                   ├── cli/
│   │                   ├── model/
│   │                   ├── service/
│   │                   └── persistence/
│   │                       └── SqlQueries.java
│   └── test/
│       └── java/
│           └── org/
│               └── veenix/
│                   └── timer/
├── pom.xml
└── CLAUDE.md
```

## Technology Stack

- **Java**: 25
- **Build Tool**: Maven 3.9+
- **CLI Framework**: Picocli
- **Database**: SQLite (JDBC)
- **Testing**: JUnit 5

## Requirements

- Java 25
- Maven 3.9+

## Building and Running

### Standard JAR Build

#### Compile and run:
```bash
mvn compile exec:java -Dexec.mainClass="org.veenix.timer.Main" -Dexec.args="timer start"
```

#### Build executable JAR:
```bash
mvn clean package
```

#### Run the JAR (recommended - no warnings):
```bash
./timer.sh timer start -d "My task"
./timer.sh activity list --all
```

Or run directly with java (shows warnings):
```bash
java -jar target/timer-app.jar timer start -d "My task"
```

#### Run tests:
```bash
mvn test
```

**Note about Java 25 Warnings**:
- **Recommended**: Use the `timer.sh` wrapper script to run the application - it produces zero warnings
- When running via `mvn exec:java`, some warnings about deprecated `Unsafe` methods from Maven's Guava library will appear
- These Guava warnings come from Maven's own dependencies (not the application) and cannot be suppressed
- The warnings are harmless and don't affect application functionality
- All SQLite and application-level warnings are properly suppressed via JVM arguments

### Native Image Build (Fast Startup)

The application can be compiled to a native executable using GraalVM for dramatically faster startup times (~10-50x improvement).

#### Prerequisites:
- GraalVM 25+ with native-image installed
  ```bash
  # Install GraalVM (using SDKMAN)
  sdk install java 25-graalvm
  sdk use java 25-graalvm

  # Or download from: https://www.graalvm.org/downloads/
  ```

#### Build native executable:
```bash
mvn clean package -Pnative
```

This creates a native binary at `target/timer` that can be run directly:
```bash
./target/timer timer start -d "My task"
./target/timer activity list --date 20251027
```

#### Performance Comparison:
- **Standard JAR**: ~100-500ms startup time
- **Native Image**: ~5-20ms startup time
- **Benefit**: 10-50x faster for CLI operations

#### Native Image Notes:
- First build takes longer (~2-5 minutes)
- Resulting binary is platform-specific (macOS, Linux, Windows)
- Binary size: ~30-50MB (includes all dependencies)
- **No warnings**: Native image is configured with `--enable-native-access=ALL-UNNAMED` to suppress Java 25 warnings
- No JVM required to run the native executable
- SQLite JDBC driver is explicitly registered at build time via SqliteDriverInitializer
- Native SQLite libraries are bundled in the executable

## Performance Optimizations

The application includes several optimizations for fast CLI startup:

1. **Lazy Database Initialization**: Database connection and table creation only occur when first needed, avoiding overhead for commands that don't require database access.

2. **Native Image Support**: GraalVM Native Image compilation provides instant startup times, making the CLI feel responsive even for frequent operations.

3. **Picocli Annotation Processing**: Compile-time annotation processing generates native-image metadata automatically.

## Development Guidelines

- Each class must have corresponding unit tests
- SQL queries must be defined as constants
- Use plain JDBC for database operations
- Follow the layered architecture pattern
- Avoid eager initialization in constructors to support fast startup
