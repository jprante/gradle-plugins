package org.xbib.gradle.plugin.shadow.util.repo.maven

import org.xbib.gradle.plugin.shadow.util.file.TestFile

/**
 * A fixture for dealing with file Maven repositories.
 */
class MavenFileRepository implements MavenRepository {
    final TestFile rootDir

    MavenFileRepository(TestFile rootDir) {
        this.rootDir = rootDir
    }

    URI getUri() {
        return rootDir.toURI()
    }

    MavenFileModule module(String groupId, String artifactId, Object version = '1.0') {
        def artifactDir = rootDir.file("${groupId.replace('.', '/')}/$artifactId/$version")
        return new MavenFileModule(artifactDir, groupId, artifactId, version as String)
    }
}
