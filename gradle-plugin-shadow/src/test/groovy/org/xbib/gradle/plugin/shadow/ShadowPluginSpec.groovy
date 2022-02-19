package org.xbib.gradle.plugin.shadow

import org.gradle.util.GradleVersion
import org.xbib.gradle.plugin.shadow.tasks.ShadowJar
import org.xbib.gradle.plugin.shadow.util.PluginSpecification
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Issue

import java.util.jar.Attributes
import java.util.jar.JarFile

class ShadowPluginSpec extends PluginSpecification {

    def 'apply plugin'() {
        given:
        String projectName = 'myshadow'
        String version = '1.0.0'

        Project project = ProjectBuilder.builder().withName(projectName).build()
        project.version = version

        when:
        project.plugins.apply(ShadowPlugin)

        then:
        project.plugins.hasPlugin(ShadowPlugin)

        and:
        assert !project.tasks.findByName('shadowJar')

        when:
        project.plugins.apply(JavaPlugin)

        then:
        ShadowJar shadow = project.tasks.findByName('shadowJar') as ShadowJar
        assert shadow
        assert shadow.getProperty('baseName') == projectName
        assert shadow.getProperty('destinationDir') == new File(project.buildDir, 'libs')
        assert shadow.getProperty('version') == version
        assert shadow.getProperty('classifier') == 'all'
        assert shadow.getProperty('extension') == 'jar'

        and:
        Configuration shadowConfig = project.configurations.findByName('shadow')
        assert shadowConfig
        shadowConfig.artifacts.files.contains(shadow.archiveFile.get().getAsFile())

    }

    def 'shadow copy'() {
        given:
        URL artifact = this.class.classLoader.getResource('test-artifact-1.0-SNAPSHOT.jar')
        URL project = this.class.classLoader.getResource('test-project-1.0-SNAPSHOT.jar')

        buildFile << """
            shadowJar {
                from('${artifact.path}')
                from('${project.path}')
            }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        assert output.exists()
    }

    def 'include project sources'() {
        given:
        file('src/main/java/shadow/Passed.java') << '''
            package shadow;
            public class Passed {}
        '''.stripIndent()

        buildFile << """
            dependencies { implementation 'junit:junit:3.8.2' }

            // tag::rename[]
            shadowJar {
               baseName = 'shadow'
               classifier = null
               version = null
            }
            // end::rename[]
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        contains(output("shadow.jar"), ['shadow/Passed.class', 'junit/framework/Test.class'])

        and:
        doesNotContain(output("shadow.jar"), ['/'])
    }

    def 'include project dependencies'() {
        given:
        file('settings.gradle') << """
            include 'client', 'server'
        """.stripIndent()

        file('client/src/main/java/client/Client.java') << """
            package client;
            public class Client {}
            """.stripIndent()

        file('client/build.gradle') << """
            apply plugin: 'java'
            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation 'junit:junit:3.8.2' }
        """.stripIndent()

        file('server/src/main/java/server/Server.java') << """
            package server;

            import client.Client;

            public class Server {}
        """.stripIndent()

        file('server/build.gradle') << """
            apply plugin: 'java'
            apply plugin: 'org.xbib.gradle.plugin.shadow'

            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation project(':client') }

        """.stripIndent()

        File serverOutput = file('server/build/libs/server-all.jar')

        when:
        runner.withArguments(':server:shadowJar').build()

        then:
        contains(serverOutput, [
                'client/Client.class',
                'server/Server.class',
                'junit/framework/Test.class'
        ])
    }

    /**
     * 'Server' depends on 'Client'. 'junit' is independent.
     * The minimize shall remove 'junit'.
     */
    def 'minimize by keeping only transitive dependencies'() {
        given:
        file('settings.gradle') << """
            include 'client', 'server'
        """.stripIndent()

        file('client/src/main/java/client/Client.java') << """
            package client;
            public class Client {}
        """.stripIndent()

        file('client/build.gradle') << """
            apply plugin: 'java'
            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation 'junit:junit:3.8.2' }
        """.stripIndent()

        file('server/src/main/java/server/Server.java') << """
            package server;

            import client.Client;

            public class Server {
                private final String client = Client.class.getName();
            }
        """.stripIndent()

        file('server/build.gradle') << """
            apply plugin: 'java-library'
            apply plugin: 'org.xbib.gradle.plugin.shadow'

            shadowJar {
                minimize()
            }

            tasks.withType(org.xbib.gradle.plugin.shadow.tasks.ShadowJar) {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
            tasks.withType(Jar) {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }

            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation project(':client') }
        """.stripIndent()

        File serverOutput = file('server/build/libs/server-all.jar')

        when:
        runner.withArguments(':server:shadowJar', '--stacktrace').withDebug(true).build()

        then:
        contains(serverOutput, [
                'client/Client.class',
                'server/Server.class'
        ])
        doesNotContain(serverOutput, ['junit/framework/Test.class'])
    }

