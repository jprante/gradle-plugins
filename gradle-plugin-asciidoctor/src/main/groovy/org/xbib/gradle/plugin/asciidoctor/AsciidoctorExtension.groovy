package org.xbib.gradle.plugin.asciidoctor

import org.gradle.api.Project

class AsciidoctorExtension {

    String version = '2.5.3'

    boolean addDefaultRepositories = true

    final Project project

    AsciidoctorExtension(Project project) {
        this.project = project
    }
}
