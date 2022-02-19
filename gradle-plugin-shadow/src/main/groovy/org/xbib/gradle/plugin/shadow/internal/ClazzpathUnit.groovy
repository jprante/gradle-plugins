package org.xbib.gradle.plugin.shadow.internal

class ClazzpathUnit {

    private final String s

    private final Map<String, Clazz> clazzes

    private final Map<String, Clazz> dependencies

    ClazzpathUnit(String s, Map<String, Clazz> clazzes, Map<String, Clazz> dependencies) {
        this.s = s
        this.clazzes = clazzes
        this.dependencies = dependencies
    }

    Set<Clazz> getClazzes() {
        new HashSet<Clazz>(clazzes.values())
    }

    Clazz getClazz( final String pClazzName ) {
        clazzes.get(pClazzName)
    }

    Set<Clazz> getDependencies() {
        new HashSet<Clazz>(dependencies.values())
    }

    Set<Clazz> getTransitiveDependencies() {
        Set<Clazz> all = new HashSet<Clazz>()
        for (Clazz clazz : clazzes.values()) {
            clazz.findTransitiveDependencies(all)
        }
        all
    }

    String toString() {
        s
    }
}
