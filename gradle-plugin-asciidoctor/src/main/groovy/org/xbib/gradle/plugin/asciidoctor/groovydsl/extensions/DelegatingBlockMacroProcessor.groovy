package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.StructuralNode
import org.asciidoctor.extension.BlockMacroProcessor

class DelegatingBlockMacroProcessor extends BlockMacroProcessor {

    private final Closure cl

    DelegatingBlockMacroProcessor(String name, Map options, @DelegatesTo(BlockMacroProcessor) Closure cl) {
        super(name, options)
        this.cl = cl
        cl.delegate = this
    }

    @Override
    Object process(StructuralNode parent, String target, Map<String, Object> attributes) {
        cl.call(parent, target, attributes)
    }
}
