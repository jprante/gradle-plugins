package org.xbib.gradle.plugin.shadow

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

class ShadowBasePlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = 'shadow'

    static final String CONFIGURATION_NAME = 'shadow'

    @Override
    void apply(Project project) {
        if (GradleVersion.current() < GradleVersion.version('4.10')) {
            throw new IllegalArgumentException('shadow requires at least Gradle 4.10')
        }
        project.extensions.create(EXTENSION_NAME, ShadowExtension, project)
        project.configurations.create(CONFIGURATION_NAME)
    }
}
