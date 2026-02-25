# The four primitives and Weapons - Build Guide

Minecraft Forge 1.20.1 Mod (Java 17)

## Build Commands

### Compile (check for errors)
```bash
./gradlew compileJava
```

### Build (generate jar)
```bash
./gradlew build
```
Output: `build/libs/The four primitives and Weapons-forge-1.20.1--1.0.jar`

### Clean Build
```bash
./gradlew clean build
```

## Run (Test Play)

### Terminal
```bash
./gradlew runClient

bash run_client.sh

```

### VSCode (F5 Debug)
1. `F5` or Run > Start Debugging
2. Select **runClient**
3. Minecraft launches with mod loaded + breakpoint support

### VSCode Task (Ctrl+Shift+B)
- **Build Project** - `./gradlew build`
- **Run Client** - `./gradlew runClient`
- **Run Server** - `./gradlew runServer`

### VSCode Run Config Regeneration
```bash
./gradlew genVSCodeRuns
```

## Server
```bash
./gradlew runServer
```

## Environment

| Item | Value |
|------|-------|
| Minecraft | 1.20.1 |
| Forge | 47.1.0 |
| Java | JDK 17 (`C:\Program Files\Java\jdk-17`) |
| Gradle | 7.5 (wrapper) |

### Dependencies

| Library | Version |
|---------|---------|
| GeckoLib | 4.7.3 (forge-1.20.1) |
| Curios | 5.x (forge-1.20.1) |
| JEI | 15.x (forge-1.20.1) |

## Troubleshooting

### Build Errors
```bash
./gradlew clean
./gradlew build --stacktrace
```

### Gradle Cache Clear
```bash
rm -rf ~/.gradle/caches/forge_gradle
./gradlew build
```

### WSL (Linux)
WSL does not have Java. Use Windows terminal (PowerShell/cmd/VSCode terminal).
```cmd
.\gradlew.bat build
```
