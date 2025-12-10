# Creating ExampleLauncher.exe with Launch4j

## Your JAR is Ready!
✅ Location: `build/libs/ExampleLauncher-1.0-SNAPSHOT.jar`

---

## Step 1: Download Launch4j

1. Go to: https://launch4j.sourceforge.net/
2. Download: **Launch4j 3.x** (Windows version)
3. Extract to a folder (e.g., `C:\Launch4j`)
4. Run `launch4j.exe`

---

## Step 2: Configure Launch4j

### Basic Tab:
1. **Output file:** 
   - Click `...` button
   - Choose location: `C:\ModDevelopmentKit\ExampleLauncher\ExampleLauncher.exe`

2. **Jar:**
   - Click `...` button
   - Select: `C:\ModDevelopmentKit\ExampleLauncher\build\libs\ExampleLauncher-1.0-SNAPSHOT.jar`

3. **Icon:** (Optional)
   - Click `...` button
   - Select your `.ico` file (if you have one)
   - Or leave empty for default Java icon

### JRE Tab:
1. **Min JRE version:** `21`
2. **Max JRE version:** Leave empty
3. **JRE path:** Leave empty (will use system Java)

### Version Info Tab: (Optional but recommended)
1. **File version:** `1.0.0.0`
2. **Product version:** `1.0.0.0`
3. **File description:** `ExampleLauncher - Minecraft Launcher`
4. **Product name:** `ExampleLauncher`
5. **Company name:** Your name
6. **Copyright:** `Copyright © 2024`

---

## Step 3: Build the EXE

1. Click the **gear icon** (⚙️) at the top
2. Or go to: **Build wrapper** menu
3. Wait for "Successfully created..."
4. Done! Your `ExampleLauncher.exe` is ready!

---

## Step 4: Test Your EXE

1. Navigate to: `C:\ModDevelopmentKit\ExampleLauncher\`
2. Double-click `ExampleLauncher.exe`
3. Your launcher should start!

---

## Step 5: Distribution

To distribute your launcher, users need:

### Option A: EXE + JAR (Simple)
Give users:
- `ExampleLauncher.exe`
- `ExampleLauncher-1.0-SNAPSHOT.jar` (in same folder)
- Java 21 installed

### Option B: EXE Only (Advanced)
Use Launch4j's "Bundle JRE" option:
1. In Launch4j, go to **JRE** tab
2. Check "Bundle JRE"
3. Specify path to JRE folder
4. Rebuild

---

## Troubleshooting

### "Java not found" error:
- Users need Java 21 installed
- Download from: https://adoptium.net/

### "JAR not found" error:
- Make sure `.jar` and `.exe` are in same folder
- Or use absolute path in Launch4j config

### EXE doesn't start:
- Check if Java 21 is installed: `java -version`
- Try running JAR directly: `java -jar ExampleLauncher-1.0-SNAPSHOT.jar`

---

## Creating an Icon (Optional)

If you want a custom icon:

1. Create or download a PNG image (256x256 recommended)
2. Convert to ICO format:
   - Use: https://convertio.co/png-ico/
   - Or: https://www.icoconverter.com/
3. Save as `icon.ico`
4. In Launch4j, select this icon file

---

## Launch4j Configuration File

Launch4j can save your configuration. Here's a template:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<launch4jConfig>
  <dontWrapJar>false</dontWrapJar>
  <headerType>gui</headerType>
  <jar>build\libs\ExampleLauncher-1.0-SNAPSHOT.jar</jar>
  <outfile>ExampleLauncher.exe</outfile>
  <errTitle>ExampleLauncher</errTitle>
  <cmdLine></cmdLine>
  <chdir>.</chdir>
  <priority>normal</priority>
  <downloadUrl>https://adoptium.net/</downloadUrl>
  <supportUrl></supportUrl>
  <stayAlive>false</stayAlive>
  <restartOnCrash>false</restartOnCrash>
  <manifest></manifest>
  <icon></icon>
  <jre>
    <path></path>
    <bundledJre64Bit>false</bundledJre64Bit>
    <bundledJreAsFallback>false</bundledJreAsFallback>
    <minVersion>21</minVersion>
    <maxVersion></maxVersion>
    <jdkPreference>preferJre</jdkPreference>
    <runtimeBits>64/32</runtimeBits>
  </jre>
  <versionInfo>
    <fileVersion>1.0.0.0</fileVersion>
    <txtFileVersion>1.0.0</txtFileVersion>
    <fileDescription>ExampleLauncher - Minecraft Launcher</fileDescription>
    <copyright>Copyright © 2024</copyright>
    <productVersion>1.0.0.0</productVersion>
    <txtProductVersion>1.0.0</txtProductVersion>
    <productName>ExampleLauncher</productName>
    <companyName></companyName>
    <internalName>ExampleLauncher</internalName>
    <originalFilename>ExampleLauncher.exe</originalFilename>
  </versionInfo>
</launch4jConfig>
```

Save this as `launch4j-config.xml` and load it in Launch4j!

---

## Quick Summary

1. ✅ JAR built: `build/libs/ExampleLauncher-1.0-SNAPSHOT.jar`
2. Download Launch4j
3. Configure: Output file + JAR path + Min JRE 21
4. Click Build
5. Done! You have `ExampleLauncher.exe`

**That's it! Your launcher is now a professional Windows executable!**
