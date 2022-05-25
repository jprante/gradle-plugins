package org.xbib.gradle.plugin.shadow.transformers

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

/**
 * Test for {@link AppendingTransformer}.
 */
class AppendingTransformerTest extends TransformerTestSupport {

    private AppendingTransformer transformer

    static
    {
        /*
         * NOTE: The Turkish locale has an usual case transformation for the letters "I" and "i", making it a prime
         * choice to test for improper case-less string comparisions.
         */
        Locale.setDefault(new Locale("tr"))
    }

    @Before
    void setUp() {
        this.transformer = new AppendingTransformer()
    }

    @Test
    void testCanTransformResource() {
        this.transformer.resource = "abcdefghijklmnopqrstuvwxyz"

        assertTrue(this.transformer.canTransformResource(getFileElement("abcdefghijklmnopqrstuvwxyz")))
        assertTrue(this.transformer.canTransformResource(getFileElement("ABCDEFGHIJKLMNOPQRSTUVWXYZ")))
        assertFalse(this.transformer.canTransformResource(getFileElement("META-INF/MANIFEST.MF")))
    }

}
