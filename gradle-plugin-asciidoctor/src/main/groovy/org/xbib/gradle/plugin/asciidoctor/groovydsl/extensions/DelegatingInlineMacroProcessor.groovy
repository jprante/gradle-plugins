package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.ContentNode
import org.asciidoctor.extension.InlineMacroProcessor

class DelegatingInlineMacroProcessor extends InlineMacroProcessor {

    private final Closure cl

    DelegatingInlineMacroProcessor(String name, Map options, @DelegatesTo(InlineMacroProcessor) Closure cl) {
        super(name, options)
        this.cl = cl
        cl.delegate = this
    }

    @Override
    Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        cl.call(parent, target, attributes)
    }
}
