# Module: `app` (sample application)

## Purpose and boundaries

Demo Android app (`com.github.druk.rxdnssd`, `applicationId`) showing how to use `rx2dnssd` from real Android UI code. This is a development/testing aid only, never intended for public release — it exists purely for manual verification of the library and as a living usage example (see `README.md`'s "You can find more samples in app inside this repository"). It carries no versioning or release process of its own — see `docs/runbooks/release.md`.

## Public API

None — it's an application, not a library. Key classes:
- `MainActivity` — entry point / permission handling, canonical reference for chaining `browse().compose(resolve()).compose(queryIPRecords())`.
- `DNSSDActivity` — browse/resolve/register/register-service demo using the lower-level, listener-based `dnssd` API directly.
- `ServiceAdapter` — RecyclerView/ListView adapter for discovered `BonjourService` items.

## Upstream / downstream

- Upstream: `rx2dnssd` only (`implementation project(':rx2dnssd')`). Does not depend on `rxdnssd` or `dnssd` directly.
- Downstream: none (leaf module, not published — no `publish-module.gradle` applied).

## Testing in isolation

No dedicated test suite; this module isn't included in the CircleCI `check` reporting steps (`circle.yml` only stores reports for `dnssd`, `rxdnssd`, `rx2dnssd`). Manual verification: build and run on a device/emulator on the same network as another mDNS advertiser (or a second copy of the app registering a service).

## Extension points / typical changes

When adding a sample for a new `rx2dnssd` capability, extend `MainActivity` rather than creating a new Activity, unless the feature needs a genuinely separate UI flow.
