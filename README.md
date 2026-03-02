# I_Tuoi_Versetti

I wrote this project to help people sharing their favourite bible verse. 
This app is not meant to be used in substitution of official JW Library app developed under official channel: Play Store. 

The code can be used for study purpose in harmony with open-source philosophy, GPL compliance.

The use of Jsoup libraries is under terms of condition.

Author is Pasquale Edmondo Lombardi, you can contact me at email plombardi85@gmail.com

Hopefully, if this work will please God, i'll have soon a google developer account to make a serious developing project.

This is my personal effort to help the purpose of global instruction about Jehovah and his wonderful purpose for humankind.
All the things we give comes from God and to God we give back, so if i cannot help people to increase faith, i can
do my little effort to increase knowledge hoping we can all be together working to make the Kingdom of God known.

## Signed release build (Play Store)

### One-command automatic setup (recommended)

Run:

```
powershell -ExecutionPolicy Bypass -File .\tools\release-build.ps1
```

This script will:
- generate `release-key.jks` if missing,
- generate `keystore.properties` (ASCII, no BOM) if missing,
- run signed `:app:bundleRelease`,
- write logs to `release_build.log`.

If successful, AAB is at:

`app/build/outputs/bundle/release/app-release.aab`

### GitHub Actions (build signed AAB in cloud)

If local Gradle crashes on Windows, use CI build:

Fast path (automatic secrets prep):

```
powershell -ExecutionPolicy Bypass -File .\tools\prepare-github-secrets.ps1
```

This creates `github-secrets.local.txt` with all 4 values ready to paste in GitHub Secrets.

Automatic upload from PowerShell (no Store required):

```
powershell -ExecutionPolicy Bypass -File .\tools\install-gh-cli.ps1
powershell -ExecutionPolicy Bypass -File .\tools\upload-github-secrets.ps1
```

1. Add repository secrets:
	- `ANDROID_KEYSTORE_BASE64`
	- `ANDROID_STORE_PASSWORD`
	- `ANDROID_KEY_ALIAS`
	- `ANDROID_KEY_PASSWORD`

2. Convert keystore to base64 (PowerShell):

	```
	[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\release-key.jks"))
	```

3. Push a tag (example):

	```
	git tag v0.1.0
	git push origin v0.1.0
	```

	Or run manually from Actions tab (`workflow_dispatch`).

Workflow file: `.github/workflows/android-release-aab.yml`

1. Create a keystore (example):

	keytool -genkeypair -v -keystore release-key.jks -alias ituoiversetti -keyalg RSA -keysize 2048 -validity 10000

2. Copy `keystore.properties.example` to `keystore.properties` and set real values.

3. Build signed release bundle (recommended for Play Store):

	.\gradlew.bat :app:bundleRelease --no-daemon --stacktrace

4. Optional signed APK build:

	.\gradlew.bat :app:assembleRelease --no-daemon --stacktrace

`keystore.properties` and keystore files are git-ignored by default.
