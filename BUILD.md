# BUILD INSTRUCTIONS

Make sure setup rxdnssd and rx2dnssd modules build.gradle files are using dnssd project dependency

```groovy
dependencies {
    // uncomment this line for local building, comment for publishing
    api project(':dnssd')

    // uncomment this line for publishing, comment for local building
    // api "${rootProject.ext.groupId}:dnssd:${rootProject.ext.mavenCentralVersion}"

[...]

}
```

Finally, build project

```
gradle aR
```

# PUBLISH INSTRUCTIONS

Fill these values into your gradle.properties:

```ini
displaynoteDeployerArtifactoryUrl=artifactory_url
displaynoteMavenArtifactoryUsername=artifactory_user_name_with_ro_access
displaynoteMavenArtifactoryPassword=artifactory_user_name_with_ro_access_password
displaynoteDeployerArtifactoryUsername=artifactory_user_name_with_rw_access
displaynoteDeployerArtifactoryPassword=artifactory_user_name_with_rw_access_password
```

Also you can setup these enviroment variables instead

```ini
DEPLOYER_ARTIFACTORY_URL
MAVEN_ARTIFACTORY_USERNAME
MAVEN_ARTIFACTORY_PASSWORD
DEPLOYER_ARTIFACTORY_USERNAME
DEPLOYER_ARTIFACTORY_PASSWORD
```
Then setup rxdnssd and rx2dnssd modules for deploying

```groovy
dependencies {
    // uncomment this line for local building, comment for publishing
    api project(':dnssd')

    // uncomment this line for publishing, comment for local building
    // api "${rootProject.ext.groupId}:dnssd:${rootProject.ext.mavenCentralVersion}"

[...]

}
```

And update version in publish-root.gradle
```groovy
mavenCentralVersion = System.getenv("PUBLISH_VERSION") ?: '0.9.28'
```

Finally, call to publish

```
gradle publish
```
