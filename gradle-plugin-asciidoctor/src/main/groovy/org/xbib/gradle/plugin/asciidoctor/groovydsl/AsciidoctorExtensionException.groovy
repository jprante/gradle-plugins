package org.xbib.gradle.plugin.asciidoctor.groovydsl

class AsciidoctorExtensionException extends Exception {

    AsciidoctorExtensionException() {
    }

    AsciidoctorExtensionException(String message) {
        super(message)
    }

    AsciidoctorExtensionException(String message, Throwable cause) {
        super(message, cause)
    }

    AsciidoctorExtensionException(Throwable cause) {
        super(cause)
    }
}
