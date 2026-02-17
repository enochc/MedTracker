# MedTracker - Complete Google Play Store Deployment Guide

## 📋 Pre-Deployment Checklist

### ✅ What's Already Done
- [x] App version configured (1.0.0)
- [x] App icon generator ready
- [x] Feature graphic generator ready
- [x] Store listing text written
- [x] Privacy policy created
- [x] Release notes prepared
- [x] Screenshot instructions provided

### 🔨 What You Need To Do

## Step 1: Generate Visual Assets

### App Icon (Required)
1. Open `/play-store-assets/create_app_icon.html` in your browser
2. Click "Download app-icon-512.png"
3. Save for later upload to Play Console

### Feature Graphic (Required)
1. Open `/play-store-assets/create_feature_graphic.html` in your browser
2. Click "Download feature-graphic-1024x500.png"
3. Save for later upload to Play Console

### Screenshots (Required - Minimum 2)
1. Follow instructions in `/play-store-assets/screenshot-instructions.md`
2. Run your app on an Android device or emulator
3. Take at least 2 screenshots (home screen + widget)
4. Recommended: 4 screenshots total (home, widget, confirm dialog, history)

## Step 2: Host Privacy Policy

You need to host the privacy policy online. Here are easy options:

### Option A: GitHub Pages (Free & Easy)
1. Create a GitHub repository (e.g., `medtracker-privacy`)
2. Upload `privacy-policy.html`
3. Enable GitHub Pages in repository settings
4. Your URL will be: `https://yourusername.github.io/medtracker-privacy/privacy-policy.html`

### Option B: Other Free Hosting
- Netlify Drop
- Vercel
- Google Sites
- Any web hosting you have access to

**Important:** Save this URL - you'll need it for the Play Console.

## Step 3: Create Signing Key

Open your terminal and run:

```bash
cd /path/to/MedTracker
keytool -genkey -v -keystore medtracker-release-key.jks \
    -keyalg RSA -keysize 2048 -validity 10000 -alias medtracker
```

You'll be prompted for:
- Keystore password (choose a strong password)
- Key password (can be same as keystore password)
- Your name, organization, city, state, country

**CRITICAL:** Store these safely:
- `medtracker-release-key.jks` file (back it up!)
- Keystore password
- Key alias: `medtracker`
- Key password

**Without these, you cannot update your app in the future!**

## Step 4: Configure Signing in Gradle

