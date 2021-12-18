package org.xbib.gradle.plugin.asciidoctor

import java.util.regex.Pattern

class AsciidoctorUtils {

    static String getRelativePath(File target, File base) throws IOException {
        String[] baseComponents = base.canonicalPath.split(Pattern.quote(File.separator))
        String[] targetComponents = target.canonicalPath.split(Pattern.quote(File.separator))
        int index = 0
        for (; index < targetComponents.length && index < baseComponents.length; ++index) {
            if (!targetComponents[index].equals(baseComponents[index])) {
                break
            }
        }
        StringBuilder result = new StringBuilder()
        if (index != baseComponents.length) {
            for (int i = index; i < baseComponents.length; ++i) {
                if (i != index) {
                    result.append(File.separator)
                }
                result.append('..')
            }
        }
        for (int i = index; i < targetComponents.length; ++i) {
            if (i != index) {
                result.append(File.separator)
            }
            result.append(targetComponents[i])
        }
        result.toString()
    }
}
