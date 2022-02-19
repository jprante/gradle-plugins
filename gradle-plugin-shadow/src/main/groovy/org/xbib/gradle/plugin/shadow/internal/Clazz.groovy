package org.xbib.gradle.plugin.shadow.internal

class Clazz implements Comparable<Clazz> {

    private final Set<Clazz> dependencies = new HashSet<Clazz>()

    private final Set<Clazz> references = new HashSet<Clazz>()

    private final Set<ClazzpathUnit> units = new HashSet<ClazzpathUnit>()

    private final String name

    Clazz( final String pName ) {
        name = pName;
    }

    String getName() {
        name
    }

    void addClazzpathUnit(ClazzpathUnit pUnit ) {
        units.add(pUnit)
    }

    void removeClazzpathUnit( ClazzpathUnit pUnit ) {
        units.remove(pUnit)
    }

    Set<ClazzpathUnit> getClazzpathUnits() {
        units
    }

    void addDependency( final Clazz pClazz ) {
        pClazz.references.add(this)
        dependencies.add(pClazz)
    }

    public void removeDependency( final Clazz pClazz ) {
        pClazz.references.remove(this)
        dependencies.remove(pClazz)
    }

    Set<Clazz> getDependencies() {
        return dependencies
    }

    Set<Clazz> getReferences() {
        return references
    }

    Set<Clazz> getTransitiveDependencies() {
        final Set<Clazz> all = new HashSet<Clazz>();
        findTransitiveDependencies(all);
        return all;
    }

    void findTransitiveDependencies( final Set<? super Clazz> pAll ) {
        for (Clazz clazz : dependencies) {
            if (!pAll.contains(clazz)) {
                pAll.add(clazz)
                clazz.findTransitiveDependencies(pAll)
            }
        }
    }

    boolean equals( final Object pO ) {
        if (pO.getClass() != Clazz.class) {
            return false
        }
        Clazz c = (Clazz) pO
        name.equals(c.name)
    }

    int hashCode() {
        name.hashCode()
    }

    int compareTo( final Clazz pO ) {
        name.compareTo(((Clazz) pO).name)
    }

    String toString() {
        name
    }
}
