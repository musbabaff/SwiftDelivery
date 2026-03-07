# Building SwiftDelivery

## Prerequisites

- **Java Development Kit (JDK):** 17 or higher
- **Apache Maven:** 3.8+ (or use the bundled Maven wrapper if available)
- **Git:** For cloning the repository

## Quick Build

```bash
git clone https://github.com/musbabaff/SwiftDelivery.git
cd SwiftDelivery
mvn clean package
```

The compiled plugin JAR will be located at:
```
target/SwiftDelivery-1.0.0.jar
```

## Build Options

### Skip Tests
```bash
mvn clean package -DskipTests
```

### Clean Build (removes all cached artifacts)
```bash
mvn clean install
```

### Debug Build
```bash
mvn clean package -X
```

## Project Structure

```
SwiftDelivery/
├── src/
│   ├── main/
│   │   ├── java/com/nettyforge/swiftdelivery/   # Java source code
│   │   └── resources/                            # Plugin resources
│   │       ├── plugin.yml                        # Plugin descriptor
│   │       ├── config.yml                        # Default configuration
│   │       ├── deliveries.yml                    # Delivery definitions
│   │       ├── categories.yml                    # Item categories
│   │       ├── items.yml                         # Item display names
│   │       └── locales/                          # Language files
│   └── test/
│       └── java/                                 # Unit tests
├── pom.xml                                       # Maven build configuration
├── README.md                                     # Project overview
├── BUILDING.md                                   # This file
├── LICENSE                                       # License information
└── EULA.md                                       # Minecraft EULA compliance
```

## Dependencies

| Dependency | Version | Scope | Purpose |
|-----------|---------|-------|---------|
| Spigot API | 1.16.5 | Provided | Minecraft server API |
| SnakeYAML | 2.5 | Compile (shaded) | YAML configuration parsing |
| JUnit 5 | 5.10.1 | Test | Unit testing framework |
| jqwik | 1.8.2 | Test | Property-based testing |
| AssertJ | 3.24.2 | Test | Fluent test assertions |

## IDE Setup

### IntelliJ IDEA
1. Open → Select the `pom.xml` file → Open as Project
2. Wait for Maven to index dependencies
3. Set Project SDK to Java 17+

### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Select the project root directory
3. Click Finish

## Troubleshooting

- **"Source option 17 is not supported"** — Ensure JDK 17+ is installed and set as the project SDK
- **"Cannot resolve symbol"** — Run `mvn clean install` to download all dependencies
- **Build fails with encoding errors** — Ensure all `.yml` files are saved as UTF-8 (no BOM)
