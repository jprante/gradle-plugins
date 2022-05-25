package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.internal.Utils
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

import static groovy.lang.Closure.IDENTITY

/**
 * Resources transformer that merges Properties files.
 *
 * <p>The default merge strategy discards duplicate values coming from additional
 * resources. This behavior can be changed by setting a value for the <tt>mergeStrategy</tt>
 * property, such as 'first' (default), 'latest' or 'append'. If the merge strategy is
 * 'latest' then the last value of a matching property entry will be used. If the
 * merge strategy is 'append' then the property values will be combined, using a
 * merge separator (default value is ','). The merge separator can be changed by
 * setting a value for the <tt>mergeSeparator</tt> property.</p>
 *
 * Say there are two properties files A and B with the
 * following entries:
 *
 * <strong>A</strong>
 * <ul>
 *   <li>key1 = value1</li>
 *   <li>key2 = value2</li>
 * </ul>
 *
 * <strong>B</strong>
 * <ul>
 *   <li>key2 = balue2</li>
 *   <li>key3 = value3</li>
 * </ul>
 *
 * With <tt>mergeStrategy = first</tt> you get
 *
 * <strong>C</strong>
 * <ul>
 *   <li>key1 = value1</li>
 *   <li>key2 = value2</li>
 *   <li>key3 = value3</li>
 * </ul>
 *
 * With <tt>mergeStrategy = latest</tt> you get
 *
 * <strong>C</strong>
 * <ul>
 *   <li>key1 = value1</li>
 *   <li>key2 = balue2</li>
 *   <li>key3 = value3</li>
 * </ul>
 *
 * With <tt>mergeStrategy = append</tt> and <tt>mergeSparator = ;</tt> you get
 *
 * <strong>C</strong>
 * <ul>
 *   <li>key1 = value1</li>
 *   <li>key2 = value2;balue2</li>
 *   <li>key3 = value3</li>
 * </ul>
 *
 * <p>There are three additional properties that can be set: <tt>paths</tt>, <tt>mappings</tt>,
 * and <tt>keyTransformer</tt>.
 * The first contains a list of strings or regexes that will be used to determine if
 * a path should be transformed or not. The merge strategy and merge separator are
 * taken from the global settings.</p>
 *
 * <p>The <tt>mappings</tt> property allows you to define merge strategy and separator per
 * path</p>. If either <tt>paths</tt> or <tt>mappings</tt> is defined then no other path
 * entries will be merged. <tt>mappings</tt> has precedence over <tt>paths</tt> if both
 * are defined.</p>
 *
 * <p>If you need to transform keys in properties files, e.g. because they contain class
 * names about to be relocated, you can set the <tt>keyTransformer</tt> property to a
 * closure that receives the original key and returns the key name to be used.</p>
 *
 * <p>Example:</p>
 * <pre>
 * import org.codehaus.griffon.gradle.shadow.transformers.*
 * shadowJar {
 *     transform(PropertiesFileTransformer) {
 *         paths = [
 *             'META-INF/editors/java.beans.PropertyEditor'
 *         ]
 *         keyTransformer = { key ->
 *             key.replaceAll('^(orig\.package\..*)$', 'new.prefix.$1')
 *         }
 *     }
 * }
 * </pre>
 */
class PropertiesFileTransformer implements Transformer {

    private static final String PROPERTIES_SUFFIX = '.properties'

    Map<String, Properties> propertiesEntries = [:]

    List<String> paths = []

    Map<String, Map<String, String>> mappings = [:]

    String mergeStrategy = 'first'

    String mergeSeparator = ','

    Closure<String> keyTransformer = IDENTITY

    @Override
    boolean canTransformResource(FileTreeElement element) {
        def path = element.relativePath.pathString
        if (mappings.containsKey(path)) {
            return true
        }
        for (key in mappings.keySet()) {
            if (path =~ /$key/) {
                return true
            }
        }
        if (path in paths) {
            return true
        }
        for (p in paths) {
            if (path =~ /$p/) {
                return true
            }
        }
        !mappings && !paths && path.endsWith(PROPERTIES_SUFFIX)
    }

    @Override
    void transform(TransformerContext context) {
        Properties props = propertiesEntries[context.path]
        Properties incoming = loadAndTransformKeys(context.inputStream)
        if (props == null) {
            propertiesEntries[context.path] = incoming
        } else {
            incoming.each { key, value ->
                if (props.containsKey(key)) {
                    switch (mergeStrategyFor(context.path).toLowerCase()) {
                        case 'latest':
                            props.put(key, value)
                            break
                        case 'append':
                            props.put(key, props.getProperty(key as String) + mergeSeparatorFor(context.path) + value)
                            break
                        case 'first':
                            break
                        default:
                            break
                    }
                } else {
                    props.put(key, value)
                }
            }
        }
    }

    @Override
    boolean hasTransformedResource() {
        propertiesEntries.size() > 0
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        propertiesEntries.each { String path, Properties props ->
            ZipEntry entry = new ZipEntry(path)
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            os.putNextEntry(entry)
            Utils.copyLarge(toInputStream(props), os)
            os.closeEntry()
        }
    }

    private Properties loadAndTransformKeys(InputStream is) {
        Properties props = new Properties()
        props.load(is)
        transformKeys(props)
    }

    private Properties transformKeys(Properties properties) {
        if (keyTransformer == IDENTITY)
            return properties
        def result = new Properties()
        properties.each { key, value ->
            result.put(keyTransformer.call(key), value)
        }
        result
    }

    private String mergeStrategyFor(String path) {
        if (mappings.containsKey(path)) {
            return mappings.get(path).mergeStrategy ?: mergeStrategy
        }
        for (key in mappings.keySet()) {
            if (path =~ /$key/) {
                return mappings.get(key).mergeStrategy ?: mergeStrategy
            }
        }
        mergeStrategy
    }

    private String mergeSeparatorFor(String path) {
        if (mappings.containsKey(path)) {
            return mappings.get(path).mergeSeparator ?: mergeSeparator
        }
        for (key in mappings.keySet()) {
            if (path =~ /$key/) {
                return mappings.get(key).mergeSeparator ?: mergeSeparator
            }
        }
        mergeSeparator
    }

    private static InputStream toInputStream(Properties props) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        props.store(baos, '')
        new ByteArrayInputStream(baos.toByteArray())
    }
}
