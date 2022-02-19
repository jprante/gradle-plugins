package org.xbib.gradle.plugin.shadow.internal

import org.junit.Test

import java.nio.charset.Charset

import static org.junit.Assert.assertEquals

class UtilsTest {

    @Test
    void testCopyLarge() {
        def temporaryFile = File.createTempFile("test", ".dat")
        temporaryFile.deleteOnExit()
        InputStream inputStream = getClass().getResourceAsStream('/jar2.jar')
        Utils.copyLarge(inputStream, new FileOutputStream(temporaryFile))
        assertEquals(143847, temporaryFile.bytes.length)
    }

    @Test
    void testFileWrite() {
        File file = new File('build/testfile')
        Utils.writeStringToFile(file, 'Hello world', Charset.defaultCharset())
        assertEquals('Hello world', file.text)
        file.delete()
    }

    @Test
    void testReadFileToString() {
        File file = new File('build/testfile')
        Utils.writeStringToFile(file, 'Hello world', Charset.defaultCharset())
        String text = Utils.readFileToString(file, Charset.defaultCharset())
        assertEquals('Hello world', text)
        file.delete()
    }


    @Test
    void testReadByteArray() {
        File file = new File('build/testfile')
        Utils.writeStringToFile(file, 'Hello world', Charset.defaultCharset())
        byte[] bytes = Utils.readFileToByteArray(file)
        assertEquals('Hello world', new String(bytes, Charset.defaultCharset()))
        file.delete()
    }
}
