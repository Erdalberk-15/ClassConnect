# Image Guide for ExampleLauncher

## Required Images

You need to add 3 images to this folder:
`src/main/resources/com/examplelauncher/examplelauncher/images/`

---

## 1. icon.png (Logo/Icon)
**Location:** Top right corner of login and register screens

**Recommended Size:** 
- **256x256 pixels** (will be displayed as 100x100)
- Format: PNG with transparent background
- Square aspect ratio (1:1)

**Design Tips:**
- Use your server logo or launcher icon
- Keep it simple and recognizable
- Transparent background works best
- High resolution for crisp display

---

## 2. login_background.png
**Location:** Full background of login screen

**Recommended Size:**
- **1920x1080 pixels** (Full HD)
- Or **1280x720 pixels** (HD)
- Format: PNG or JPG
- Aspect ratio: 16:9

**Design Tips:**
- Use darker images for better text readability
- Can be a Minecraft screenshot, server spawn, or custom design
- Add blur or dark overlay for better contrast with white text
- Keep important content away from center (where form appears)

---

## 3. register_background.png
**Location:** Full background of register screen

**Recommended Size:**
- **1920x1080 pixels** (Full HD)
- Or **1280x720 pixels** (HD)
- Format: PNG or JPG
- Aspect ratio: 16:9

**Design Tips:**
- Similar style to login background but different image
- Darker images work better with white text
- Can be another Minecraft screenshot or server area
- Keep center area clear for the registration form

---

## How to Add Images

### Step 1: Prepare Your Images
1. Create or download your images
2. Resize them to recommended dimensions
3. Name them exactly as:
   - `icon.png`
   - `login_background.png`
   - `register_background.png`

### Step 2: Add to Project
Copy the images to:
```
src/main/resources/com/examplelauncher/examplelauncher/images/
```

Your folder structure should look like:
```
src/main/resources/com/examplelauncher/examplelauncher/
├── images/
│   ├── icon.png
│   ├── login_background.png
│   └── register_background.png
├── views/
│   ├── login.fxml
│   ├── register.fxml
│   └── launcher.fxml
└── css/
    └── style.css
```

### Step 3: Rebuild
After adding images:
```cmd
gradlew.bat clean jar
```

Then rebuild your EXE with Launch4j.

---

## Image Optimization Tips

### For Backgrounds:
- Use JPG for photos (smaller file size)
- Use PNG for graphics with transparency
- Compress images to reduce launcher size
- Recommended tools:
  - TinyPNG: https://tinypng.com/
  - Squoosh: https://squoosh.app/

### For Icon:
- Always use PNG with transparency
- Keep file size under 500KB
- Use high contrast colors
- Test on dark and light backgrounds

---

## Example Image Sources

### Free Minecraft Backgrounds:
- Minecraft Screenshots (your own server)
- Unsplash: https://unsplash.com/s/photos/minecraft
- Wallpaper sites with Minecraft themes

### Icon/Logo:
- Create with Photoshop/GIMP
- Use Canva: https://www.canva.com/
- Minecraft head renders
- Server logo

---

## Current Screen Sizes

The launcher windows are:
- **Width:** 800 pixels
- **Height:** 600 pixels

But backgrounds should be larger (1920x1080) for better quality when scaled.

---

## Testing

After adding images, run the launcher to check:
1. ✅ Icon appears in top right corner
2. ✅ Background covers entire screen
3. ✅ Text is readable over background
4. ✅ Images load without errors

If images don't appear:
- Check file names (case-sensitive!)
- Verify folder path
- Rebuild JAR file
- Check console for errors

---

## Quick Checklist

- [ ] icon.png (256x256, transparent PNG)
- [ ] login_background.png (1920x1080, JPG/PNG)
- [ ] register_background.png (1920x1080, JPG/PNG)
- [ ] All images in correct folder
- [ ] Rebuilt JAR file
- [ ] Tested in launcher

**That's it! Your launcher will look professional with custom images!** 🎨
