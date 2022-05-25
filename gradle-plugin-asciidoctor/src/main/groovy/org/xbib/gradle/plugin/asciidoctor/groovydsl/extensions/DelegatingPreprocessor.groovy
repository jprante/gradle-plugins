package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.Preprocessor
import org.asciidoctor.extension.PreprocessorReader

class DelegatingPreprocessor extends Preprocessor {

    private final Closure cl

    DelegatingPreprocessor(Map options, @DelegatesTo(Preprocessor) Closure cl) {
        super(options)
        this.cl = cl
        cl.delegate = this
    }

    @Override
    void process(Document document, PreprocessorReader reader) {
        cl.call(document, reader)
    }
}
