# Troubleshooting

## `JAVA_HOME is set to an invalid directory` even though the directory exists (Windows / Git Bash)

Cause: the `JAVA_HOME` environment variable contains literal double-quote characters as part of its value (e.g. it was set from a quoted Windows path string), so `test -d "$JAVA_HOME"` fails even though the unquoted path is a real directory.

Fix:
```bash
export JAVA_HOME='C:/Program Files/Microsoft/jdk-11.0.25.9-hotspot'
```
(no embedded quotes in the value itself, and slash-separated so `./gradlew`'s `"$JAVA_HOME/bin/java"` check resolves correctly under Git Bash). Verify with `test -x "$JAVA_HOME/bin/java" && echo ok`. Note the Gradle 7.0.2 wrapper caps at Java 16 — JDK 17+ fails with an "Unsupported Java" error, so use a JDK 11–16 build here.

## `JNI DETECTED ERROR IN APPLICATION: java_class == null`

Fixed historically in commit `7804cc6` (`DNSSD$1.serviceRegistered` path) — if you see this again, check that the JNI global refs to Java classes in `JNISupport.c`/`DNSSD.java.h` are being initialized before any callback fires, not lazily on first use from a native thread.

## Bindable implementation doesn't discover anything on Android 12+

Expected — Google deprecated the system mDNS daemon starting at `targetSDK 31` (see `README.md`). Switch to `DNSSDEmbedded`/`Rx2DnssdEmbedded` for devices targeting API 31+.

## Native (`dnssd`) module fails to build with an NDK error

Check the NDK version installed matches `ndkVersion "28.2.13676358"` in `dnssd/build.gradle` (and `app/build.gradle`). `externalNativeBuild.ndkBuild` in `dnssd/build.gradle` points at `src/main/jni/Android.mk`; a missing/wrong NDK side-by-side install is the most common cause of `ndk-build` invocation failures.

## Publishing pushes the wrong `dnssd` version (project vs. Maven coordinate)

Check whether `rxdnssd/build.gradle` / `rx2dnssd/build.gradle` still have `api project(':dnssd')` active instead of the Maven-coordinate line — see `docs/runbooks/release.md` step 2. This is a manual toggle with no build-time guard.
