---
inclusion: always
---

# Tech

Android/Java 8, Gradle (AGP 7.0.3), native C via NDK 28.2.13676358 (vendored Apple mDNSResponder), RxJava 1 and RxJava 2 in parallel wrapper modules.

Build/test:
```bash
./gradlew clean build
./gradlew test
./gradlew :dnssd:test   # or :rxdnssd:test / :rx2dnssd:test
```

Read `AGENTS.md` "Run / build / test / lint" and `docs/runbooks/local-setup.md` (NDK version, JDK, Windows `JAVA_HOME` gotcha) before your first build.

Never hand-edit `dnssd/src/main/jni/mdnsresponder/` — vendored third-party source.
