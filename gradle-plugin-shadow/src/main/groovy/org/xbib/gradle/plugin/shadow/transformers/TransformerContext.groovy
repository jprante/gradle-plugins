package org.xbib.gradle.plugin.shadow.transformers

import groovy.transform.Canonical
import groovy.transform.builder.Builder
import org.xbib.gradle.plugin.shadow.ShadowStats
import org.xbib.gradle.plugin.shadow.relocation.Relocator
import org.xbib.gradle.plugin.shadow.tasks.ShadowCopyAction

@Canonical
@Builder
class TransformerContext {

    String path

    InputStream inputStream

    List<Relocator> relocators

    ShadowStats stats

    static long getEntryTimestamp(boolean preserveFileTimestamps, long entryTime) {
        preserveFileTimestamps ? entryTime : ShadowCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES
    }
}
