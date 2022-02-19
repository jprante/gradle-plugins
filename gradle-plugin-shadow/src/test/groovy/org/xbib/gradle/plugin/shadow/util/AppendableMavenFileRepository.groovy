package org.xbib.gradle.plugin.shadow.util

import org.xbib.gradle.plugin.shadow.util.repo.maven.MavenFileRepository
import groovy.transform.InheritConstructors

@InheritConstructors
class AppendableMavenFileRepository extends MavenFileRepository {

    @Override
    AppendableMavenFileModule module(String groupId, String artifactId, Object version = '1.0') {
        def artifactDir = rootDir.file("${groupId.replace('.', '/')}/$artifactId/$version")
        return new AppendableMavenFileModule(artifactDir, groupId, artifactId, version as String)
    }

}
