package org.xbib.gradle.plugin.shadow.relocation

/**
 * Modified from org.apache.maven.plugins.shade.relocation.Relocator
 */
interface Relocator {

    boolean canRelocatePath(RelocatePathContext context)

    String relocatePath(RelocatePathContext context)

    boolean canRelocateClass(RelocateClassContext context)

    String relocateClass(RelocateClassContext context)

    String applyToSourceContent(String sourceContent)
}
