package org.xbib.gradle.plugin.shadow.tasks

import org.xbib.gradle.plugin.shadow.ShadowStats
import org.xbib.gradle.plugin.shadow.internal.DependencyFilter
import org.xbib.gradle.plugin.shadow.relocation.Relocator
import org.xbib.gradle.plugin.shadow.relocation.SimpleRelocator
import org.xbib.gradle.plugin.shadow.transformers.ServiceFileTransformer
import org.xbib.gradle.plugin.shadow.transformers.Transformer
import org.gradle.api.Action
import org.gradle.api.file.CopySpec

interface ShadowSpec extends CopySpec {

    ShadowSpec minimize()

    ShadowSpec minimize(Action<DependencyFilter> configureClosure)

    ShadowSpec dependencies(Action<DependencyFilter> configure)

    ShadowSpec transform(Class<? extends Transformer> clazz)
            throws InstantiationException, IllegalAccessException

    ShadowSpec transform(Class<? extends Transformer> clazz, Action<? extends Transformer> configure)
            throws InstantiationException, IllegalAccessException

    ShadowSpec transform(Transformer transformer)

    ShadowSpec mergeServiceFiles()

    ShadowSpec mergeServiceFiles(String rootPath)

    ShadowSpec mergeServiceFiles(Action<ServiceFileTransformer> configureClosure)

    ShadowSpec mergeGroovyExtensionModules()

    ShadowSpec append(String resourcePath)

    ShadowSpec relocate(String pattern, String destination)

    ShadowSpec relocate(String pattern, String destination, Action<SimpleRelocator> configure)

    ShadowSpec relocate(Relocator relocator)

    ShadowSpec relocate(Class<? extends Relocator> clazz)
            throws InstantiationException, IllegalAccessException

    ShadowSpec relocate(Class<? extends Relocator> clazz, Action<? extends Relocator> configure)
            throws InstantiationException, IllegalAccessException

    ShadowStats getStats()
}