    /**
     * 'Client', 'Server' and 'junit' are independent.
     * 'junit' is excluded from the minimize.
     * The minimize shall remove 'Client' but not 'junit'.
     */
    def 'exclude a dependency from minimize'() {
        given:
        file('settings.gradle') << """
            include 'client', 'server'
        """.stripIndent()

        file('client/src/main/java/client/Client.java') << """
            package client;
            public class Client {}
        """.stripIndent()

        file('client/build.gradle') << """
            apply plugin: 'java'
            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation 'junit:junit:3.8.2' }
        """.stripIndent()

        file('server/src/main/java/server/Server.java') << """
            package server;
            public class Server {}
        """.stripIndent()

        file('server/build.gradle') << """
            apply plugin: 'java'
            apply plugin: 'org.xbib.gradle.plugin.shadow'

            shadowJar {
                minimize {
                    exclude(dependency('junit:junit:.*'))
                }
            }

            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation project(':client') }
        """.stripIndent()

        File serverOutput = file('server/build/libs/server-all.jar')

        when:
        runner.withArguments(':server:shadowJar', '--stacktrace').withDebug(true).build()

        then:
        contains(serverOutput, [
                'server/Server.class',
                'junit/framework/Test.class'
        ])
        doesNotContain(serverOutput, ['client/Client.class'])
    }

    def 'depend on project shadow jar'() {
        given:
        file('settings.gradle') << """
            include 'client', 'server'
        """.stripIndent()

        file('client/src/main/java/client/Client.java') << """
            package client;
            public class Client {}
            """.stripIndent()

        file('client/build.gradle') << """
            apply plugin: 'java'
            apply plugin: 'org.xbib.gradle.plugin.shadow'
            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation 'junit:junit:3.8.2' }

            shadowJar {
               relocate 'junit.framework', 'client.junit.framework'
            }
        """.stripIndent()

        file('server/src/main/java/server/Server.java') << """
            package server;

            import client.Client;
            import client.junit.framework.Test;

            public class Server {}
        """.stripIndent()

        file('server/build.gradle') << """
            apply plugin: 'java'

            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation project(path: ':client', configuration: 'shadow') }
        """.stripIndent()

        File serverOutput = file('server/build/libs/server.jar')

        when:
        runner.withArguments(':server:jar').build()

        then:
        contains(serverOutput, [
                'server/Server.class'
        ])

        and:
        doesNotContain(serverOutput, [
                'client/Client.class',
                'junit/framework/Test.class',
                'client/junit/framework/Test.class'
        ])
    }

    def 'shadow a project shadow jar'() {
        given:
        file('settings.gradle') << """
            include 'client', 'server'
        """.stripIndent()

        file('client/src/main/java/client/Client.java') << """
            package client;
            public class Client {}
        """.stripIndent()

        file('client/build.gradle') << """
            apply plugin: 'java'
            apply plugin: 'org.xbib.gradle.plugin.shadow'
            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation 'junit:junit:3.8.2' }

            shadowJar {
               relocate 'junit.framework', 'client.junit.framework'
            }
        """.stripIndent()

        file('server/src/main/java/server/Server.java') << """
            package server;

            import client.Client;
            import client.junit.framework.Test;

            public class Server {}
        """.stripIndent()

        file('server/build.gradle') << """
            apply plugin: 'java'
            apply plugin: 'org.xbib.gradle.plugin.shadow'

            repositories { maven { url "${repo.uri}" } }
            dependencies { implementation project(path: ':client', configuration: 'shadow') }
        """.stripIndent()

        File serverOutput = file('server/build/libs/server-all.jar')

        when:
        runner.withArguments(':server:shadowJar').build()

        then:
        contains(serverOutput, [
                'client/Client.class',
                'client/junit/framework/Test.class',
                'server/Server.class',
        ])

        and:
        doesNotContain(serverOutput, [
                'junit/framework/Test.class'
        ])
    }

    def "exclude INDEX.LIST, *.SF, *.DSA, and *.RSA by default"() {
        given:
        repo.module('shadow', 'a', '1.0')
                .insertFile('a.properties', 'a')
                .insertFile('META-INF/INDEX.LIST', 'JarIndex-Version: 1.0')
                .insertFile('META-INF/a.SF', 'Signature File')
                .insertFile('META-INF/a.DSA', 'DSA Signature Block')
                .insertFile('META-INF/a.RSA', 'RSA Signature Block')
                .insertFile('META-INF/a.properties', 'key=value')
                .publish()

        file('src/main/java/shadow/Passed.java') << '''
            package shadow;
            public class Passed {}
        '''.stripIndent()

        buildFile << """
            dependencies { implementation 'shadow:a:1.0' }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        contains(output, ['a.properties', 'META-INF/a.properties'])

        and:
        doesNotContain(output, ['META-INF/INDEX.LIST', 'META-INF/a.SF', 'META-INF/a.DSA', 'META-INF/a.RSA'])
    }

    def "include runtimeOnly configuration by default"() {
        given:
        repo.module('shadow', 'a', '1.0')
                .insertFile('a.properties', 'a')
                .publish()

        repo.module('shadow', 'b', '1.0')
                .insertFile('b.properties', 'b')
                .publish()

        buildFile << """
            dependencies {
               runtimeOnly 'shadow:a:1.0'
               shadow 'shadow:b:1.0'
            }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        contains(output, ['a.properties'])

        and:
        doesNotContain(output, ['b.properties'])
    }

