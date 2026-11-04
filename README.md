# Minesweeper (Android)

Classic Minesweeper for Android 8.0 and up, in portrait.

## Controls
- **Tap** a square to reveal it. Tap a revealed number whose flags are all placed to reveal the rest of its neighbours (chording).
- **Long press** to place or remove a flag.
- **Pinch** to zoom, **drag** to pan.
- Tap the **face** to start a new game.

## Get the APK (no software needed on your computer)

1. Create a free account at https://github.com if you don't have one.
2. Click **+ → New repository**. Name it `minesweeper`, leave it **Public**, and click **Create repository**.
3. On the new repository page, click **uploading an existing file**.
4. Unzip `minesweeper-project.zip` on your computer, open the `minesweeper` folder, select everything inside it (including the hidden `.github` folder) and drag it into the upload page. Wait for the upload to finish, then click **Commit changes**.
   - If the `.github` folder doesn't appear (some file browsers hide it), see "Hidden folder" below.
5. Click the **Actions** tab. A run called **Build APK** starts by itself and takes about 5 to 8 minutes. Wait for the green tick.
6. Go back to the repository's main page and click **Releases** on the right. Open the newest release and tap **Minesweeper.apk** to download it.
7. On your phone, open the downloaded file. Android will ask you to allow installs from this source the first time. Allow it, then install.

You can also download the APK on your computer and send it to your phone.

### Hidden folder
If `.github` didn't upload: in the repository click **Add file → Create new file**, type `.github/workflows/build.yml` as the name, paste in the contents of that file from the zip, and click **Commit changes**.

### If the build shows a red cross
Click the failed run, open the failed step and copy the red error text to Claude.

## Updating the app
Upload changed files to the same repository. Every upload builds a new release. New APKs are signed with the same key, so they install over the old one and keep your saved game and stats.

## Notes
- The signing key is included in the project on purpose so updates install over each other. It is fine for a personal app; don't use it for the Play Store.
- Game rules are covered by unit tests in `app/src/test`, which run during every build.
