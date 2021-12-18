package org.xbib.gradle.plugin.asciidoctor

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.WorkResult

class ResourceCopyProxyImpl implements ResourceCopyProxy {

    Project project

    ResourceCopyProxyImpl(Project p) { project = p }

    @Override
    WorkResult copy(File outputDir, CopySpec spec) {
        project.copy {
            into outputDir
            with spec
        }
    }
}
