# Debugging with AI — log sanitisation (control 4.7)

> Generated from the `log-sanitise` template (displaynote-engineering plugin) by
> `docs-update`. The "Mandatory step" and "Rules" sections are kept verbatim.

## Where this repo's logs live

| Source | Location / command | Typical sensitive content |
|---|---|---|
| Application log | No log file is written by this repo. The libraries (`dnssd`, `rxdnssd`, `rx2dnssd`) log only through `android.util.Log` (logcat) with literal tags: `DNSSD` (`Log.wtf` in `DNSSD.java`, multicast-lock problems), `RxResolveListener` (`Log.w` in `DNSSD.parseTXTRecords`, TXT parse failures — despite the tag it lives in `dnssd`), `DNSSDEmbedded` (`Log.i`/`Log.e` start/stop lifecycle and native error codes in `DNSSDEmbedded.java`) and `DNSSDBindable` (`Log.e` when the system `NSD_SERVICE` cannot be started). `rxdnssd` and `rx2dnssd` contain no log calls; they report errors to the consumer through `onError` with the numeric DNS-SD error code. Native code: the embedded mDNSResponder core (`libjdns_sd_embedded.so`) would log through `__android_log_print` with tag `mdns` (`mDNSShared/PlatformCommon.c`), but it is built with `MDNS_DEBUGMSGS=0` (`dnssd/src/main/jni/Android.mk`), which drops every message on Android (see the note below); `JNISupport.c` has no active log calls. The daemon client stub used by `DNSSDBindable` (`libjdns_sd.so`, `mDNSShared/dnssd_clientstub.c`) calls `syslog()` on socket/IPC failures, which Android's libc forwards to logcat. The sample `app` logs with tags `TAG` and `Thread`. When consumed by Montage, these lines appear in the Montage process's logcat; whether they also reach Montage's own log files depends on Montage's logging setup, not on this repo | Library: error codes, thread/lifecycle messages, socket error text — no service names, hostnames, IPs or TXT values. Sample app: service names, hostnames, TXT record keys and values, `Build.DEVICE` |
| Live stream | `adb logcat -d -s DNSSD DNSSDEmbedded DNSSDBindable RxResolveListener` for the libraries (add `TAG` and `Thread` for the sample app). The `syslog()` lines from `dnssd_clientstub.c` carry a tag chosen by the C library, not by this repo, so for the Bindable variant capture an unfiltered `adb logcat -d` (and sanitise it) | Sample app: service names, hostnames, TXT records; Bindable stub: local socket paths |
| Crash reports | No crash reporter (Sentry or similar) is wired into this repo. Java exceptions surface in logcat (`adb logcat -d -b crash`); native crashes in `libjdns_sd_embedded.so` / `libjdns_sd.so` produce `tombstone_*` files on the device. Inside Montage, crashes are reported through Montage's own crash reporting | native stack traces, paths, memory contents |
| CI logs | Azure Pipelines run logs (`displaynote-devops`) | internal paths, hostnames |
| Customer bundles | Zendesk ticket attachments — download them to a file and sanitise before reading; the Zendesk MCP bypasses the Claude Code guard, so never read attachments through it (see `.agents/skills/log-sanitise/SKILL.md`) | **Protected**: end-user identifiers (1.3 §4.2) |

## Mandatory step — sanitise before any AI tool sees the log

Before a log file, a log excerpt or a live log stream reaches Claude Code, Codex,
Copilot, Cursor, ChatGPT, Claude (Cowork) or any custom OpenAI-API integration,
run it through `dn_logscrub`:

```bash
# Work OUTSIDE the repository: a raw log inside it blocks the searches that would read it
mkdir -p ~/dn-tickets/22416 && cd ~/dn-tickets/22416

# Claude Code (plugin installed) — one call, all files, shared placeholders
/log-sanitise montage.log launcher.log --map ticket-22416.dnmap

# Any agent / shell — the portable copy synced into the repo
python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py montage.log launcher.log --map ticket-22416.dnmap

# Inputs in a read-only folder, or elsewhere: write the copies into one directory
python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py /var/log/omni/*.log --out-dir ~/dn-tickets/22416

# Live streams — pipe, never paste; use a command that ends (`-d`), not a live tail
adb logcat -d | python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py - > logcat.scrubbed.txt

# Customer logs — add the customer's domain(s) so their hostnames are pseudonymised too
python3 <repo>/.agents/skills/log-sanitise/scripts/dn_logscrub.py bundle/*.log --domain acme-school.org
```

A run that is cut short (Ctrl-C, a tool timeout) still writes a trailer marked
`interrupted`: the copy is clean but incomplete. The sanitiser processes a few
MB per second; run very large bundles from your own terminal.

The tool writes `<name>.scrubbed.<ext>` next to each input, prints one summary
line per file (`dn_logscrub: montage.log: 41 redactions (EMAIL=3, ID=12, …)`) and
starts every output with a `# dn_logscrub v…` marker header and ends it with a
`# dn_logscrub end | redactions=…` trailer carrying the per-category counts
(output is written as it is produced, so a live stream appears immediately
instead of waiting for the end). Only files that start with that header and
end with that trailer may be opened by, pasted into, or attached to an AI tool.
A file with the header but no trailer as its last non-empty line is raw:
something was appended after scrubbing (`cat raw >> x.scrubbed.log`,
concatenated files, a process still writing). A trailer ending in
`| interrupted` is accepted — the copy is clean, only incomplete. A
`# dn_logscrub-partial` header does not count: it means rules were switched off
for that run.

## Rules

1. Only `*.scrubbed.*` files (marker header on the first line and the
   `# dn_logscrub end` trailer as the last non-empty line) go to an AI tool.
   Raw logs never do — the Claude Code hook blocks them; for other tools the
   rule is on you.
