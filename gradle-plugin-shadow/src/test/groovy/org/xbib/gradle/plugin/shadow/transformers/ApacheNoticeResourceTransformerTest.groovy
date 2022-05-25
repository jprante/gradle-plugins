package org.xbib.gradle.plugin.shadow.transformers

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * Test for {@link ApacheNoticeResourceTransformer}.
 * Modified from org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformerTest
 */
class ApacheNoticeResourceTransformerTest extends TransformerTestSupport {

    private ApacheNoticeResourceTransformer transformer

    static {
        /*
         * NOTE: The Turkish locale has an usual case transformation for the letters "I" and "i", making it a prime
         * choice to test for improper case-less string comparisions.
         */
        Locale.setDefault(new Locale("tr"))
    }

    @Before
    void setUp() {
        this.transformer = new ApacheNoticeResourceTransformer()
    }

    @Test
    void testCanTransformResource() {
        assertTrue(this.transformer.canTransformResource(getFileElement("META-INF/NOTICE")))
        assertTrue(this.transformer.canTransformResource(getFileElement("META-INF/NOTICE.TXT")))
        assertTrue(this.transformer.canTransformResource(getFileElement("META-INF/Notice.txt")))
        assertFalse(this.transformer.canTransformResource(getFileElement("META-INF/MANIFEST.MF")))
    }

}
