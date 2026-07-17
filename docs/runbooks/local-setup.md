# Local setup

## Prerequisites

- Android SDK with `compileSdkVersion 31` platform installed (module `app`; other modules use 30).
- Android NDK **28.2.13676358** (pinned in `app/build.gradle` and `dnssd/build.gradle` via `ndkVersion`). `dnssd` needs the NDK because it builds native mDNSResponder code via `ndkBuild`.
- JDK 17 minimum (no upper bound pinned; repo was built/tested with `jdk-17.0.7.7-hotspot` locally).
- `local.properties` with `sdk.dir` pointing at your Android SDK (gitignored, create your own).

## Windows / Git Bash gotcha

If `./gradlew` fails with:
```
ERROR: JAVA_HOME is set to an invalid directory: "C:\Program Files\Microsoft\jdk-17.0.7.7-hotspot"
```
even though that directory exists, check whether `JAVA_HOME` in your shell holds **literal quote characters** as part of the value (common when it's set from a Windows `setx`/registry value copied with quotes into a Git Bash profile). Fix by re-exporting without quotes and with slash-separated path (so `./gradlew`'s `"$JAVA_HOME/bin/java"` check resolves under Git Bash):
```bash
export JAVA_HOME='C:/Program Files/Microsoft/jdk-17.0.7.7-hotspot'
```
See also `docs/runbooks/troubleshooting.md`.

## Clone and build

```bash
git clone <this repo>
cd RxDNSSD
./gradlew clean build
```

`BUILD.md` has the full local-vs-publish dependency toggle instructions — read it before your first build, since `rxdnssd/build.gradle` and `rx2dnssd/build.gradle` ship with the **local** `api project(':dnssd')` dependency active by default (correct for local dev, must be switched before publishing).

## Run the sample app

1. Open the repo in Android Studio (it will pick up `settings.gradle`'s four modules: `app`, `dnssd`, `rxdnssd`, `rx2dnssd`).
2. Run configuration: `app` module, deploy to a device/emulator on the same Wi-Fi/LAN as another mDNS advertiser to see browse results (mDNS is multicast — emulator NAT/bridged networking must support multicast, which not all emulator network modes do).
