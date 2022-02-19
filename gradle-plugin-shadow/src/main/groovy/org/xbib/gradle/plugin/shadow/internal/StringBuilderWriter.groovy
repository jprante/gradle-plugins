package org.xbib.gradle.plugin.shadow.internal

/**
 * {@link Writer} implementation that outputs to a {@link StringBuilder}.
 * <p>
 * <strong>NOTE:</strong> This implementation, as an alternative to
 * <code>java.io.StringWriter</code>, provides an <i>un-synchronized</i>
 * (i.e. for use in a single thread) implementation for better performance.
 * For safe usage with multiple {@link Thread}s then
 * <code>java.io.StringWriter</code> should be used.
 */
class StringBuilderWriter extends Writer {

    private final StringBuilder builder

    /**
     * Constructs a new {@link StringBuilder} instance with default capacity.
     */
    StringBuilderWriter() {
        this.builder = new StringBuilder()
    }

    /**
     * Constructs a new {@link StringBuilder} instance with the specified capacity.
     *
     * @param capacity The initial capacity of the underlying {@link StringBuilder}
     */
    StringBuilderWriter(int capacity) {
        this.builder = new StringBuilder(capacity)
    }

    /**
     * Constructs a new instance with the specified {@link StringBuilder}.
     *
     * <p>If {@code builder} is null a new instance with default capacity will be created.</p>
     *
     * @param builder The String builder. May be null.
     */
    StringBuilderWriter(StringBuilder builder) {
        this.builder = builder != null ? builder : new StringBuilder()
    }

    /**
     * Appends a single character to this Writer.
     *
     * @param value The character to append
     * @return This writer instance
     */
    @Override
    Writer append(char value) {
        builder.append(value)
        this
    }

    /**
     * Appends a character sequence to this Writer.
     *
     * @param value The character to append
     * @return This writer instance
     */
    @Override
    Writer append(CharSequence value) {
        builder.append(value)
        this
    }

    /**
     * Appends a portion of a character sequence to the {@link StringBuilder}.
     *
     * @param value The character to append
     * @param start The index of the first character
     * @param end The index of the last character + 1
     * @return This writer instance
     */
    @Override
    Writer append(CharSequence value, int start, int end) {
        builder.append(value, start, end)
        this
    }

    /**
     * Closing this writer has no effect.
     */
    @Override
    void close() {
        // no-op
    }

    /**
     * Flushing this writer has no effect.
     */
    @Override
    void flush() {
        // no-op
    }


    /**
     * Writes a String to the {@link StringBuilder}.
     *
     * @param value The value to write
     */
    @Override
    void write(String value) {
        if (value != null) {
            builder.append(value)
        }
    }

    /**
     * Writes a portion of a character array to the {@link StringBuilder}.
     *
     * @param value The value to write
     * @param offset The index of the first character
     * @param length The number of characters to write
     */
    @Override
    void write(char[] value, int offset, int length) {
        if (value != null) {
            builder.append(value, offset, length)
        }
    }

    /**
     * Returns the underlying builder.
     *
     * @return The underlying builder
     */
    StringBuilder getBuilder() {
        builder
    }

    /**
     * Returns {@link StringBuilder#toString()}.
     *
     * @return The contents of the String builder.
     */
    @Override
    String toString() {
        builder.toString()
    }
}