package org.xbib.gradle.plugin.asciidoctor.groovydsl

import org.asciidoctor.Asciidoctor
import org.asciidoctor.extension.BlockMacroProcessor
import org.asciidoctor.extension.BlockProcessor
import org.asciidoctor.extension.DocinfoProcessor
import org.asciidoctor.extension.IncludeProcessor
import org.asciidoctor.extension.InlineMacroProcessor
import org.asciidoctor.extension.Postprocessor
import org.asciidoctor.extension.Preprocessor
import org.asciidoctor.extension.Treeprocessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingBlockMacroProcessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingBlockProcessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingDocinfoProcessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingIncludeProcessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingPostprocessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingPreprocessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingInlineMacroProcessor
import org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions.DelegatingTreeprocessor

class AsciidoctorExtensionHandler {

    static final String OPTION_NAME = 'name'

    static final String OPTION_FILTER = 'filter'

    static final String OPTION_CONTEXTS = 'contexts'

    private final Asciidoctor asciidoctor

    AsciidoctorExtensionHandler(Asciidoctor asciidoctor) {
        this.asciidoctor = asciidoctor
    }

    void block(String blockName, @DelegatesTo(BlockProcessor) Closure cl) {
        block([(OPTION_NAME): blockName], cl)
    }

    void block(Map options=[:], @DelegatesTo(BlockProcessor) Closure cl) {
        if (!options.containsKey(OPTION_NAME)) {
            throw new IllegalArgumentException('Block must define a name!')
        }
        if (!options.containsKey(OPTION_CONTEXTS)) {
            //TODO: What are sensible defaults?
            options[OPTION_CONTEXTS] = [':open', ':paragraph']
        }
        asciidoctor.javaExtensionRegistry().block(new DelegatingBlockProcessor(options, cl))
    }

    void block_macro(Map options, @DelegatesTo(BlockMacroProcessor) Closure cl) {
        asciidoctor.javaExtensionRegistry().blockMacro(new DelegatingBlockMacroProcessor(options[OPTION_NAME] as String, options, cl))
    }

    void block_macro(String name, @DelegatesTo(BlockMacroProcessor) Closure cl) {
        block_macro([(OPTION_NAME): name], cl)
    }

    void postprocessor(Map options=[:], @DelegatesTo(Postprocessor) Closure cl) {
        asciidoctor.javaExtensionRegistry().postprocessor(new DelegatingPostprocessor(options, cl))
    }

    void preprocessor(Map options=[:], @DelegatesTo(Preprocessor) Closure cl) {
        asciidoctor.javaExtensionRegistry().preprocessor(new DelegatingPreprocessor(options, cl))
    }

    void include_processor(Map options=[:], @DelegatesTo(IncludeProcessor) Closure cl) {
        Closure filter = options[OPTION_FILTER] as Closure
        Map optionsWithoutFilter = options - options.subMap([OPTION_FILTER])
        asciidoctor.javaExtensionRegistry().includeProcessor(new DelegatingIncludeProcessor(optionsWithoutFilter, filter, cl))
    }

    void inline_macro(Map options, @DelegatesTo(InlineMacroProcessor) Closure cl) {
        asciidoctor.javaExtensionRegistry().inlineMacro(new DelegatingInlineMacroProcessor(options[OPTION_NAME] as String, options, cl))
    }

    void inline_macro(String macroName, @DelegatesTo(InlineMacroProcessor) Closure cl) {
        inline_macro([(OPTION_NAME): macroName], cl)
    }

    void treeprocessor(Map options=[:], @DelegatesTo(Treeprocessor) Closure cl) {
        asciidoctor.javaExtensionRegistry().treeprocessor(new DelegatingTreeprocessor(options, cl))
    }

    void docinfo_processor(Map options=[:], @DelegatesTo(DocinfoProcessor) Closure cl) {
        asciidoctor.javaExtensionRegistry().docinfoProcessor(new DelegatingDocinfoProcessor(options, cl))
    }
}
