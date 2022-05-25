package org.xbib.gradle.plugin.shadow.impl

import org.objectweb.asm.commons.Remapper
import org.xbib.gradle.plugin.shadow.relocation.RelocateClassContext
import org.xbib.gradle.plugin.shadow.relocation.RelocatePathContext
import org.xbib.gradle.plugin.shadow.relocation.Relocator
import org.xbib.gradle.plugin.shadow.tasks.ShadowCopyAction
import org.xbib.gradle.plugin.shadow.ShadowStats

import java.util.regex.Matcher
import java.util.regex.Pattern

class RelocatorRemapper extends Remapper {

    private final Pattern classPattern = Pattern.compile("(\\[*)?L(.+)")

    List<Relocator> relocators
    ShadowStats stats

    RelocatorRemapper(List<Relocator> relocators, ShadowStats stats) {
        this.relocators = relocators
        this.stats = stats
    }

    boolean hasRelocators() {
        return !relocators.empty
    }

    Object mapValue(Object object) {
        if (object instanceof String) {
            String name = (String) object
            String value = name

            String prefix = ""
            String suffix = ""

            Matcher m = classPattern.matcher(name)
            if (m.matches()) {
                prefix = m.group(1) + "L"
                suffix = ""
                name = m.group(2)
            }

            RelocateClassContext classContext = RelocateClassContext.builder().className(name).stats(stats).build()
            RelocatePathContext pathContext = RelocatePathContext.builder().path(name).stats(stats).build()
            for (Relocator r : relocators) {
                if (r.canRelocateClass(classContext)) {
                    value = prefix + r.relocateClass(classContext) + suffix
                    break
                } else if (r.canRelocatePath(pathContext)) {
                    value = prefix + r.relocatePath(pathContext) + suffix
                    break
                }
            }

            return value
        }

        return super.mapValue(object)
    }

    String map(String name) {
        String value = name

        String prefix = ""
        String suffix = ""

        Matcher m = classPattern.matcher(name)
        if (m.matches()) {
            prefix = m.group(1) + "L"
            suffix = ""
            name = m.group(2)
        }

        RelocatePathContext pathContext = RelocatePathContext.builder().path(name).stats(stats).build()
        for (Relocator r : relocators) {
            if (r.canRelocatePath(pathContext)) {
                value = prefix + r.relocatePath(pathContext) + suffix
                break
            }
        }

        return value
    }

    String mapPath(String path) {
        map(path.substring(0, path.indexOf('.')))
    }

    String mapPath(ShadowCopyAction.RelativeArchivePath path) {
        mapPath(path.pathString)
    }

}
