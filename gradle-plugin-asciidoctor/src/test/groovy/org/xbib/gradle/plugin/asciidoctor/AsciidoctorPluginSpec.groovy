package org.xbib.gradle.plugin.asciidoctor

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.Specification

class AsciidoctorPluginSpec extends Specification {

    private static final String ASCIIDOCTOR = 'asciidoctor'

    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
    }

    @SuppressWarnings('MethodName')
    def "Applies plugin and checks default setup"() {
        expect:
            project.tasks.findByName(ASCIIDOCTOR) == null
        when:
            project.apply plugin: AsciidoctorPlugin
        then:
            Task asciidoctorTask = project.tasks.findByName(ASCIIDOCTOR)
            asciidoctorTask != null
            asciidoctorTask.group == 'Documentation'
            asciidoctorTask.sourceDir == project.file('src/docs/asciidoc')
            asciidoctorTask.outputDir == new File(project.buildDir, 'asciidoc')

            project.tasks.findByName('clean') != null
    }

    @Ignore("Method 'getDependencyResolutionBroadcast' is unknown")
    def "testPluginWithAlternativeAsciidoctorVersion"() {
        expect:
        project.tasks.findByName(ASCIIDOCTOR) == null

        when:
        project.apply plugin: AsciidoctorPlugin

        def expectedVersion = 'my.expected.version-SNAPSHOT'
        project.asciidoctorj.version = expectedVersion

        def expectedDslVersion = 'dsl.' + expectedVersion
        project.asciidoctorj.groovyDslVersion = expectedDslVersion

        def config = project.project.configurations.getByName('asciidoctor')
        def dependencies = config.dependencies
        assert dependencies.isEmpty();

        // mock-trigger beforeResolve() to avoid 'real' resolution of dependencies
        DependencyResolutionListener broadcast = config.getDependencyResolutionBroadcast()
        ResolvableDependencies incoming = config.getIncoming()
        broadcast.beforeResolve(incoming)
        def dependencyHandler = project.getDependencies();

        then:
        assert dependencies.contains(dependencyHandler.create(AsciidoctorPlugin.ASCIIDOCTORJ_CORE_DEPENDENCY + expectedVersion))
    }
}
