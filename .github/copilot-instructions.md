# Copilot instructions

Full detail: `AGENTS.md` at repo root. This file is the 5–10 rules that hurt most when violated, for GitHub Copilot in VS Code / Visual Studio.

1. Never hand-edit `dnssd/src/main/jni/mdnsresponder/` — it's vendored Apple mDNSResponder source, not DisplayNote code.
2. `rxdnssd` (RxJava1) and `rx2dnssd` (RxJava2) both wrap `dnssd` independently — never add a dependency between them, and don't have `app` depend on `rxdnssd`.
3. When adding an operation to `Rx2Dnssd`, implement it in both `Rx2DnssdBindable` and `Rx2DnssdEmbedded`, then port the same change to `RxDnssd`/`RxDnssdBindable`/`RxDnssdEmbedded`.
4. Don't call `Internal*` classes (`InternalDNSSD`, etc.) from outside the `dnssd` module — they're native-method plumbing, not public API.
5. Keep `@Deprecated` methods (e.g. `queryRecords()`) in place for binary compatibility; don't delete them.
6. Before publishing, `rxdnssd`/`rx2dnssd` `build.gradle` must switch `api project(':dnssd')` to the Maven coordinate — see `docs/runbooks/release.md`. This is a manual step with no build-time guard.
7. Tests use JUnit4 + Mockito + PowerMock (native calls can't run on the JVM test runner) — follow the existing per-module test pattern in `docs/runbooks/testing.md`.
8. Copy the existing Apache 2.0 license header into any new source file.

See `AGENTS.md` for architecture diagrams, the repository map, and the full "Where to add X" table.
