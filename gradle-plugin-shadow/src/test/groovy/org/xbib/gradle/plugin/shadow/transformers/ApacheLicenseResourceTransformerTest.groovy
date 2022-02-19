package org.xbib.gradle.plugin.shadow.transformers

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 * Test for {@link ApacheLicenseResourceTransformer}.
 *
 * Modified from org.apache.maven.plugins.shade.resources.ApacheLicenseResourceTransformerTest
 */
class ApacheLicenseResourceTransformerTest extends TransformerTestSupport {

    private ApacheLicenseResourceTransformer transformer

    static {
        /*
         * NOTE: The Turkish locale has an usual case transformation for the letters "I" and "i", making it a prime
         * choice to test for improper case-less string comparisions.
         */
        Locale.setDefault(new Locale("tr"))
    }

    @Before
    void setUp() {
        this.transformer = new ApacheLicenseResourceTransformer()
    }

    @Test
    void testCanTransformResource() {
        assertTrue(this.transformer.canTransformResource(getFileElement("META-INF/LICENSE")))
        assertTrue(this.transformer.canTransformResource(getFileElement("META-INF/LICENSE.TXT")))
        assertTrue(this.transformer.canTransformResource(getFileElement("META-INF/License.txt")))
        assertFalse(this.transformer.canTransformResource(getFileElement("META-INF/MANIFEST.MF")))
    }

}
