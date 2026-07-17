# Module: `dnssd`

## Purpose and boundaries

Lowest-level module. Provides a plain-Java DNS-SD API (`DNSSD` interface) plus the native mDNSResponder core that backs it. No RxJava dependency — `rxdnssd` and `rx2dnssd` both wrap this module and add nothing to it that isn't reactive plumbing.

What belongs here: the `DNSSD` contract, its two implementations (`DNSSDBindable`, `DNSSDEmbedded`), the JNI bridge, and the vendored native core.
What doesn't belong here: anything RxJava-specific, anything Android-app-specific (that's `app`).

## Public API

- `DNSSD` (`dnssd/src/main/java/.../dnssd/DNSSD.java`) — interface with `register`, `browse`, `resolve`, `queryRecord`, `createRecordRegistrar`.
- `DNSSDBindable` — implementation bound to the system `mdnsd`/`nsd` service (requires a `Context`). Deprecated on API 31+ by the OS itself (see `README.md`).
- `DNSSDEmbedded` — implementation using the bundled native core, no `Context` required, works API 14+.
- Listener interfaces: `BrowseListener`, `RegisterListener`, `ResolveListener`, `QueryListener`, `DomainListener`, `RegisterRecordListener`.
- `TXTRecord`, `DNSSDException`, `NSClass`, `NSType` — supporting value types.
- `DNSSDRecordRegistrar` / `DNSSDRegistration` / `DNSSDService` — handles for in-flight operations.

Everything under `Internal*` (`InternalDNSSD`, `InternalBrowseListener`, etc.) is package-private plumbing between the public API and the native layer — not part of the public contract, do not reference it from `rxdnssd`/`rx2dnssd` or `app`.

## Native layer

- `dnssd/src/main/jni/Android.mk` — ndk-build script, builds `jdns_sd_embedded` (full mDNSCore + Posix + JNISupport) for the Embedded path.
- `dnssd/src/main/jni/JNISupport.c` + `DNSSD.java.h` — hand-written JNI bridge. This is the only native file DisplayNote maintains directly.
- `dnssd/src/main/jni/mdnsresponder/` — **vendored** copy of Apple's open-source mDNSResponder (mDNSCore, mDNSPosix, mDNSShared, plus unused mDNSWindows/Clients trees kept for reference). Do not hand-edit; see Gotchas in `AGENTS.md`.
- `ndkVersion "28.2.13676358"` pinned in `dnssd/build.gradle`.

## Upstream / downstream dependencies

- Upstream: none inside this repo (root of the dependency graph).
- Downstream: `rxdnssd`, `rx2dnssd` (both `api project(':dnssd')`), `app` (transitively via `rx2dnssd`).

## Testing in isolation

```
./gradlew :dnssd:test
```

Tests live in `dnssd/src/test/java/.../dnssd/` (`DnssdTest`, `DNSSDEmbeddedTest`, `MulticastLockTest`) and use JUnit4 + Mockito + PowerMock (needed to mock static/native calls — see `powermock-api-mockito2` in `dnssd/build.gradle`). `unitTests.returnDefaultValues = true` is set because native methods can't run on the JVM test runner.

## Extension points / typical changes

- Adding a new DNS-SD operation: extend the `DNSSD` interface, then implement it in both `DNSSDBindable` and `DNSSDEmbedded`, then add the native method + JNI glue in `InternalDNSSD`/`JNISupport.c` if the Embedded path needs it.
- Bumping the vendored mDNSResponder: replace files under `mdnsresponder/` from upstream Apple source; re-check `Android.mk` source lists still match; do not carry over unrelated Apple changes to Windows/Clients trees.
