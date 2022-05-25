package org.xbib.gradle.plugin.shadow.util.repo

import org.gradle.internal.UncheckedException
import org.xbib.gradle.plugin.shadow.util.file.TestFile

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


abstract class AbstractModule {
    /**
     * @param cl A closure that is passed a writer to use to generate the content.
     */
    protected void publish(TestFile file, Closure cl) {
        def hashBefore = file.exists() ? getHash(file, "sha1") : null
        def tmpFile = file.parentFile.file("${file.name}.tmp")

        tmpFile.withWriter("utf-8") {
            cl.call(it)
        }

        def hashAfter = getHash(tmpFile, "sha1")
        if (hashAfter == hashBefore) {
            // Already published
            return
        }

        assert !file.exists() || file.delete()
        assert tmpFile.renameTo(file)
        onPublish(file)
    }

    protected void publishWithStream(TestFile file, Closure cl) {
        def hashBefore = file.exists() ? getHash(file, "sha1") : null
        def tmpFile = file.parentFile.file("${file.name}.tmp")

        tmpFile.withOutputStream {
            cl.call(it)
        }

        def hashAfter = getHash(tmpFile, "sha1")
        if (hashAfter == hashBefore) {
            // Already published
            return
        }

        assert !file.exists() || file.delete()
        assert tmpFile.renameTo(file)
        onPublish(file)
    }

    protected abstract onPublish(TestFile file)

    static TestFile getSha1File(TestFile file) {
        getHashFile(file, "sha1")
    }

    static TestFile sha1File(TestFile file) {
        hashFile(file, "sha1", 40)
    }

    static TestFile getMd5File(TestFile file) {
        getHashFile(file, "md5")
    }

    static TestFile md5File(TestFile file) {
        hashFile(file, "md5", 32)
    }

    private static TestFile hashFile(TestFile file, String algorithm, int len) {
        def hashFile = getHashFile(file, algorithm)
        def hash = getHash(file, algorithm)
        hashFile.text = String.format("%0${len}x", hash)
        return hashFile
    }

    private static TestFile getHashFile(TestFile file, String algorithm) {
        file.parentFile.file("${file.name}.${algorithm}")
    }

    protected static BigInteger getHash(TestFile file, String algorithm) {
        createHash(file, algorithm.toUpperCase())
    }

    static BigInteger createHash(File file, String algorithm) {
        try {
            return createHash(new FileInputStream(file), algorithm)
        } catch (UncheckedIOException e) {
            throw new UncheckedIOException(String.format("Failed to create %s hash for file %s.",
                    algorithm, file.getAbsolutePath()), e.getCause())
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e)
        }
    }

    static BigInteger createHash(InputStream instr, String algorithm) {
        MessageDigest messageDigest
        try {
            messageDigest = createMessageDigest(algorithm)
            byte[] buffer = new byte[4096]
            try {
                while (true) {
                    int nread = instr.read(buffer)
                    if (nread < 0) {
                        break
                    }
                    messageDigest.update(buffer, 0, nread)
                }
            } finally {
                instr.close()
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e)
        }
        new BigInteger(1, messageDigest.digest())
    }

    private static MessageDigest createMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm)
        } catch (NoSuchAlgorithmException e) {
            throw UncheckedException.throwAsUncheckedException(e)
        }
    }
}
