# Module: `rx2dnssd` (RxJava 2, recommended)

## Purpose and boundaries

RxJava 2 (`io.reactivex.rxjava2:rxjava:2.2.21`, `rxandroid:2.1.1`) wrapper over [`dnssd`](dnssd.md). This is the module new integrations should use — the sample `app` module depends on it, not on `rxdnssd`.

## Public API

- `Rx2Dnssd` (`rx2dnssd/src/main/java/.../rx2dnssd/Rx2Dnssd.java`) — same operation set as `RxDnssd` but returning `Flowable`/`FlowableTransformer` instead of `Observable`/`Observable.Transformer`.
- `Rx2DnssdBindable` / `Rx2DnssdEmbedded` — implementations delegating to `DNSSDBindable`/`DNSSDEmbedded`.
- `Rx2DnssdCommon` — shared logic between the two implementations.
- `BonjourService` — own data model, structurally similar to `rxdnssd`'s but not interchangeable (different package, no shared base class).

## Upstream / downstream

- Upstream: `dnssd` (`api project(':dnssd')`, switched to Maven coordinate before publishing — see `BUILD.md`).
- Downstream: `app` (`implementation project(':rx2dnssd')`).

## Testing in isolation

```
./gradlew :rx2dnssd:test
```

Test: `rx2dnssd/src/test/java/.../rx2dnssd/Rx2DnssdTest.java` (JUnit4 + Mockito + PowerMock).

## Extension points / typical changes

- Adding a new query/browse variant: add to `Rx2Dnssd`, implement in both `Rx2DnssdBindable` and `Rx2DnssdEmbedded`, then port the same addition to `rxdnssd`'s `RxDnssd` (see [`rxdnssd`](rxdnssd.md)) unless it's Flowable-specific.
- Example usage patterns to follow are in `app/src/main/java/.../dnssdsamples/MainActivity.java` — `browse().compose(resolve()).compose(queryIPRecords())` chain, subscribed on `Schedulers.io()`, observed on `AndroidSchedulers.mainThread()`.
