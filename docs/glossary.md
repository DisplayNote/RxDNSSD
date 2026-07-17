# Glossary

| Term | Meaning |
|---|---|
| **DNS-SD** | DNS Service Discovery — the IETF spec (RFC 6763) this library implements a client for. Also called Bonjour/Zeroconf/mDNSResponder. |
| **mDNS** | Multicast DNS (RFC 6762) — the name-resolution transport DNS-SD runs over on the local network (no central DNS server). |
| **mDNSResponder** | Apple's open-source daemon/core implementing mDNS + DNS-SD. Vendored source lives under `dnssd/src/main/jni/mdnsresponder/` — see [dnssd module](modules/dnssd.md). |
| **Bindable** | The `*Bindable` implementations (`DNSSDBindable`, `RxDnssdBindable`, `Rx2DnssdBindable`) that talk to the **system's** `mdnsd`/`nsd` daemon over a bound service connection, instead of running their own native core. Lower battery use, but deprecated by Google since Android 12 (`targetSDK 31`) — see `README.md`. |
| **Embedded** | The `*Embedded` implementations that link and run mDNSResponder's native core (compiled via NDK) directly inside the app's process — no dependency on the OS daemon. Works on any device API 14+. |
| **BonjourService** | The RxDNSSD/Rx2DNSSD data model (`rxdnssd`/`rx2dnssd` module) representing one discovered service: name, type, domain, host, port, TXT records, addresses. Emitted by `browse()`/`resolve()`/`queryRecords()` chains. |
| **Register / Browse / Resolve / Query** | The four DNS-SD primitives exposed by `DNSSD`/`RxDnssd`/`Rx2Dnssd`: advertise a service, discover services of a type, resolve a found service's host/port, and query arbitrary DNS records (used for TXT and address lookups). |
| **JNI shim** | `dnssd/src/main/jni/JNISupport.c` + `DNSSD.java.h` — the hand-written bridge between the Java `InternalDNSSD` class and the native mDNSResponder library. |
| **`dnssd` module** | Lowest-level module: plain Java API (`DNSSD` abstract class) + native mDNSResponder core. No Rx dependency. Published as `com.github.andriydruk:dnssd` historically, now under `com.displaynote.dnssd` (see [release runbook](runbooks/release.md)). |
| **`rxdnssd` module** | RxJava 1 wrapper over `dnssd`. Legacy; kept for existing consumers. |
| **`rx2dnssd` module** | RxJava 2 wrapper over `dnssd`. The actively recommended API for new integrations. |
| **`app` module** | Sample Android application demonstrating `rx2dnssd` usage (`DNSSDActivity`, `MainActivity`). Not published. |
