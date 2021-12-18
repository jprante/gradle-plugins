package org.xbib.gradle.plugin.asciidoctor

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.dsl.DependencyHandler

class AsciidoctorPlugin implements Plugin<Project> {

    static final String ASCIIDOCTOR = 'asciidoctor'

    static final String ASCIIDOCTORJ = 'asciidoctorj'

    static final String ASCIIDOCTORJ_CORE_DEPENDENCY = 'org.asciidoctor:asciidoctorj:'

    void apply(Project project) {
        project.apply(plugin: 'base')

        AsciidoctorExtension extension = project.extensions.create(ASCIIDOCTORJ, AsciidoctorExtension, project)

        project.afterEvaluate {
            if(project.extensions.asciidoctorj.addDefaultRepositories) {
                project.repositories {
                    mavenCentral()
                }
            }
        }

        Configuration configuration = project.configurations.maybeCreate(ASCIIDOCTOR)
        project.logger.info("[Asciidoctor] asciidoctorj: ${extension.version}")

        configuration.incoming.beforeResolve(new Action<ResolvableDependencies>() {
            @SuppressWarnings('UnusedMethodParameter')
            void execute(ResolvableDependencies resolvableDependencies) {
                DependencyHandler dependencyHandler = project.dependencies
                def dependencies = configuration.dependencies
                dependencies.add(dependencyHandler.create(ASCIIDOCTORJ_CORE_DEPENDENCY + extension.version))
            }
        })

        project.task(ASCIIDOCTOR,
                type: AsciidoctorTask,
                group: 'Documentation',
                description: 'Converts AsciiDoc files and copies the output files and related resources to the build directory.') {
            classpath = configuration
        }
    }
}
