# CLAUDE.md

Claude Code-specific guide. Full detail in `AGENTS.md` — read that first for architecture, module map, and gotchas.

## DO

- DO run `./gradlew :dnssd:test`, `:rxdnssd:test`, `:rx2dnssd:test` (as relevant) after touching any of those modules — no CI runs on your behalf until push.
- DO implement any new `Rx2Dnssd` operation in **both** `Rx2DnssdBindable` and `Rx2DnssdEmbedded`, then port it to `RxDnssd`/`RxDnssdBindable`/`RxDnssdEmbedded` — see `docs/modules/rx2dnssd.md`.
- DO keep `@Deprecated` methods in place (e.g. `queryRecords()`) — binary compatibility for existing consumers, don't delete them.
- DO copy the existing Apache 2.0 header into any new source file.
- DO read `BUILD.md` before your first build — `rxdnssd`/`rx2dnssd` default to `api project(':dnssd')`, which must be swapped to a Maven coordinate before publish (`docs/runbooks/release.md`).

## DON'T

- DON'T hand-edit anything under `dnssd/src/main/jni/mdnsresponder/` — it's vendored Apple mDNSResponder source, not DisplayNote code. Only touch it for a deliberate, whole-tree upstream sync.
- DON'T add a dependency between `rxdnssd` and `rx2dnssd`, or have `app` depend on anything but `rx2dnssd`.
- DON'T call `Internal*` classes (`InternalDNSSD`, etc.) from outside `dnssd` — they're native-method plumbing, not public API.
- DON'T assume Maven Central coordinates (`com.github.andriydruk`) and this fork's Artifactory coordinates (`com.displaynote.dnssd`) share version history.
- DON'T trust a green build alone if you touched `dnssd`'s native layer — the JVM test runner stubs native calls (`unitTests.returnDefaultValues = true`); native behavior needs a real device/emulator run (`docs/runbooks/testing.md`).

## Gotcha you will hit first

If `./gradlew` reports `JAVA_HOME is set to an invalid directory` on Windows/Git Bash even though the directory exists, your `JAVA_HOME` value likely contains literal embedded quote characters — re-export it without quotes. Full detail: `docs/runbooks/troubleshooting.md`.

## Commands

```bash
./gradlew clean build   # full build
./gradlew test          # all unit tests
./gradlew check         # what CI runs
```

See `AGENTS.md` for the repository map, architecture diagrams, and the full "Where to add X" table.