2. Placeholders are stable within a run: `<EMAIL_1>` is the same person in every
   file of that run. Keep placeholders in PR descriptions, ticket comments and
   Slack messages; never expand them there.
3. The `--map` file (`*.dnmap`) holds the originals for your own reverse lookup.
   It stays on your machine; never attach it, commit it or paste from it.
4. Customer logs are **Protected** data by default (AI Governance Policy 1.3 §4.2).
   Sanitised customer logs may go to Green-List tools with a corporate account.
   Raw customer logs may go to an AI tool only with AI Lead + CEO approval
   (1.3 §4.1) — signal an approved exception with `DN_LOGSCRUB_ALLOW_RAW=1`.
5. Residual check: skim the scrubbed file for anything the patterns missed
   (a person's name in free text, a customer hostname without `--domain`, an
   unusual token format). Fix with `--domain`, or open an issue on
   `displaynote-engineering` with the *category and shape* of the miss — never
   with the value itself.
6. Automated flows (Sentry fixer, Release Management Agent, n8n) call the same
   library (`Scrubber().scrub_text(...)`) before every external model call. A
   flow that writes its result to a file uses `Scrubber().finalize(text, name)`,
   which adds the header and trailer the Claude Code hook and `--check` require.

## What is redacted vs. kept

Redacted irreversibly: private keys, JWTs, auth headers, URL credentials,
cloud/API keys, any `password= / token= / secret= / *_key=` value, OAuth
`code=`/`state=`/`nonce=` in URLs.
Pseudonymised consistently: e-mails, quoted/keyed names (people, devices,
computers), keyed ids (serial, deviceId, session, meetingId, roomPin, tenantId,
licence…), Android `getprop` serials, Wi-Fi SSIDs, IPv4/IPv6, MACs, UUIDs, phone
numbers, the user-home part of any path (Windows with either slash, JSON-escaped,
`file:///`, WSL), Windows `DOMAIN\user` accounts and UNC servers, `DESKTOP-…`
machine names, `.local/.lan/.internal` hosts, `--domain` domains, long hex and
high-entropy strings.
Kept: timestamps, version numbers, stack traces, package/class/method names and
JNI symbols, the rest of the path, file hashes and git commit ids, loopback
addresses, and keys that only describe a secret (`token_expires_in=3600`).
Not caught: a person's name in free text, a customer hostname without `--domain`.

Repo-specific note: the published libraries carry little sensitive data in
their own logs. `dnssd/src/main/java/com/github/druk/dnssd/DNSSD.java` logs
fixed multicast-lock messages and, in `parseTXTRecords`, only the index of a
TXT entry that failed to parse plus the exception (no key or value);
`DNSSDEmbedded.java` and `DNSSDBindable.java` log lifecycle messages, numeric
error codes and exceptions; `rxdnssd`/`rx2dnssd` do not log at all. None of
these modules enables minification (`minifyEnabled false` in every
`build.gradle`), their ProGuard files (`dnssd/proguard-rules.txt`,
`app/proguard-rules.pro`) contain no `-assumenosideeffects` rule, and `dnssd`
ships no consumer ProGuard rules, so every `Log.*` call above survives release
builds; whether a consuming app strips them depends on that app's own R8
configuration. Native: both libraries are compiled with `MDNS_DEBUGMSGS=0`
(`dnssd/src/main/jni/Android.mk`), so `debugf` compiles to a no-op and
`mDNSPlatformWriteDebugMsg` is not built
(`mDNSCore/mDNSDebug.h`, `mDNSShared/PlatformCommon.c`); `mDNS_DebugMode` is
false on Android (`mDNSShared/mDNSDebug.c`), and `mDNSPlatformWriteLogMsg`
returns without logging for every level except `MDNS_LOG_DEBUG`, which only
`debugf` emits — so `LogMsg`/`LogInfo`/`LogOperation` lines from the embedded
core, which would contain service names, hostnames and IP addresses, never
reach logcat in this build. Turning `MDNS_DEBUGMSGS` on would change that.
The Bindable client stub (`mDNSShared/dnssd_clientstub.c`, in `libjdns_sd.so`)
logs, at warning/info level, file-descriptor numbers, errno codes and messages
and local socket/device paths on IPC failures; the line that would log the
request name is commented out. The real residual risk is outside the library
code: the sample app (`app/`, not published, but its release build type is not
minified either) logs service names (`DNSSDActivity.java` "Found",
`MainActivity.java` via `BonjourService.toString()`), resolved hostnames
("Resolved"), full service names ("Query address") and `Build.DEVICE`
("Register successfully"); and `BonjourService.toString()` in both
`rxdnssd` and `rx2dnssd` renders `serviceName`, `domain`, `hostname`, `port`
and the full TXT record map (`dnsRecords`) — not the IP addresses — so any
consumer (Montage included) that logs a `BonjourService` puts that data in its
logs. Service names and TXT values are often user-chosen device or room names;
in `toString()` output they are quoted and keyed, but check them, `.local`
hostnames and any free-text names in the residual pass. Nothing in this repo
writes a log file. CI: build and test run on CircleCI (`circle.yml`, test
reports uploaded as artifacts); the GitHub Actions docs-sync workflows run
`tools/docs-sync/run_docs_agent.sh`, which writes the full agent transcript to
`docs-agent.log` and echoes it to the workflow run log (only
`docs-freshness.json` is uploaded as an artifact) — that transcript contains
repository content, not runtime logs. No repo-specific sanitiser patterns have been shipped yet; raise any needed as an issue on `displaynote-engineering` and list them here once shipped.