Add this to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../medtracker-release-key.jks")
            storePassword = "YOUR_KEYSTORE_PASSWORD"
            keyAlias = "medtracker"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // Already configured:
            // isMinifyEnabled = true
            // proguardFiles(...)
        }
    }
}
```

**Security Note:** Never commit passwords to git. For production, use environment variables or gradle.properties.

## Step 5: Build Release App Bundle

```bash
cd /path/to/MedTracker
./gradlew bundleRelease
```

Your signed AAB will be at:
`app/build/outputs/bundle/release/app-release.aab`

This is the file you'll upload to Google Play.

## Step 6: Create Google Play Developer Account

1. Go to [Google Play Console](https://play.google.com/console)
2. Sign in with your Google account
3. Pay the **$25 one-time registration fee**
4. Complete your account details
5. Agree to the Developer Distribution Agreement

## Step 7: Create Your App in Play Console

1. Click **"Create app"**
2. Fill in:
   - **App name:** MedTracker
   - **Default language:** English (United States)
   - **App or game:** App
   - **Free or paid:** Free
3. Check the declarations boxes
4. Click **"Create app"**

## Step 8: Complete Store Listing

Navigate to **Store presence → Main store listing**

### Basic Information
- **App name:** MedTracker
- **Short description:** (Copy from `store-listing.md`)
- **Full description:** (Copy from `store-listing.md`)

### Graphics
Upload the files you generated:
- **App icon:** `app-icon-512.png` (512x512)
- **Feature graphic:** `feature-graphic-1024x500.png` (1024x500)
- **Phone screenshots:** Upload your 2-4 screenshots

### Categorization
- **App category:** Medical (or Health & Fitness)
- **Tags:** medication, health, tracker, widget

### Contact Details
- **Email:** [Your email address]
- **Website:** (Optional - your GitHub or personal site)
- **Phone:** (Optional)

### Privacy Policy
- **Privacy policy URL:** [Your hosted privacy-policy.html URL]

Click **Save**

## Step 9: Complete Content Rating

Navigate to **Policy → App content → Content ratings**

1. Click **"Start questionnaire"**
2. Enter your email address
3. Select **"Medical"** category
4. Answer the questions (typically all "No" for a medication tracker)
5. Submit and get your rating (likely Everyone/PEGI 3)

## Step 10: Complete Target Audience

Navigate to **Policy → App content → Target audience**

1. Select age groups: **All ages** (or your preference)
2. Save

## Step 11: Complete Other Policy Sections

### App Access
Navigate to **Policy → App content → App access**
- Select: "All functionality is available to all users"

### Ads
Navigate to **Policy → App content → Ads**
- Select: "No, my app does not contain ads"

### Data Safety
Navigate to **Policy → App content → Data safety**
1. Answer "No" to all data collection questions
2. Explain: "MedTracker stores all data locally on the user's device. No data is collected, transmitted, or stored on external servers."
3. Submit for review

## Step 12: Set Up Pricing & Distribution

Navigate to **Release → Countries/regions**

1. Select **"Add countries/regions"**
2. Choose **"Available everywhere"** or select specific countries
3. Ensure **"Pricing"** is set to **Free**

## Step 13: Create Production Release

Navigate to **Release → Production**

1. Click **"Create new release"**
2. Upload your AAB: `app-release.aab`
3. Review the warnings (if any) and fix if needed
4. **Release name:** `1.0.0` (or leave auto-generated)
5. **Release notes:** (Copy from `release-notes.txt`)
6. Click **"Next"**
7. Review all information
8. Click **"Start rollout to Production"**

## Step 14: Submit for Review

1. Review the summary page
2. Click **"Submit for review"**
3. Google will review your app (typically 1-7 days)
4. You'll receive an email when:
   - Review is complete
   - App is published
   - Or if changes are needed

## Step 15: After Publishing

Once approved:
- Your app will be live on Google Play
- Share your Play Store link: `https://play.google.com/store/apps/details?id=com.medtracker.app`
- Monitor the Play Console for user reviews and crash reports

## 🎉 Congratulations!

Your app is now on the Google Play Store!

## Future Updates

To release an update:

1. Update `versionCode` and `versionName` in `build.gradle.kts`
2. Build new AAB: `./gradlew bundleRelease`
3. Go to Play Console → Production → Create new release
4. Upload new AAB and add release notes
5. Submit for review

## Troubleshooting

### "Upload failed: Duplicate version"
- Increase `versionCode` in `build.gradle.kts`

### "Missing privacy policy"
- Make sure privacy policy URL is accessible and working

### "APK not signed"
- Verify signing configuration in `build.gradle.kts`
- Ensure keystore file exists and passwords are correct

### "Screenshots don't meet requirements"
- Check dimensions (min 320px, max 3840px)
- Ensure aspect ratio is between 16:9 and 9:16

## Need Help?

- Play Console Help: https://support.google.com/googleplay/android-developer
- Common Issues: https://support.google.com/googleplay/android-developer/answer/9859455

## Files Reference

All necessary files are in `/play-store-assets/`:
- `create_app_icon.html` - Generate app icon
- `create_feature_graphic.html` - Generate feature graphic
- `store-listing.md` - Store text and descriptions
- `privacy-policy.html` - Privacy policy (needs hosting)
- `release-notes.txt` - Release notes for v1.0.0
- `screenshot-instructions.md` - How to take screenshots
- `README.md` - Quick reference
- `DEPLOYMENT-GUIDE.md` - This file

Good luck with your launch! 🚀