    // https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_known_issues
    def "include java-library configurations by default"() {
        given:
        GradleRunner versionRunner = runner
                .withGradleVersion(GradleVersion.current().version)
                .withArguments('--stacktrace')
                .withDebug(true)

        repo.module('shadow', 'api', '1.0')
                .insertFile('api.properties', 'api')
                .publish()

        repo.module('shadow', 'implementation-dep', '1.0')
                .insertFile('implementation-dep.properties', 'implementation-dep')
                .publish()

        repo.module('shadow', 'implementation', '1.0')
                .insertFile('implementation.properties', 'implementation')
                .dependsOn('implementation-dep')
                .publish()

        repo.module('shadow', 'runtimeOnly', '1.0')
                .insertFile('runtimeOnly.properties', 'runtimeOnly')
                .publish()

        buildFile.text = defaultBuildScript.replace('java', 'java-library')
        buildFile << """
            dependencies {
               api 'shadow:api:1.0'
               implementation 'shadow:implementation:1.0'
               runtimeOnly 'shadow:runtimeOnly:1.0'
            }
        """.stripIndent()

        when:
        versionRunner.withArguments('shadowJar').build()

        then:
        contains(output, ['api.properties',
                          'implementation.properties',
                          'runtimeOnly.properties',
                          'implementation-dep.properties'])
    }

    def "doesn't include compileOnly configuration by default"() {
        given:
        repo.module('shadow', 'a', '1.0')
                .insertFile('a.properties', 'a')
                .publish()

        repo.module('shadow', 'b', '1.0')
                .insertFile('b.properties', 'b')
                .publish()

        buildFile << """
            dependencies {
               runtimeOnly 'shadow:a:1.0'
               compileOnly 'shadow:b:1.0'
            }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        contains(output, ['a.properties'])

        and:
        doesNotContain(output, ['b.properties'])
    }

    def "default copying strategy"() {
        given:
        repo.module('shadow', 'a', '1.0')
                .insertFile('META-INF/MANIFEST.MF', 'MANIFEST A')
                .publish()

        repo.module('shadow', 'b', '1.0')
                .insertFile('META-INF/MANIFEST.MF', 'MANIFEST B')
                .publish()

        buildFile << """
            dependencies {
               runtimeOnly 'shadow:a:1.0'
               runtimeOnly 'shadow:b:1.0'
            }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        JarFile jar = new JarFile(output)
        assert jar.entries().collect().size() == 2
    }

    def "Class-Path in Manifest not added if empty"() {
        given:

        buildFile << """
            dependencies { implementation 'junit:junit:3.8.2' }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        assert output.exists()

        and:
        JarFile jar = new JarFile(output)
        Attributes attributes = jar.manifest.getMainAttributes()
        assert attributes.getValue('Class-Path') == null
    }

    @Issue('SHADOW-65')
    def "add shadow configuration to Class-Path in Manifest"() {
        given:

        buildFile << """
            // tag::shadowConfig[]
            dependencies {
              shadow 'junit:junit:3.8.2'
            }
            // end::shadowConfig[]

            // tag::jarManifest[]
            jar {
               manifest {
                   attributes 'Class-Path': '/libs/a.jar'
               }
            }
            // end::jarManifest[]
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        assert output.exists()

        and: 'SHADOW-65 - combine w/ existing Class-Path'
        JarFile jar = new JarFile(output)
        Attributes attributes = jar.manifest.getMainAttributes()
        String classpath = attributes.getValue('Class-Path')
        assert classpath == '/libs/a.jar junit-3.8.2.jar'

    }

    @Issue('SHADOW-92')
    def "do not include null value in Class-Path when jar file does not contain Class-Path"() {
        given:

        buildFile << """
            dependencies { shadow 'junit:junit:3.8.2' }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar').build()

        then:
        assert output.exists()

        and:
        JarFile jar = new JarFile(output)
        Attributes attributes = jar.manifest.getMainAttributes()
        String classpath = attributes.getValue('Class-Path')
        assert classpath == 'junit-3.8.2.jar'
    }

    @Issue('SHADOW-203')
    def "support ZipCompression.STORED"() {
        given:

        buildFile << """
            dependencies { shadow 'junit:junit:3.8.2' }

            shadowJar {
                zip64 true
                entryCompression = org.gradle.api.tasks.bundling.ZipEntryCompression.STORED
            }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar', '--stacktrace').build()

        then:
        assert output.exists()

    }
}
