package org.xbib.gradle.plugin.shadow.util.repo.maven

/**
 * A fixture for dealing with Maven repositories.
 */
interface MavenRepository {
    URI getUri()

    MavenModule module(String groupId, String artifactId)

    MavenModule module(String groupId, String artifactId, Object version)
}
