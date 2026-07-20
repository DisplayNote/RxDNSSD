# Testing

## Test pyramid that actually exists

Unit tests only, organized per module (`dnssd`, `rxdnssd`, `rx2dnssd`) — no instrumented/UI tests, no tests in `app`. `dnssd` has three test classes (one per public entry point); `rxdnssd`/`rx2dnssd` each have one.

| Module | Test file(s) |
|---|---|
| `dnssd` | `DnssdTest.java`, `DNSSDEmbeddedTest.java`, `MulticastLockTest.java` |
| `rxdnssd` | `RxDnssdTest.java` |
| `rx2dnssd` | `Rx2DnssdTest.java` |

Stack: JUnit 4 + Mockito 3.6.0 + PowerMock 2.0.9 (`powermock-api-mockito2`, `powermock-module-junit4`). PowerMock is required because tests need to mock static/final/native calls into the `dnssd` native layer — do not upgrade Mockito/PowerMock independently of checking PowerMock's supported-Mockito-version table (linked as a comment in each `build.gradle`).

`unitTests.returnDefaultValues = true` is set in every library module's `build.gradle` — it only stubs unmocked Android SDK calls under the JVM test runner. `InternalDNSSD`'s native methods are handled separately: PowerMock mocks/suppresses them (`@PrepareForTest`/`mockStatic`) so tests never hit the real JNI layer.

## Running tests

```bash
./gradlew test                 # all modules
./gradlew :dnssd:test           # single module
./gradlew :rxdnssd:test
./gradlew :rx2dnssd:test
./gradlew check                 # what CircleCI runs (circle.yml)
```

## Adding a new test

Follow the existing per-module pattern: one test class per public entry point (`DnssdTest` mirrors `DNSSD`/`InternalDNSSD`, `RxDnssdTest`/`Rx2DnssdTest` mirror their respective wrapper interface). Mock the native layer via PowerMock rather than trying to run real mDNS traffic in a unit test.

## CI

CircleCI (`circle.yml`) runs `./gradlew androidDependencies` (cache warm) then `./gradlew check`, and uploads `build/reports` + `build/test-results` for `dnssd`, `rxdnssd`, `rx2dnssd` (not `app`). There is no GitHub Actions workflow under `.github/workflows/`; `.github/` also holds an issue template and `copilot-instructions.md`.
