# How to Build ExampleLauncher.exe

## Method 1: Using jlink (Recommended)

### Step 1: Build the application
```cmd
gradlew.bat clean build
```

### Step 2: Create runtime image with jlink
```cmd
gradlew.bat jlink
```

This creates a custom JRE with your app in:
`build/image/`

### Step 3: Run the launcher
```cmd
build\image\bin\ExampleLauncher.bat
```

---

## Method 2: Create Installer with jpackage

### Requirements:
- Install WiX Toolset: https://wixtoolset.org/releases/
- Add WiX to PATH

### Build installer:
```cmd
gradlew.bat jpackage
```

This creates an installer in:
`build/jpackage/`

---

## Method 3: Simple JAR with Batch File (Easiest)

### Step 1: Build JAR
```cmd
gradlew.bat jar
```

### Step 2: Create launcher.bat
Create a file named `ExampleLauncher.bat`:
```batch
@echo off
java -jar build\libs\ExampleLauncher-1.0-SNAPSHOT.jar
```

### Step 3: Convert BAT to EXE (Optional)
Use Bat To Exe Converter: http://www.f2ko.de/en/b2e.php

---

## Method 4: Using Launch4j (Best for EXE)

### Step 1: Download Launch4j
https://launch4j.sourceforge.net/

### Step 2: Build fat JAR first
Add to build.gradle.kts:
```kotlin
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.examplelauncher.examplelauncher.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
```

Build:
```cmd
gradlew.bat jar
```

### Step 3: Configure Launch4j
1. Open Launch4j
2. Output file: `ExampleLauncher.exe`
3. Jar: `build/libs/ExampleLauncher-1.0-SNAPSHOT.jar`
4. Icon: (optional) your .ico file
5. JRE minimum version: 21
6. Click "Build wrapper"

---

## Quick Method (For Testing)

Just run:
```cmd
gradlew.bat run
```

This runs the launcher directly without creating EXE.

---

## Recommended Approach:

1. **For Development**: Use `gradlew.bat run`
2. **For Distribution**: Use jlink + jpackage (creates installer)
3. **For Simple EXE**: Use Launch4j

The jpackage method creates a professional installer that includes everything users need!
