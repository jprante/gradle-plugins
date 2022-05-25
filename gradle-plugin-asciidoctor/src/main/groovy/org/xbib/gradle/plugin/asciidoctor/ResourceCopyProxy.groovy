package org.xbib.gradle.plugin.asciidoctor

import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.WorkResult

interface ResourceCopyProxy {

    WorkResult copy(File outputDir, CopySpec spec)

}
