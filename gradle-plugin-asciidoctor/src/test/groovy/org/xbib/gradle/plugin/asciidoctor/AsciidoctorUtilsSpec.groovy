package org.xbib.gradle.plugin.asciidoctor

import spock.lang.Specification

class AsciidoctorUtilsSpec extends Specification {

    static s = File.separator

    def 'finds relative paths'(String target, String base, String relative) {
        expect:
            AsciidoctorUtils.getRelativePath(new File(target), new File(base)) == relative
        where:
            target             | base               | relative
            'src/test/groovy'  | 'src'              | "test${s}groovy"
            'src/test/groovy/' | 'src/'             | "test${s}groovy"
            'src/test/groovy'  | 'src/'             | "test${s}groovy"
            'src/test/groovy/' | 'src'              | "test${s}groovy"
            'src'              | 'src/test/groovy'  | "..${s}.."
            'src/'             | 'src/test/groovy/' | "..${s}.."
            'src'              | 'src/test/groovy/' | "..${s}.."
            'src/'             | 'src/test/groovy'  | "..${s}.."
            'src/test'         | 'src/test'         | ''
            'src/test/'        | 'src/test/'        | ''
            'src/test'         | 'src/test/'        | ''
            'src/test/'        | 'src/test'         | ''
    }
}
