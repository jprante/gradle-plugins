package org.xbib.gradle.plugin.shadow.transformers

import org.xbib.gradle.plugin.shadow.ShadowStats
import junit.framework.TestCase
import org.xbib.gradle.plugin.shadow.relocation.Relocator

/**
 * Tests {@link ApacheLicenseResourceTransformer} parameters.
 *
 * Modified from org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformerParameterTests
 */
class ApacheNoticeResourceTransformerParameterTests extends TestCase {

    private static final String NOTICE_RESOURCE = "META-INF/NOTICE"
    private ApacheNoticeResourceTransformer subject
    private ShadowStats stats

    protected void setUp() {
        super.setUp()
        subject = new ApacheNoticeResourceTransformer()
        stats = new ShadowStats()
    }

    void testNoParametersShouldNotThrowNullPointerWhenNoInput() {
        processAndFailOnNullPointer("")
    }

    void testNoParametersShouldNotThrowNullPointerWhenNoLinesOfInput() {
        processAndFailOnNullPointer("Some notice text")
    }

    void testNoParametersShouldNotThrowNullPointerWhenOneLineOfInput() {
        processAndFailOnNullPointer("Some notice text\n")
    }

    void testNoParametersShouldNotThrowNullPointerWhenTwoLinesOfInput() {
        processAndFailOnNullPointer("Some notice text\nSome notice text\n")
    }

    void testNoParametersShouldNotThrowNullPointerWhenLineStartsWithSlashSlash() {
        processAndFailOnNullPointer("Some notice text\n//Some notice text\n")
    }

    void testNoParametersShouldNotThrowNullPointerWhenLineIsSlashSlash() {
        processAndFailOnNullPointer("//\n")
    }

    void testNoParametersShouldNotThrowNullPointerWhenLineIsEmpty() {
        processAndFailOnNullPointer("\n")
    }

    private void processAndFailOnNullPointer(final String noticeText) {
        try {
            final ByteArrayInputStream noticeInputStream = new ByteArrayInputStream(noticeText.getBytes())
            final List<Relocator> emptyList = Collections.emptyList()
            subject.transform(TransformerContext.builder().path(NOTICE_RESOURCE).inputStream(noticeInputStream).relocators(emptyList).stats(stats).build())
        }
        catch (NullPointerException e) {
            fail("Null pointer should not be thrown when no parameters are set.")
        }
    }
}
