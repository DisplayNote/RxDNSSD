# Testing

## Test pyramid that actually exists

Unit tests only, organized per module (`dnssd`, `rxdnssd`, `rx2dnssd`) — no instrumented/UI tests, no tests in `app`.

| Module | Test file(s) |
|---|---|
| `dnssd` | `DnssdTest.java`, `DNSSDEmbeddedTest.java` (`@Ignore`d, does not run in `./gradlew test`/`check`), `MulticastLockTest.java`, `TXTRecordTest.java`, `AppleDNSSDExceptionTest.java` |
| `rxdnssd` | `RxDnssdTest.java`, `BonjourServiceTest.java`, `RxRegisterListenerTest.java`, `RxQueryListenerTest.java` |
| `rx2dnssd` | `Rx2DnssdTest.java`, `BonjourServiceTest.java`, `Rx2RegisterListenerTest.java`, `Rx2QueryListenerTest.java` |

- `DnssdTest`, `MulticastLockTest`, `RxDnssdTest`, `Rx2DnssdTest` — the public entry points, with the native layer mocked through PowerMock.
- `TXTRecordTest` — the TXT wire format (length-prefixed `key=value`), key lookup ignoring case, replace in place, remove, the 255-byte limit, and `DNSSD.parseTXTRecords`.
- `AppleDNSSDExceptionTest` — the `DNS-SD Error <code>: <NAME>` message for every known error code.
- `BonjourServiceTest` (same file in both Rx modules, as `BonjourService` is duplicated) — builder and copy builder, equality on name/type/domain/interface only, `isLost`, IPv4/IPv6 lookup, unmodifiable records and addresses, `toString`. `Parcelable` is not covered (needs Android).
- `Rx*RegisterListenerTest`, `Rx*QueryListenerTest` — the stream events for each callback: emitted service, A/AAAA/TXT answers accumulating on one service, completion, errors, and nothing emitted once cancelled/unsubscribed.

Stack: JUnit 4 + Mockito 3.6.0 + PowerMock 2.0.9 (`powermock-api-mockito2`, `powermock-module-junit4`). PowerMock is required where tests mock static/final/native calls into the `dnssd` native layer — do not upgrade Mockito/PowerMock independently of checking PowerMock's supported-Mockito-version table (linked as a comment in each `build.gradle`). Pure-Java classes are tested with plain JUnit + Mockito (rx1 tests use `rx.observers.TestSubscriber`).

`unitTests.returnDefaultValues = true` is set in every library module's `build.gradle` — it only stubs unmocked Android SDK calls under the JVM test runner (so `TextUtils.isEmpty` returns `false` in tests). `InternalDNSSD`'s native methods are handled separately: PowerMock mocks/suppresses them (`@PrepareForTest`/`mockStatic`) so tests never hit the real JNI layer.

## Running tests

Gradle 7.0.2 needs JDK 11–16. The root `build.gradle` reads the Artifactory URL and credentials (`publish-root.gradle`) while it is configured, so they must be set even for a test run. Without Artifactory access, any non-empty values do: every dependency of the libraries and their tests comes from `google()`/`mavenCentral()`, which are searched first.

```bash
export DEPLOYER_ARTIFACTORY_URL=https://artifactory.invalid/artifactory
export MAVEN_ARTIFACTORY_USERNAME=unused MAVEN_ARTIFACTORY_PASSWORD=unused
export DEPLOYER_ARTIFACTORY_USERNAME=unused DEPLOYER_ARTIFACTORY_PASSWORD=unused

./gradlew test                 # all modules
./gradlew :dnssd:test           # single module
./gradlew :rxdnssd:test
./gradlew :rx2dnssd:test
./gradlew check                 # unit tests + lint
```

The unit tests don't run `ndk-build`; only `assembleRelease`/`publish` need the NDK.

### Coverage

```bash
./gradlew :dnssd:testDebugUnitTest :rxdnssd:testDebugUnitTest :rx2dnssd:testDebugUnitTest
./gradlew :dnssd:jacocoTestReport -x :dnssd:testDebugUnitTest \
          :rxdnssd:jacocoTestReport -x :rxdnssd:testDebugUnitTest \
          :rx2dnssd:jacocoTestReport -x :rx2dnssd:testDebugUnitTest
```

`jacocoTestReport` (defined in `jacoco-module.gradle`, applied by the three libraries) writes
`<module>/build/reports/jacoco/jacocoTestReport/jacocoTestReport.{xml,html}` from the `testDebugUnitTest` run. `testCoverageEnabled` is on for the debug build type: with AGP 7.0 that instruments the debug classes offline and the run writes `build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec`. Offline instrumentation is what makes code run under the PowerMock runner count. JaCoCo is pinned to 0.8.7 for both AGP and Gradle; with two versions the `.exec` stays empty. `R`, `BuildConfig` and the manifest classes are not counted. What the figure measures is in the README, under *Coverage scope*.

### In CI

`ci/azure-pipelines.yml` runs the same tasks in the `tests_coverage_java` stage (`android-gradle/tests-coverage.yml` from qt-conan-ci, JDK 11), publishes the JUnit results and the JaCoCo reports to the run and checks line coverage against the 75% target. It is in audit mode (`blocking: false`): failing tests or a figure below target are warnings and the build still runs. Set `blocking: true` once the suite has been green on five consecutive `master` runs; from then on a PR must not lower the figure.

## Suspected bugs pinned by the tests

The tests pin today's behaviour; these look wrong and are left for a separate fix:

- **`TXTRecord` reads the length byte as signed** in `getKey`, `getValue` and `remove` (only `size` masks it). A pair of 128–254 bytes, which `set` accepts, reads back as an empty key with no value, `remove` cannot find it and `getValue(key)` throws `ArrayIndexOutOfBoundsException`. Same code as Apple's original. `TXTRecordTest.pairLongerThan127BytesReadsBackAsAnEmptyKey`.
- **The query listeners emit after an error.** For a malformed A/AAAA answer or an unsupported record type, `Rx2QueryListener` calls `tryOnError` and `RxQueryListener` calls `onError`, then both still emit the service (and complete when completable) — for rx1 that breaks the Observable contract. `Rx*QueryListenerTest.*ReportsAnErrorAndStillEmits`.
- **`BonjourService.Builder.dnsRecords(map)` keeps the caller's map**: changing it after `build()` changes the service's TXT records. `BonjourServiceTest.builtServiceSharesTheMapPassedToDnsRecords`.
- **Register failures say "DNSSD browse error"** (`Rx*RegisterListener.operationFailed`).
- **`AppleDNSSDException` message for an unknown code** starts with `null`, e.g. `null(0)` (cosmetic).

## Adding a new test

Put tests in the module's `src/test/java`, in the package of the class under test, name them after the behaviour they check, and prefer plain JUnit + Mockito; mock the native layer via PowerMock (as `DnssdTest` does) only when the code calls into `InternalDNSSD`. Never try to run real mDNS traffic in a unit test.

## CI

Azure Pipelines (`ci/azure-pipelines.yml`): SecretScan + DependencyScan, version, `tests_coverage_java` (above), `assembleRelease` of the three libraries (NDK 28.2.13676358), and `publish` to Artifactory on tag builds only. The upstream `circle.yml` was removed: it never ran on this fork. `.github/workflows/` holds the docs-sync automation (roadmap 2.30, nightly/on-demand documentation upkeep) rather than build or test CI — see `tools/docs-sync/README.md`.
