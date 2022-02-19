package org.xbib.gradle.plugin.shadow

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.xbib.gradle.plugin.shadow.tasks.ShadowJar

import javax.inject.Inject

class ShadowJavaPlugin implements Plugin<Project> {

    static final String SHADOW_JAR_TASK_NAME = 'shadowJar'

    static final String SHADOW_GROUP = 'Shadow'

    private final ProjectConfigurationActionContainer configurationActionContainer;

    @Inject
    ShadowJavaPlugin(ProjectConfigurationActionContainer configurationActionContainer) {
        this.configurationActionContainer = configurationActionContainer
    }

    @Override
    void apply(Project project) {
        configureShadowTask(project)
        project.configurations.compileClasspath.extendsFrom project.configurations.shadow
    }

    protected void configureShadowTask(Project project) {
        JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)
        project.tasks.register(SHADOW_JAR_TASK_NAME, ShadowJar) { shadow ->
            shadow.group = SHADOW_GROUP
            shadow.description = 'Create a combined JAR of project and runtime dependencies'
            shadow.archiveClassifier.set("all")
            shadow.manifest.inheritFrom project.tasks.jar.manifest
            def libsProvider = project.provider { -> [project.tasks.jar.manifest.attributes.get('Class-Path')] }
            def files = project.objects.fileCollection().from { ->
                project.configurations.findByName(ShadowBasePlugin.CONFIGURATION_NAME)
            }
            shadow.doFirst {
                if (!files.empty) {
                    def libs = libsProvider.get()
                    libs.addAll files.collect { "${it.name}" }
                    manifest.attributes 'Class-Path': libs.findAll { it }.join(' ')
                }
            }
            shadow.from(convention.sourceSets.main.output)
            shadow.configurations = [project.configurations.findByName('runtimeClasspath') ?
                                             project.configurations.runtimeClasspath : project.configurations.runtime]
            shadow.exclude('META-INF/INDEX.LIST', 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA', 'module-info.class')
            shadow.dependencies {
                exclude(dependency(project.dependencies.gradleApi()))
            }
            project.artifacts.add(ShadowBasePlugin.CONFIGURATION_NAME, shadow)
        }
    }
}
