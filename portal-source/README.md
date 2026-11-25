<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/1eaee2e6-a2b4-4b98-a823-0ccf82508943

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## Android app releases

Super Admins can manage APK releases at `/app-releases` in the Web Admin. Uploads are stored in the existing Firebase Storage bucket under `app_release_packages/`; release metadata and status are stored in the existing named Firestore database under `app_releases`. Only published releases appear at `/download` or in the Android app's update banner. Publishing, unpublishing, and uploads write immutable entries to `audit_logs`; release files and records are not deleted by the manager.

Upload only an APK signed with the production release key. The release signing configuration uses the `upload` alias and reads `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` from the build environment. Keep the keystore and passwords in a secure local or CI secret store; never commit them. The repository's `debug.keystore` and debug-signed APK are not suitable for production updates.

The local `firestore.rules` and `storage.rules` include the release access controls. They must be deployed to the existing Firebase project before release upload/download is enabled. Production deployment is intentionally not performed as part of this implementation.
