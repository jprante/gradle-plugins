package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.Document

import org.asciidoctor.extension.IncludeProcessor
import org.asciidoctor.extension.PreprocessorReader

class DelegatingIncludeProcessor extends IncludeProcessor {

    private final Closure filter

    private final Closure cl

    DelegatingIncludeProcessor(Map options, Closure filter, @DelegatesTo(IncludeProcessor) Closure cl) {
        super(options)
        this.filter = filter
        this.cl = cl
        filter.delegate = this
        cl.delegate = this
        
    }
    @Override
    boolean handles(String target) {
        filter.call(target)
    }

    @Override
    void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        cl.call(document, reader, target, attributes)
    }
}
