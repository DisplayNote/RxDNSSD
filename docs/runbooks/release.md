# Release

Full step-by-step lives in `BUILD.md` ("PUBLISH INSTRUCTIONS") — this runbook summarizes it and links back.

## From CI

Pushing a tag named after the version (e.g. `git tag 0.9.22 && git push origin 0.9.22`) runs
`ci/azure-pipelines.yml` on the tag (a bare `<version>` tag publishes; any other tag skips the publish stage): after the tests and the release build, the `gradle_publish`
stage runs `:dnssd:publish :rxdnssd:publish :rx2dnssd:publish` with `PUBLISH_VERSION` set to the tag
and the Artifactory credentials (`MAVEN_ARTIFACTORY_USER`/`MAVEN_ARTIFACTORY_PASS`) from the `montage-client-environment-variables` variable group, base URL in the pipeline. Branch and
PR builds never publish. No local/published toggle is needed: with `api project(':dnssd')` the
`rxdnssd`/`rx2dnssd` POMs already name `com.displaynote.dnssd:dnssd:<version>`. The manual steps
below remain for a release from a workstation.

## 1. Set Artifactory credentials

In `gradle.properties` (local) or environment variables (CI):
```ini
displaynoteDeployerArtifactoryUrl=...
displaynoteMavenArtifactoryUsername=...
displaynoteMavenArtifactoryPassword=...
displaynoteDeployerArtifactoryUsername=...
displaynoteDeployerArtifactoryPassword=...
```
or the environment-variable equivalents (`DEPLOYER_ARTIFACTORY_URL`, `MAVEN_ARTIFACTORY_USERNAME`, `MAVEN_ARTIFACTORY_PASSWORD`, `DEPLOYER_ARTIFACTORY_USERNAME`, `DEPLOYER_ARTIFACTORY_PASSWORD`) — see `publish-root.gradle` for exactly how these resolve.

## 2. Switch modules from local to published `dnssd` dependency

In both `rxdnssd/build.gradle` and `rx2dnssd/build.gradle`, comment out:
```groovy
api project(':dnssd')
```
and uncomment:
```groovy
api "${rootProject.ext.groupId}:dnssd:${rootProject.ext.displaynotePublishVersion}"
```
This is a manual edit with no automated guard — see Gotchas in `AGENTS.md`.

## 3. Set the version

In `publish-root.gradle`:
```groovy
displaynotePublishVersion = System.getenv("PUBLISH_VERSION") ?: '0.9.21'
```
Either bump the fallback literal or set `PUBLISH_VERSION` in the environment before running Gradle. Group ID is `com.displaynote.dnssd` for all three artifacts (`dnssd`, `rxdnssd`, `rx2dnssd` — see `PUBLISH_ARTIFACT_ID` in each module's `build.gradle`).

## 4. Publish

```bash
./gradlew publish
```
`publish-module.gradle` (applied by `dnssd`, `rxdnssd`, `rx2dnssd`) wires the `release` component into a `MavenPublication`, routing to `libs-release-local` or `libs-snapshot-local` on the configured Artifactory URL depending on whether the version string ends in `-SNAPSHOT`.

## 5. Revert the local/publish toggle

After publishing, switch `rxdnssd`/`rx2dnssd` back to `api project(':dnssd')` before continuing local development, or the next contributor's build will silently pull the just-published (possibly stale) Artifactory artifact instead of their local `dnssd` changes.

## `app` is out of scope for release

`app` is a development/testing-only demo, not a publicly released artifact — `publish-module.gradle` isn't applied to it, and its `versionCode`/`versionName` have no bearing on the release process above. No release step is needed for it.
