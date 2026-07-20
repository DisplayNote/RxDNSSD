---
inclusion: always
---

# Structure

```
app/          Sample Android app, depends on rx2dnssd only. Not published.
dnssd/        Plain-Java DNS-SD API + JNI bridge + vendored native mDNSResponder core.
rxdnssd/      RxJava 1 wrapper over dnssd (legacy).
rx2dnssd/     RxJava 2 wrapper over dnssd (use for new work).
docs/         architecture.md, modules/<module>.md, runbooks/, glossary.md
```

`rxdnssd` and `rx2dnssd` both depend on `dnssd` independently and must never depend on each other. See `AGENTS.md` "Where to add X" for a per-change-type routing table, and `docs/modules/*.md` for per-module public API, dependencies, and test instructions.
