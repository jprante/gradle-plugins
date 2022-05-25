package org.xbib.gradle.plugin.asciidoctor.groovydsl.extensions

import org.asciidoctor.ast.Document
import org.asciidoctor.extension.Treeprocessor

class DelegatingTreeprocessor extends Treeprocessor {

    private final Closure cl

    DelegatingTreeprocessor(Map options, @DelegatesTo(Treeprocessor) Closure cl) {
        super(options)
        this.cl = cl
        cl.delegate = this
    }

    @Override
    Document process(Document document) {
        def ret = cl.call(document)
        if (!(ret in Document)) {
            // Assume that the closure does something as last
            // statement that was not intended to be the return value
            // -> Return null
            null
        } else {
            ret
        }
    }
}
