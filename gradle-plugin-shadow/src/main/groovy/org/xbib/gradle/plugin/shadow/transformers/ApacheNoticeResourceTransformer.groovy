package org.xbib.gradle.plugin.shadow.transformers

import org.gradle.api.file.FileTreeElement
import org.xbib.gradle.plugin.shadow.zip.ZipEntry
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

import java.text.SimpleDateFormat

/**
 * Merges <code>META-INF/NOTICE.TXT</code> files.
 * Modified from org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer
 */
class ApacheNoticeResourceTransformer implements Transformer {
    Set<String> entries = new LinkedHashSet<String>()

    Map<String, Set<String>> organizationEntries = new LinkedHashMap<String, Set<String>>()

    String projectName = ""

    boolean addHeader = true

    String preamble1 = "// ------------------------------------------------------------------\n" +
            "// NOTICE file corresponding to the section 4d of The Apache License,\n" +
            "// Version 2.0, in this case for "

    String preamble2 = "\n// ------------------------------------------------------------------\n"

    String preamble3 = "This product includes software developed at\n"

    String organizationName = "The Apache Software Foundation"

    String organizationURL = "http://www.apache.org/"

    String inceptionYear = "2006"

    String copyright

    /**
     * The file encoding of the <code>NOTICE</code> file.
     */
    String encoding

    private static final String NOTICE_PATH = "META-INF/NOTICE"

    private static final String NOTICE_TXT_PATH = "META-INF/NOTICE.txt"

    @Override
    boolean canTransformResource(FileTreeElement element) {
        def path = element.relativePath.pathString
        if (NOTICE_PATH.equalsIgnoreCase(path) || NOTICE_TXT_PATH.equalsIgnoreCase(path)) {
            return true
        }

        return false
    }

    @Override
    void transform(TransformerContext context) {
        if (entries.isEmpty()) {
            String year = new SimpleDateFormat("yyyy").format(new Date())
            if (!inceptionYear.equals(year)) {
                year = inceptionYear + "-" + year
            }

            //add headers
            if (addHeader) {
                entries.add(preamble1 + projectName + preamble2)
            } else {
                entries.add("")
            }
            //fake second entry, we'll look for a real one later
            entries.add(projectName + "\nCopyright " + year + " " + organizationName + "\n")
            entries.add(preamble3 + organizationName + " (" + organizationURL + ").\n")
        }

        BufferedReader reader
        if (encoding != null && !encoding.isEmpty()) {
            reader = new BufferedReader(new InputStreamReader(context.inputStream, encoding))
        } else {
            reader = new BufferedReader(new InputStreamReader(context.inputStream))
        }

        String line = reader.readLine()
        StringBuffer sb = new StringBuffer()
        Set<String> currentOrg = null
        int lineCount = 0
        while (line != null) {
            String trimedLine = line.trim()

            if (!trimedLine.startsWith("//")) {
                if (trimedLine.length() > 0) {
                    if (trimedLine.startsWith("- ")) {
                        //resource-bundle 1.3 mode
                        if (lineCount == 1
                                && sb.toString().indexOf("This product includes/uses software(s) developed by") != -1) {
                            currentOrg = organizationEntries.get(sb.toString().trim())
                            if (currentOrg == null) {
                                currentOrg = new TreeSet<String>()
                                organizationEntries.put(sb.toString().trim(), currentOrg)
                            }
                            sb = new StringBuffer()
                        } else if (sb.length() > 0 && currentOrg != null) {
                            currentOrg.add(sb.toString())
                            sb = new StringBuffer()
                        }

                    }
                    sb.append(line).append("\n")
                    lineCount++
                } else {
                    String ent = sb.toString()
                    if (ent.startsWith(projectName) && ent.indexOf("Copyright ") != -1) {
                        copyright = ent
                    }
                    if (currentOrg == null) {
                        entries.add(ent)
                    } else {
                        currentOrg.add(ent)
                    }
                    sb = new StringBuffer()
                    lineCount = 0
                    currentOrg = null
                }
            }

            line = reader.readLine()
        }
        if (sb.length() > 0) {
            if (currentOrg == null) {
                entries.add(sb.toString())
            } else {
                currentOrg.add(sb.toString())
            }
        }
    }

    @Override
    boolean hasTransformedResource() {
        return true
    }

    @Override
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        ZipEntry zipEntry = new ZipEntry(NOTICE_PATH)
        zipEntry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, zipEntry.time)
        os.putNextEntry(zipEntry)

        Writer pow
        if (encoding != null && !encoding.isEmpty()) {
            pow = new OutputStreamWriter(os, encoding)
        } else {
            pow = new OutputStreamWriter(os)
        }
        PrintWriter writer = new PrintWriter(pow)

        int count = 0
        for (String line : entries) {
            ++count
            if (line.equals(copyright) && count != 2) {
                continue
            }

            if (count == 2 && copyright != null) {
                writer.print(copyright)
                writer.print('\n')
            } else {
                writer.print(line)
                writer.print('\n')
            }
            if (count == 3) {
                for (Map.Entry<String, Set<String>> entry : organizationEntries.entrySet()) {
                    writer.print(entry.getKey())
                    writer.print('\n')
                    for (String l : entry.getValue()) {
                        writer.print(l)
                    }
                    writer.print('\n')
                }
            }
        }
        writer.flush()
        entries.clear()
    }
}
