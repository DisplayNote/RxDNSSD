# BUILD INSTRUCTIONS

Make sure setup rxdnssd and rx2dnssd modules build.gradle files are using dnssd project dependency

```groovy
dependencies {
    // uncomment this line for local building, comment for publishing
    api project(':dnssd')

    // uncomment this line for publishing, comment for local building
    // api "${rootProject.ext.groupId}:dnssd:${rootProject.ext.displaynotePublishVersion}"

[...]

}
```

Finally, build project

```
gradle aR
```

For testing, copy AARs from **rx2dnssd** and **dnssd** to your libs folder in app's template

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
    // api project(':dnssd')

    // uncomment this line for publishing, comment for local building
    api "${rootProject.ext.groupId}:dnssd:${rootProject.ext.displaynotePublishVersion}"

[...]

}
```

And update version in publish-root.gradle (you can also use **PUBLISH_VERSION** environment var instead)

```groovy
displaynotePublishVersion = System.getenv("PUBLISH_VERSION") ?: '0.9.21'
```

Finally, call to publish

```
gradle publish
```
