package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.DocinfoProcessor

class DelegatingDocinfoProcessor extends DocinfoProcessor {

    private final Closure cl

    DelegatingDocinfoProcessor(Map options, @DelegatesTo(DocinfoProcessor) Closure cl) {
        super(options)
        this.cl = cl
        cl.delegate = this
    }

    @Override
    String process(Document document) {
        cl.call(document)
    }
}
