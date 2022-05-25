package org.xbib.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

class RpmPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(BasePlugin)
        project.ext.Rpm = Rpm.class
    }
}
