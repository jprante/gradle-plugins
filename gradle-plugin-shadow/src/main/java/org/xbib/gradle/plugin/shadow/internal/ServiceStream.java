package org.xbib.gradle.plugin.shadow.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class was moved to java because groovy compiler reports "no such property: count"
 */
public class ServiceStream extends ByteArrayOutputStream {

    public ServiceStream() {
        super(1024);
    }

    public void append(InputStream is) throws IOException {
        if (count > 0 && buf[count - 1] != '\n' && buf[count - 1] != '\r') {
            byte[] newline = new byte[] {'\n'};
            write(newline, 0, newline.length);
        }
        is.transferTo(this);
    }

    public InputStream toInputStream() {
        return new ByteArrayInputStream(buf, 0, count);
    }
}
