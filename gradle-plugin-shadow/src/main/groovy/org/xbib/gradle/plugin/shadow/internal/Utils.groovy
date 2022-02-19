package org.xbib.gradle.plugin.shadow.internal

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class Utils {

    private static final String EMPTY_STRING = ""

    private static final Character EXTENSION_SEPARATOR = '.' as Character

    private static final Character UNIX_SEPARATOR = '/' as Character

    private static final Character WINDOWS_SEPARATOR = '\\' as Character

    private static final Character SYSTEM_SEPARATOR = File.separatorChar as Character

    private static final int NOT_FOUND = -1

    private static final char OTHER_SEPARATOR

    static {
        if (SYSTEM_SEPARATOR == WINDOWS_SEPARATOR) {
            OTHER_SEPARATOR = UNIX_SEPARATOR
        } else {
            OTHER_SEPARATOR = WINDOWS_SEPARATOR
        }
    }

    static String separatorsToUnix(String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR as String) == NOT_FOUND) {
            return path
        }
        path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR)
    }

    static String getExtension(String fileName) throws IllegalArgumentException {
        if (fileName == null) {
            return null
        }
        int index = indexOfExtension(fileName)
        if (index == NOT_FOUND) {
            return EMPTY_STRING
        }
        return fileName.substring(index + 1)
    }

    static int indexOfExtension(String fileName) throws IllegalArgumentException {
        if (fileName == null) {
            return NOT_FOUND
        }
        if (SYSTEM_SEPARATOR == WINDOWS_SEPARATOR) {
            int offset = fileName.indexOf(':' as String, getAdsCriticalOffset(fileName))
            if (offset != -1) {
                throw new IllegalArgumentException("NTFS ADS separator (':') in file name is forbidden.")
            }
        }
        int extensionPos = fileName.lastIndexOf(EXTENSION_SEPARATOR as String)
        int lastSeparator = indexOfLastSeparator(fileName)
        lastSeparator > extensionPos ? NOT_FOUND : extensionPos
    }

    static int getAdsCriticalOffset(String fileName) {
        int offset1 = fileName.lastIndexOf(SYSTEM_SEPARATOR as String)
        int offset2 = fileName.lastIndexOf(OTHER_SEPARATOR as String)
        if (offset1 == -1) {
            if (offset2 == -1) {
                return 0
            }
            return offset2 + 1
        }
        if (offset2 == -1) {
            return offset1 + 1
        }
        return Math.max(offset1, offset2) + 1
    }

    static int indexOfLastSeparator(String fileName) {
        if (fileName == null) {
            return NOT_FOUND
        }
        int lastUnixPos = fileName.lastIndexOf(UNIX_SEPARATOR as String)
        int lastWindowsPos = fileName.lastIndexOf(WINDOWS_SEPARATOR as String)
        return Math.max(lastUnixPos, lastWindowsPos)
    }

    static String normalize(String fileName) {
        doNormalize(fileName, SYSTEM_SEPARATOR, true)
    }

    private static String doNormalize(String fileName, char separator, boolean keepSeparator) {
        if (fileName == null) {
            return null
        }
        failIfNullBytePresent(fileName)
        int size = fileName.length()
        if (size == 0) {
            return fileName
        }
        int prefix = getPrefixLength(fileName)
        if (prefix < 0) {
            return null
        }
        char[] array = new char[size + 2]
        fileName.getChars(0, fileName.length(), array, 0)
        char otherSeparator = separator == SYSTEM_SEPARATOR ? OTHER_SEPARATOR : SYSTEM_SEPARATOR
        for (int i = 0; i < array.length; i++) {
            if (array[i] == otherSeparator) {
                array[i] = separator
            }
        }
        boolean lastIsDirectory = true
        if (array[size - 1] != separator) {
            array[size++] = separator
            lastIsDirectory = false
        }
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == separator && array[i - 1] == separator) {
                System.arraycopy(array, i, array, i - 1, size - i)
                size--
                i--
            }
        }
        char dot = '.' as char
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == separator && array[i - 1] == dot &&
                    (i == prefix + 1 || array[i - 2] == separator)) {
                if (i == size - 1) {
                    lastIsDirectory = true
                }
                System.arraycopy(array, i + 1, array, i - 1, size - i)
                size -=2
                i--
            }
        }
        outer:
        for (int i = prefix + 2; i < size; i++) {
            if (array[i] == separator && array[i - 1] == dot && array[i - 2] == dot &&
                    (i == prefix + 2 || array[i - 3] == separator)) {
                if (i == prefix + 2) {
                    return null
                }
                if (i == size - 1) {
                    lastIsDirectory = true
                }
                int j
                for (j = i - 4 ; j >= prefix; j--) {
                    if (array[j] == separator) {
                        System.arraycopy(array, i + 1, array, j + 1, size - i)
                        size -= i - j
                        i = j + 1
                        continue outer
                    }
                }
                System.arraycopy(array, i + 1, array, prefix, size - i)
                size -= i + 1 - prefix
                i = prefix + 1
            }
        }
        if (size <= 0) {
            return EMPTY_STRING
        }
        if (size <= prefix) {
            return new String(array, 0, size)
        }
        if (lastIsDirectory && keepSeparator) {
            return new String(array, 0, size)
        }
        new String(array, 0, size - 1)
    }

    static int getPrefixLength(String fileName) {
        if (fileName == null) {
            return NOT_FOUND
        }
        int len = fileName.length()
        if (len == 0) {
            return 0
        }
        char ch0 = fileName.charAt(0) as char
        if (ch0 == ':' as char) {
            return NOT_FOUND
        }
        if (len == 1) {
            if (ch0 == '~' as char) {
                return 2
            }
            return isSeparator(ch0) ? 1 : 0
        }
        if (ch0 == '~' as char) {
            int posUnix = fileName.indexOf(UNIX_SEPARATOR as String, 1)
            int posWin = fileName.indexOf(WINDOWS_SEPARATOR as String, 1)
            if (posUnix == NOT_FOUND && posWin == NOT_FOUND) {
                return len + 1
            }
            posUnix = posUnix == NOT_FOUND ? posWin : posUnix
            posWin = posWin == NOT_FOUND ? posUnix : posWin
            return Math.min(posUnix, posWin) + 1
        }
        char ch1 = fileName.charAt(1) as char
        if (ch1 == ':' as char) {
            ch0 = Character.toUpperCase(ch0)
            if (ch0 >= ('A' as char) && ch0 <= ('Z' as char)) {
                if (len == 2 || !isSeparator(fileName.charAt(2))) {
                    return 2
                }
                return 3
            } else if (ch0 == UNIX_SEPARATOR) {
                return 1
            }
            return NOT_FOUND

        } else if (isSeparator(ch0) && isSeparator(ch1)) {
            int posUnix = fileName.indexOf(UNIX_SEPARATOR as String, 2)
            int posWin = fileName.indexOf(WINDOWS_SEPARATOR as String, 2)
            if (posUnix == NOT_FOUND && posWin == NOT_FOUND || posUnix == 2 || posWin == 2) {
                return NOT_FOUND
            }
            posUnix = posUnix == NOT_FOUND ? posWin : posUnix
            posWin = posWin == NOT_FOUND ? posUnix : posWin
            int pos = Math.min(posUnix, posWin) + 1
            String hostnamePart = fileName.substring(2, pos - 1)
            return isValidHostName(hostnamePart) ? pos : NOT_FOUND
        } else {
            return isSeparator(ch0) ? 1 : 0
        }
    }

    static boolean isSeparator(char ch) {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR
    }

    private static void failIfNullBytePresent(String path) {
        int len = path.length()
        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == 0 as char) {
                throw new IllegalArgumentException("Null byte present in file/path name. There are no " +
                        "known legitimate use cases for such data, but several injection attacks may use it")
            }
        }
    }

    static String removeExtension(String fileName) {
        if (fileName == null) {
            return null
        }
        failIfNullBytePresent(fileName)
        int index = indexOfExtension(fileName)
        if (index == NOT_FOUND) {
            return fileName
        }
        fileName.substring(0, index)
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4

    static long copyLarge(Reader input, Writer output) throws IOException {
        return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE])
    }

    static long copyLarge(Reader input, Writer output, final char[] buffer) throws IOException {
        long count = 0
        int n
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n)
            count += n
        }
        count
    }

    static long copyLarge(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    static long copy(InputStream input, OutputStream output, int bufferSize) throws IOException {
        return copyLarge(input, output, new byte[bufferSize])
    }

    static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0
        int n
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n)
            count += n
        }
        return count
    }

    static void writeStringToFile(File file, String data, Charset encoding) throws IOException {
        writeStringToFile(file, data, encoding, false)
    }

    static void writeStringToFile(File file, String data, Charset encoding, boolean append) throws IOException {
        OutputStream out = append ?
                Files.newOutputStream(file.toPath(), StandardOpenOption.APPEND) : Files.newOutputStream(file.toPath())
        out.withCloseable { outputStream ->
            write(data, outputStream, encoding)
        }
    }

    static void write(String data, OutputStream output, Charset encoding) throws IOException {
        if (data != null) {
            output.write(data.getBytes(encoding))
        }
    }

    static String readFileToString(File file, Charset encoding) throws IOException {
        InputStream inputStream = Files.newInputStream(file.toPath())
        inputStream.withCloseable { is ->
            return toString(is, encoding)
        }
    }

    static String toString(InputStream input, Charset encoding) throws IOException {
        StringBuilderWriter sw = new StringBuilderWriter()
        sw.withCloseable { writer ->
            copyLarge(new InputStreamReader(input, encoding), writer)
            return sw.toString()
        }
    }

    static byte[] readFileToByteArray(File file) throws IOException {
        InputStream inputStream = Files.newInputStream(file.toPath())
        inputStream.withCloseable { is ->
            long fileLength = file.length()
            return fileLength > 0 ? toByteArray(is, fileLength) : toByteArray(is)
        }
    }

    static byte[] toByteArray(InputStream input, long size) throws IOException {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + size)
        }
        return toByteArray(input, (int) size)
    }

    static byte[] toByteArray(InputStream input, int size) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("Size must be equal or greater than zero: " + size)
        }
        if (size == 0) {
            return new byte[0]
        }
        byte[] data = new byte[size]
        int offset = 0
        int read = 0
        while (offset < size && (read = input.read(data, offset, size - offset)) != -1) {
            offset += read
        }
        if (offset != size) {
            throw new IOException("Unexpected read size. current: " + offset + ", expected: " + size)
        }
        data
    }

    static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        outputStream.withCloseable {
            copyLarge(inputStream, outputStream)
            return outputStream.toByteArray()
        }
    }

    private static final String PATTERN_HANDLER_PREFIX = "["

    private static final String PATTERN_HANDLER_SUFFIX = "]"

    private static final String REGEX_HANDLER_PREFIX = "%regex" + PATTERN_HANDLER_PREFIX

    private static final String ANT_HANDLER_PREFIX = "%ant" + PATTERN_HANDLER_PREFIX

    static boolean matchPath(String pattern, String str, boolean isCaseSensitive) {
        matchPath(pattern, str, File.separator, isCaseSensitive)
    }

    static boolean matchPath(String pattern, String str, String separator, boolean isCaseSensitive) {
        if (isRegexPrefixedPattern(pattern)) {
            pattern = pattern.substring(REGEX_HANDLER_PREFIX.length(), pattern.length() - PATTERN_HANDLER_SUFFIX.length())
            return str.matches(pattern)
        } else {
            if (isAntPrefixedPattern(pattern)) {
                pattern = pattern.substring(ANT_HANDLER_PREFIX.length(),
                        pattern.length() - PATTERN_HANDLER_SUFFIX.length())
            }

            return matchAntPathPattern(pattern, str, separator, isCaseSensitive)
        }
    }

    static boolean isRegexPrefixedPattern(String pattern) {
        pattern.length() > (REGEX_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1) &&
                pattern.startsWith(REGEX_HANDLER_PREFIX) && pattern.endsWith( PATTERN_HANDLER_SUFFIX)
    }

    static boolean isAntPrefixedPattern(String pattern) {
        pattern.length() > (ANT_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1) &&
                pattern.startsWith(ANT_HANDLER_PREFIX) && pattern.endsWith(PATTERN_HANDLER_SUFFIX)
    }

    static boolean matchAntPathPattern(String pattern, String str, String separator, boolean isCaseSensitive) {
        if (separatorPatternStartSlashMismatch(pattern, str, separator)) {
            return false
        }
        List<String> patDirs = tokenizePathToString(pattern, separator)
        List<String> strDirs = tokenizePathToString(str, separator)
        matchAntPathPattern(patDirs, strDirs, isCaseSensitive)
    }

    static boolean separatorPatternStartSlashMismatch(String pattern, String str, String separator) {
        str.startsWith(separator) != pattern.startsWith(separator)
    }

    static List<String> tokenizePathToString(String path, String separator) {
        List<String> ret = []
        StringTokenizer st = new StringTokenizer(path, separator)
        while (st.hasMoreTokens()) {
            ret.add(st.nextToken())
        }
        ret
    }

    static boolean matchAntPathPattern(List<String> patDirs, List<String> strDirs, boolean isCaseSensitive) {
        int patIdxStart = 0
        int patIdxEnd = patDirs.size() - 1
        int strIdxStart = 0
        int strIdxEnd = strDirs.size() - 1
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxStart)
            if (patDir.equals("**")) {
                break
            }
            if (!match(patDir, strDirs.get(strIdxStart), isCaseSensitive)) {
                return false
            }
            patIdxStart++
            strIdxStart++
        }
        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs.get(i).equals( "**")) {
                    return false
                }
            }
            return true
        } else {
            if (patIdxStart > patIdxEnd) {
                return false
            }
        }
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxEnd)
            if (patDir.equals("**")) {
                break
            }
            if (!match(patDir, strDirs.get(strIdxEnd), isCaseSensitive)) {
                return false
            }
            patIdxEnd--
            strIdxEnd--
        }
        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs.get(i).equals("**")) {
                    return false
                }
            }
            return true
        }
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patDirs.get(i).equals("**")) {
                    patIdxTmp = i
                    break
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                patIdxStart++
                continue
            }
            int patLength = (patIdxTmp - patIdxStart - 1)
            int strLength = (strIdxEnd - strIdxStart + 1)
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patDirs.get(patIdxStart + j + 1)
                    String subStr = strDirs.get(strIdxStart + i + j)
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue strLoop
                    }
                }
                foundIdx = strIdxStart + i
                break
            }
            if (foundIdx == -1) {
                return false
            }
            patIdxStart = patIdxTmp
            strIdxStart = foundIdx + patLength
        }
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!patDirs.get(i).equals("**")) {
                return false
            }
        }
        return true
    }

    static boolean match(String pattern, String str, boolean isCaseSensitive) {
        char[] patArr = pattern.toCharArray()
        char[] strArr = str.toCharArray()
        match(patArr, strArr, isCaseSensitive)
    }

    static boolean match(char[] patArr, char[] strArr, boolean isCaseSensitive) {
        int patIdxStart = 0
        int patIdxEnd = patArr.length - 1
        int strIdxStart = 0
        int strIdxEnd = strArr.length - 1
        char ch

        boolean containsStar = false
        for (char aPatArr : patArr) {
            if (aPatArr == ('*' as char)) {
                containsStar = true
                break
            }
        }
        if (!containsStar) {
            if (patIdxEnd != strIdxEnd) {
                return false
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i]
                if ( ch != ('?' as char) && !equals(ch, strArr[i], isCaseSensitive)) {
                    return false
                }
            }
            return true
        }
        if (patIdxEnd == 0) {
            return true
        }
        while ((ch = patArr[patIdxStart]) != ('*' as char) && strIdxStart <= strIdxEnd) {
            if ( ch != ('?' as char) && !equals( ch, strArr[strIdxStart], isCaseSensitive)) {
                return false
            }
            patIdxStart++
            strIdxStart++
        }
        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if ( patArr[i] != ('*' as char)) {
                    return false
                }
            }
            return true
        }
        while ((ch = patArr[patIdxEnd] ) != ('*' as char) && strIdxStart <= strIdxEnd) {
            if (ch != ('?' as char) && !equals( ch, strArr[strIdxEnd], isCaseSensitive)) {
                return false
            }
            patIdxEnd--
            strIdxEnd--
        }
        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if ( patArr[i] != ('*' as char)) {
                    return false
                }
            }
            return true
        }
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if ( patArr[i] == ('*' as char)) {
                    patIdxTmp = i
                    break
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                patIdxStart++
                continue
            }
            int patLength = ( patIdxTmp - patIdxStart - 1 )
            int strLength = ( strIdxEnd - strIdxStart + 1 )
            int foundIdx = -1
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1]
                    if (ch != ('?' as char) && !equals( ch, strArr[strIdxStart + i + j], isCaseSensitive)) {
                        continue strLoop
                    }
                }
                foundIdx = strIdxStart + i
                break
            }
            if (foundIdx == -1) {
                return false
            }
            patIdxStart = patIdxTmp
            strIdxStart = foundIdx + patLength
        }
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != ('*' as char)) {
                return false
            }
        }
        return true
    }

    static boolean equals(char c1, char c2, boolean isCaseSensitive) {
        if (c1 == c2) {
            return true
        }
        if (!isCaseSensitive) {
            if (Character.toUpperCase(c1) == Character.toUpperCase(c2)
                    || Character.toLowerCase(c1) == Character.toLowerCase(c2)) {
                return true
            }
        }
        return false
    }

}
