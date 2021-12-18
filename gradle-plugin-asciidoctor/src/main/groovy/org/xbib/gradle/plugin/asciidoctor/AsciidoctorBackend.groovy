package org.xbib.gradle.plugin.asciidoctor

enum AsciidoctorBackend {
    HTML('html'),
    DOCBOOK('docbook'),
    HTML5('html5'),
    DOCBOOK45('docbook45'),
    DOCBOOK5('docbook5'),
    EPUB3('epub3'),
    PDF('pdf'),
    XHTML('xhtml'),
    XHTML5('xhtml5'),

    private final static Map<String, AsciidoctorBackend> ALL_BACKENDS
    private final String id

    static {
        ALL_BACKENDS = values().collectEntries{ [it.id, it] }.asImmutable()
    }

    private AsciidoctorBackend(String id) {
        this.id = id
    }

    String getId() {
        id
    }

    static boolean isBuiltIn(String name) {
        ALL_BACKENDS.containsKey(name)
    }
}
