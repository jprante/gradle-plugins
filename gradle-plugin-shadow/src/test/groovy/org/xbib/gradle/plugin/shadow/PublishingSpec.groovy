package org.xbib.gradle.plugin.shadow

import org.xbib.gradle.plugin.shadow.util.AppendableMavenFileRepository
import org.xbib.gradle.plugin.shadow.util.PluginSpecification

class PublishingSpec extends PluginSpecification {

    AppendableMavenFileRepository repo
    AppendableMavenFileRepository publishingRepo

    def setup() {
        repo = repo()
        publishingRepo = repo('remote_repo')
    }

    def "publish shadow jar with maven-publish plugin"() {
        given:
        repo.module('shadow', 'a', '1.0')
                .insertFile('a.properties', 'a')
                .insertFile('a2.properties', 'a2')
                .publish()
        repo.module('shadow', 'b', '1.0')
                .insertFile('b.properties', 'b')
                .publish()

        settingsFile << "rootProject.name = 'maven'"
        buildFile << """
            apply plugin: 'maven-publish'

            dependencies {
               implementation 'shadow:a:1.0'
               shadow 'shadow:b:1.0'
            }
            
            shadowJar {
               classifier = ''
               baseName = 'maven-all'
            }
            
            publishing {
               publications {
                   shadow(MavenPublication) { publication ->
                       project.shadow.component(publication)
                       artifactId = 'maven-all'
                   }
               }
               repositories {
                   maven {
                       url "${publishingRepo.uri}"
                   }
               }
            }
        """.stripIndent()

        when:
        runner.withArguments('publish').build()

        then:
        File publishedFile = publishingRepo.rootDir.file('shadow/maven-all/1.0/maven-all-1.0.jar').canonicalFile
        assert publishedFile.exists()

        and:
        contains(publishedFile, ['a.properties', 'a2.properties'])

        and:
        File pom = publishingRepo.rootDir.file('shadow/maven-all/1.0/maven-all-1.0.pom').canonicalFile
        assert pom.exists()

        def contents = new XmlSlurper().parse(pom)
        assert contents.dependencies.size() == 1
        assert contents.dependencies[0].dependency.size() == 1

        def dependency = contents.dependencies[0].dependency[0]
        assert dependency.groupId.text() == 'shadow'
        assert dependency.artifactId.text() == 'b'
        assert dependency.version.text() == '1.0'
    }
}
