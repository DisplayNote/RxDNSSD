# Module: `rxdnssd` (legacy, RxJava 1)

## Purpose and boundaries

RxJava 1 (`io.reactivex:rxjava:1.3.8`, `rxandroid:1.2.1`) wrapper over [[dnssd-module]]. Kept for consumers who haven't migrated off RxJava 1. **Do not add new features here that don't also land in `rx2dnssd`** — treat `rx2dnssd` as the canonical implementation and port changes across; see Gotchas in `AGENTS.md`.

## Public API

- `RxDnssd` (`rxdnssd/src/main/java/.../rxdnssd/RxDnssd.java`) — `browse`, `resolve()` (Transformer), `queryIPRecords`/`queryIPV4Records`/`queryIPV6Records`/`queryTXTRecords` (both as Transformers and direct single-service overloads), `register`. `queryRecords()` is `@Deprecated` in favor of `queryIPRecords()`.
- `RxDnssdBindable` / `RxDnssdEmbedded` — implementations delegating to `DNSSDBindable`/`DNSSDEmbedded` from `dnssd`.
- `RxDnssdCommon` — shared logic between the two implementations.
- `BonjourService` — data model (own copy, not shared with `rx2dnssd`'s `BonjourService` — they are separate classes in separate packages).

## Upstream / downstream

- Upstream: `dnssd` (`api project(':dnssd')`, switched to Maven coordinate before publishing — see `BUILD.md`).
- Downstream: none in this repo (no other module depends on `rxdnssd`).

## Testing in isolation

```
./gradlew :rxdnssd:test
```

Test: `rxdnssd/src/test/java/.../rxdnssd/RxDnssdTest.java` (JUnit4 + Mockito + PowerMock, same stack as `dnssd`).

## Extension points / typical changes

Mirror any change made in `rx2dnssd`: same method added to `RxDnssd`, implemented in both `RxDnssdBindable` and `RxDnssdEmbedded`. Since this module predates `rx2dnssd`, when in doubt check `rx2dnssd`'s equivalent class first — it's the more actively maintained twin.
