package org.xbib.gradle.plugin.shadow.util.file;

import groovy.lang.Closure;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.xbib.gradle.plugin.shadow.internal.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class TestFile extends File {

    public TestFile(File file, Object... path) {
        super(join(file, path).getAbsolutePath());
    }

    public TestFile(URI uri) {
        this(new File(uri));
    }

    public TestFile(String path) {
        this(new File(path));
    }

    public TestFile(URL url) {
        this(toUri(url));
    }

    Object writeReplace() throws ObjectStreamException {
        return new File(getAbsolutePath());
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return new File(getAbsolutePath()).getCanonicalFile();
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return new File(getAbsolutePath()).getCanonicalPath();
    }

    private static URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static File join(File file, Object[] path) {
        File current = file.getAbsoluteFile();
        for (Object p : path) {
            current = new File(current, p.toString());
        }
        try {
            return current.getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not canonicalise '%s'.", current), e);
        }
    }

    public TestFile file(Object... path) {
        try {
            return new TestFile(this, path);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format("Could not locate file '%s' relative to '%s'.", Arrays.toString(path), this), e);
        }
    }

    public List<TestFile> files(Object... paths) {
        List<TestFile> files = new ArrayList<TestFile>();
        for (Object path : paths) {
            files.add(file(path));
        }
        return files;
    }

    public TestFile write(Object content) {
        try {
            Utils.writeStringToFile(this, content.toString(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not write to test file '%s'", this), e);
        }
        return this;
    }

    public TestFile leftShift(Object content) {
        getParentFile().mkdirs();
        try {
            ResourceGroovyMethods.leftShift(this, content);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not append to test file '%s'", this), e);
        }
    }

    public TestFile[] listFiles() {
        File[] children = super.listFiles();
        TestFile[] files = new TestFile[children.length];
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            files[i] = new TestFile(child);
        }
        return files;
    }

    public String getText() {
        assertIsFile();
        try {
            return Utils.readFileToString(this, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not read from test file '%s'", this), e);
        }
    }

    public Map<String, String> getProperties() {
        assertIsFile();
        Properties properties = new Properties();
        try {
            try (FileInputStream inStream = new FileInputStream(this)) {
                properties.load(inStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, String> map = new HashMap<>();
        for (Object key : properties.keySet()) {
            map.put(key.toString(), properties.getProperty(key.toString()));
        }
        return map;
    }

    public Manifest getManifest() {
        assertIsFile();
        try {
            try (JarFile jarFile = new JarFile(this)) {
                return jarFile.getManifest();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public TestFile create(Closure structure) {
        assertTrue(isDirectory() || mkdirs());
        new TestWorkspaceBuilder(this).apply(structure);
        return this;
    }

    @Override
    public TestFile getParentFile() {
        return super.getParentFile() == null ? null : new TestFile(super.getParentFile());
    }

    @Override
    public String toString() {
        return getPath();
    }

    public TestFile assertExists() {
        assertTrue(exists(), String.format("%s does not exist", this));
        return this;
    }

    public TestFile assertIsFile() {
        assertTrue(isFile(), String.format("%s is not a file", this));
        return this;
    }

    public TestFile assertIsDir() {
        assertTrue(isDirectory(), String.format("%s is not a directory", this));
        return this;
    }

    public TestFile assertDoesNotExist() {
        assertFalse( exists(), String.format("%s should not exist", this));
        return this;
    }

    private byte[] getHash(String algorithm) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(Utils.readFileToByteArray(this));
            return messageDigest.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getPermissions() {
        assertExists();
        return new TestFileHelper(this).getPermissions();
    }

    public TestFile setPermissions(String permissions) {
        assertExists();
        new TestFileHelper(this).setPermissions(permissions);
        return this;
    }

    public TestFile setMode(int mode) {
        assertExists();
        new TestFileHelper(this).setMode(mode);
        return this;
    }

    private void visit(Set<String> names, String prefix, File file) {
        for (File child : file.listFiles()) {
            if (child.isFile()) {
                names.add(prefix + child.getName());
            } else if (child.isDirectory()) {
                visit(names, prefix + child.getName() + "/", child);
            }
        }
    }

    public TestFile createDir() {
        if (mkdirs()) {
            return this;
        }
        if (isDirectory()) {
            return this;
        }
        throw new AssertionError("Problems creating dir: " + this
                + ". Diagnostics: exists=" + this.exists() + ", isFile=" + this.isFile() + ", isDirectory=" + this.isDirectory());
    }

    public TestFile deleteDir() {
        new TestFileHelper(this).delete(false);
        return this;
    }

    /**
     * Attempts to delete this directory, ignoring failures to do so.
     * @return this
     */
    public TestFile maybeDeleteDir() {
        try {
            deleteDir();
        } catch (RuntimeException e) {
            // Ignore
        }
        return this;
    }

    public TestFile createFile() {
        new TestFile(getParentFile()).createDir();
        try {
            assertTrue(isFile() || createNewFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    public Snapshot snapshot() {
        assertIsFile();
        return new Snapshot(lastModified(), getHash("MD5"));
    }

    /*public void assertHasChangedSince(Snapshot snapshot) {
        Snapshot now = snapshot();
        assertTrue(now.modTime != snapshot.modTime || !Arrays.equals(now.hash, snapshot.hash));
    }

    public void assertContentsHaveChangedSince(Snapshot snapshot) {
        Snapshot now = snapshot();
        assertTrue(String.format("contents of %s have not changed", this), !Arrays.equals(now.hash, snapshot.hash));
    }

    public void assertContentsHaveNotChangedSince(Snapshot snapshot) {
        Snapshot now = snapshot();
        assertArrayEquals(String.format("contents of %s has changed", this), snapshot.hash, now.hash);
    }

    public void assertHasNotChangedSince(Snapshot snapshot) {
        Snapshot now = snapshot();
        assertEquals(String.format("last modified time of %s has changed", this), snapshot.modTime, now.modTime);
        assertArrayEquals(String.format("contents of %s has changed", this), snapshot.hash, now.hash);
    }
*/
    public class Snapshot {
        private final long modTime;
        private final byte[] hash;

        public Snapshot(long modTime, byte[] hash) {
            this.modTime = modTime;
            this.hash = hash;
        }
    }
}
