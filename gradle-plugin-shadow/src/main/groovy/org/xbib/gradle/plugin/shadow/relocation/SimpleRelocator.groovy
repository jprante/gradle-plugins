package org.xbib.gradle.plugin.shadow.relocation

import org.xbib.gradle.plugin.shadow.internal.Utils

import java.util.regex.Pattern

/**
 * Modified from org.apache.maven.plugins.shade.relocation.SimpleRelocator
 */
class SimpleRelocator implements Relocator {

    private final String pattern

    private final String pathPattern

    private final String shadedPattern

    private final String shadedPathPattern

    private final Set<String> includes

    private final Set<String> excludes

    private final boolean rawString

    SimpleRelocator(String patt, String shadedPattern, List<String> includes, List<String> excludes) {
        this(patt, shadedPattern, includes, excludes, false)
    }

    SimpleRelocator(String patt, String shadedPattern, List<String> includes, List<String> excludes,
                           boolean rawString) {
        this.rawString = rawString

        if (rawString) {
            this.pathPattern = patt
            this.shadedPathPattern = shadedPattern

            this.pattern = null // not used for raw string relocator
            this.shadedPattern = null // not used for raw string relocator
        } else {
            if (patt == null) {
                this.pattern = ""
                this.pathPattern = ""
            } else {
                this.pattern = patt.replace('/', '.')
                this.pathPattern = patt.replace('.', '/')
            }

            if (shadedPattern != null) {
                this.shadedPattern = shadedPattern.replace('/', '.')
                this.shadedPathPattern = shadedPattern.replace('.', '/')
            } else {
                this.shadedPattern = "hidden." + this.pattern
                this.shadedPathPattern = "hidden/" + this.pathPattern
            }
        }

        this.includes = normalizePatterns(includes)
        this.excludes = normalizePatterns(excludes)
    }

    SimpleRelocator include(String pattern) {
        this.includes.addAll normalizePatterns([pattern])
        return this
    }

    SimpleRelocator exclude(String pattern) {
        this.excludes.addAll normalizePatterns([pattern])
        return this
    }

    private static Set<String> normalizePatterns(Collection<String> patterns) {
        Set<String> normalized = null
        if (patterns != null && !patterns.isEmpty()) {
            normalized = new LinkedHashSet<String>()
            for (String pattern : patterns) {
                String classPattern = pattern.replace('.', '/')
                normalized.add(classPattern)
                if (classPattern.endsWith("/*")) {
                    String packagePattern = classPattern.substring(0, classPattern.lastIndexOf('/'))
                    normalized.add(packagePattern)
                }
            }
        }
        return normalized ?: []
    }

    private boolean isIncluded(String path) {
        if (includes != null && !includes.isEmpty()) {
            for (String include : includes) {
                if (Utils.matchPath(include, path, true)) {
                    return true
                }
            }
            return false
        }
        return true
    }

    private boolean isExcluded(String path) {
        if (excludes != null && !excludes.isEmpty()) {
            for (String exclude : excludes) {
                if (Utils.matchPath(exclude, path, true)) {
                    return true
                }
            }
        }
        return false
    }

    boolean canRelocatePath(RelocatePathContext context) {
        String path = context.path
        if (rawString) {
            return Pattern.compile(pathPattern).matcher(path).find()
        }
        if (path.endsWith(".class")) {
            path = path.substring(0, path.length() - 6)
        }
        if (!isIncluded(path) || isExcluded(path)) {
            return false
        }
        // Allow for annoying option of an extra / on the front of a path.
        // See MSHADE-119 comes from getClass().getResource("/a/b/c.properties").
        return path.startsWith(pathPattern) || path.startsWith("/" + pathPattern)
    }

    boolean canRelocateClass(RelocateClassContext context) {
        String clazz = context.className
        RelocatePathContext pathContext = RelocatePathContext.builder().path(clazz.replace('.', '/')).stats(context.stats).build()
        return !rawString && clazz.indexOf('/') < 0 && canRelocatePath(pathContext)
    }

    String relocatePath(RelocatePathContext context) {
        String path = context.path
        context.stats.relocate(pathPattern, shadedPathPattern)
        if (rawString) {
            return path.replaceAll(pathPattern, shadedPathPattern)
        } else {
            return path.replaceFirst(pathPattern, shadedPathPattern)
        }
    }

    String relocateClass(RelocateClassContext context) {
        String clazz = context.className
        context.stats.relocate(pathPattern, shadedPathPattern)
        return clazz.replaceFirst(pattern, shadedPattern)
    }

    String applyToSourceContent(String sourceContent) {
        if (rawString) {
            return sourceContent
        } else {
            return sourceContent.replaceAll("\\b" + pattern, shadedPattern)
        }
    }
}
