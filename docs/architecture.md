# Architecture

See `docs/glossary.md` for term definitions (DNS-SD, mDNS, Bindable vs Embedded).

## Context

```mermaid
C4Context
    title RxDNSSD — System Context

    Person(dev, "Android app developer", "Consumes dnssd / rxdnssd / rx2dnssd as a Maven dependency")
    System(rxdnssd, "RxDNSSD library", "This repo: dnssd + rxdnssd + rx2dnssd")
    System_Ext(mdnsd, "Android system mdnsd/nsd daemon", "OS-provided, used by *Bindable implementations")
    System_Ext(lan, "Local network (mDNS multicast)", "Other DNS-SD advertisers/browsers on the same subnet")

    Rel(dev, rxdnssd, "implementation 'com.displaynote.dnssd:{dnssd,rxdnssd,rx2dnssd}:<version>' (Maven)")
    Rel(rxdnssd, mdnsd, "AIDL/bound service (Bindable only)")
    Rel(rxdnssd, lan, "Multicast UDP 5353 (Embedded: own native core; Bindable: via daemon)")
```

## Containers

```mermaid
C4Container
    title RxDNSSD — Containers (Gradle modules)

    Container(app, "app", "Android app (sample)", "Demonstrates Rx2Dnssd usage: browse/resolve/register")
    Container(rx2, "rx2dnssd", "Android library (AAR)", "RxJava2 wrapper: Rx2Dnssd, Rx2DnssdBindable, Rx2DnssdEmbedded, BonjourService")
    Container(rx1, "rxdnssd", "Android library (AAR)", "RxJava1 wrapper (legacy): RxDnssd, RxDnssdBindable, RxDnssdEmbedded")
    Container(dnssd, "dnssd", "Android library (AAR) + native", "Plain Java DNSSD API + JNI bridge + vendored mDNSResponder C core")

    Rel(app, rx2, "implementation project")
    Rel(rx2, dnssd, "api project(':dnssd') (or Maven coordinate when publishing)")
    Rel(rx1, dnssd, "api project(':dnssd') (or Maven coordinate when publishing)")
```

`rxdnssd` and `rx2dnssd` do not depend on each other; both wrap `dnssd` independently. Never add a dependency between them.

## Components — inside `dnssd`

```mermaid
C4Component
    title dnssd module — Components

    Component(dnssdIface, "DNSSD (interface)", "Java", "register/browse/resolve/queryRecord/createRecordRegistrar")
    Component(bindable, "DNSSDBindable", "Java", "Talks to system mdnsd via bound AIDL service")
    Component(embedded, "DNSSDEmbedded", "Java", "Loads libjdns_sd_embedded.so, talks to InternalDNSSD")
    Component(internal, "InternalDNSSD + Internal*", "Java (package-private)", "Thin native-method declarations mirroring dns_sd.h")
    Component(jni, "JNISupport.c / DNSSD.java.h", "C (JNI)", "Bridges InternalDNSSD native calls to the C API below")
    Component(core, "mdnsresponder/ (vendored)", "C", "Apple's mDNSCore + mDNSPosix + mDNSShared — DO NOT hand-edit, see Gotchas in AGENTS.md")

    Rel(bindable, dnssdIface, "implements")
    Rel(embedded, dnssdIface, "implements")
    Rel(embedded, internal, "delegates native calls")
    Rel(internal, jni, "JNI native methods")
    Rel(jni, core, "calls into DNSServiceRegister/Browse/Resolve/QueryRecord etc.")
```

## Data flow — browse → resolve → query (Rx2, Embedded)

```mermaid
sequenceDiagram
    participant App
    participant Rx2Dnssd
    participant Rx2DnssdEmbedded
    participant InternalDNSSD
    participant Native as libjdns_sd_embedded.so

    App->>Rx2Dnssd: browse("_http._tcp", "local.")
    Rx2Dnssd->>Rx2DnssdEmbedded: browse()
    Rx2DnssdEmbedded->>InternalDNSSD: native browse()
    InternalDNSSD->>Native: DNSServiceBrowse()
    Native-->>InternalDNSSD: serviceFound/serviceLost callback
    InternalDNSSD-->>Rx2DnssdEmbedded: BonjourService (partial)
    App->>Rx2Dnssd: .compose(resolve())
    Rx2Dnssd->>Native: DNSServiceResolve()
    Native-->>App: BonjourService (host, port)
    App->>Rx2Dnssd: .compose(queryRecords())
    Rx2Dnssd->>Native: DNSServiceQueryRecord() (A/AAAA + TXT)
    Native-->>App: BonjourService (fully populated, or isLost()==true)
```

## Native build

`dnssd/build.gradle` wires `externalNativeBuild.ndkBuild` to `dnssd/src/main/jni/Android.mk`. Two native targets are built (see `Android.mk`):
- `jdns_sd_embedded` — full mDNSCore + mDNSPosix + JNISupport, used by `DNSSDEmbedded`.
- (bindable path uses the plain `dnssd_client*` shim files, no full core, talking to the system daemon over a Unix domain socket via IPC — see `mDNSShared/dnssd_ipc.c`).

NDK version is pinned per-module in `build.gradle` (`ndkVersion "28.2.13676358"` in `app` and `dnssd`).

## Active decisions

- **Local vs published dependency switch** (`BUILD.md`): `rxdnssd/build.gradle` and `rx2dnssd/build.gradle` both declare `dnssd` as `api project(':dnssd')` for local builds; the commented-out alternative (`api "${rootProject.ext.groupId}:dnssd:${version}"`) is swapped in manually before publishing. There is no automated toggle — this is a manual, error-prone step, see Gotchas in `AGENTS.md`.
- **Group ID migration**: `groupId` in `publish-root.gradle` is `com.displaynote.dnssd` (internal Artifactory fork), while historical Maven Central releases used `com.github.andriydruk` (see `README.md` badges). The two coordinate spaces are not interchangeable.
