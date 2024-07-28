package net.neoforged.gradle.common.util.hash;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class Hashing {
    private static final HashFunction MD5 = MessageDigestHashFunction.of("MD5");
    private static final HashFunction SHA1 = MessageDigestHashFunction.of("SHA-1");
    private static final HashFunction SHA256 = MessageDigestHashFunction.of("SHA-256");
    private static final HashFunction SHA512 = MessageDigestHashFunction.of("SHA-512");
    private static final HashFunction DEFAULT;

    private Hashing() {
    }

    public static Hasher newHasher() {
        return DEFAULT.newHasher();
    }

    public static PrimitiveHasher newPrimitiveHasher() {
        return DEFAULT.newPrimitiveHasher();
    }

    public static HashCode signature(Class<?> type) {
        return signature("CLASS:" + type.getName());
    }

    public static HashCode signature(String thing) {
        Hasher hasher = DEFAULT.newHasher();
        hasher.putString("SIGNATURE");
        hasher.putString(thing);
        return hasher.hash();
    }

    public static HashCode hashBytes(byte[] bytes) {
        return DEFAULT.hashBytes(bytes);
    }

    public static HashCode hashString(CharSequence string) {
        return DEFAULT.hashString(string);
    }

    public static HashCode hashStream(InputStream stream) throws IOException {
        return DEFAULT.hashStream(stream);
    }

    public static HashCode hashFile(File file) throws IOException {
        return DEFAULT.hashFile(file);
    }

    public static HashCode hashDirectory(File file) throws IOException {
        Hasher hasher = newHasher();
        hasher.putString("DIRECTORY");
        hasher.putString(file.getName());
        for (File listFile : Objects.requireNonNull(file.listFiles())) {
            final HashCode innerHash;
            if (listFile.isFile()) {
                 innerHash = hashFile(listFile);
            } else {
                innerHash = hashDirectory(listFile);
            }
            hasher.putHash(innerHash);
        }
        return hasher.hash();
    }

    public static HashCode hashHashable(Hashable hashable) {
        Hasher hasher = newHasher();
        hasher.put(hashable);
        return hasher.hash();
    }

    public static HashFunction defaultFunction() {
        return DEFAULT;
    }

    public static HashFunction md5() {
        return MD5;
    }

    public static HashFunction sha1() {
        return SHA1;
    }

    public static HashFunction sha256() {
        return SHA256;
    }

    public static HashFunction sha512() {
        return SHA512;
    }

    static {
        DEFAULT = sha512();
    }

    private static class DefaultHasher implements Hasher {
        private final PrimitiveHasher hasher;

        public DefaultHasher(PrimitiveHasher unsafeHasher) {
            this.hasher = unsafeHasher;
        }

        public void putByte(byte value) {
            this.hasher.putInt(1);
            this.hasher.putByte(value);
        }

        public void putBytes(byte[] bytes) {
            this.hasher.putInt(bytes.length);
            this.hasher.putBytes(bytes);
        }

        public void putBytes(byte[] bytes, int off, int len) {
            this.hasher.putInt(len);
            this.hasher.putBytes(bytes, off, len);
        }

        public void putHash(HashCode hashCode) {
            this.hasher.putInt(hashCode.length());
            this.hasher.putHash(hashCode);
        }

        public void putInt(int value) {
            this.hasher.putInt(4);
            this.hasher.putInt(value);
        }

        public void putLong(long value) {
            this.hasher.putInt(8);
            this.hasher.putLong(value);
        }

        public void putDouble(double value) {
            this.hasher.putInt(8);
            this.hasher.putDouble(value);
        }

        public void putBoolean(boolean value) {
            this.hasher.putInt(1);
            this.hasher.putBoolean(value);
        }

        public void putString(CharSequence value) {
            this.hasher.putInt(value.length());
            this.hasher.putString(value);
        }

        public void put(Hashable hashable) {
            hashable.appendToHasher(this);
        }

        public void putNull() {
            this.putInt(0);
        }

        @Override
        public void put(@Nullable Object value) {
            this.put(value, true);
        }

        @Override
        public void put(@Nullable Object value, boolean throwOnUnknownType) {
            if (value == null) {
                this.putNull();
            } else if (value instanceof Hashable) {
                this.put((Hashable)value);
            } else if (value instanceof CharSequence) {
                this.putString((CharSequence)value);
            } else if (value instanceof byte[]) {
                this.putBytes((byte[])value);
            } else if (value instanceof Boolean) {
                this.putBoolean((Boolean)value);
            } else if (value instanceof Integer) {
                this.putInt((Integer)value);
            } else if (value instanceof Long) {
                this.putLong((Long)value);
            } else if (value instanceof Double) {
                this.putDouble((Double) value);
            } else if (value instanceof Byte) {
                this.putByte((Byte)value);
            } else {
                if (throwOnUnknownType) {
                    throw new IllegalArgumentException("Unknown type: " + value.getClass());
                }
            }
        }

        public HashCode hash() {
            return this.hasher.hash();
        }
    }

    private static class MessageDigestHasher implements PrimitiveHasher {
        private final ByteBuffer buffer;
        private MessageDigest digest;

        public MessageDigestHasher(MessageDigest digest) {
            this.buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            this.digest = digest;
        }

        private MessageDigest getDigest() {
            if (this.digest == null) {
                throw new IllegalStateException("Cannot reuse hasher!");
            } else {
                return this.digest;
            }
        }

        public void putByte(byte b) {
            this.getDigest().update(b);
        }

        public void putBytes(byte[] bytes) {
            this.getDigest().update(bytes);
        }

        public void putBytes(byte[] bytes, int off, int len) {
            this.getDigest().update(bytes, off, len);
        }

        private void update(int length) {
            this.getDigest().update(this.buffer.array(), 0, length);
            castBuffer(this.buffer).clear();
        }

        private static <T extends Buffer> Buffer castBuffer(T byteBuffer) {
            return byteBuffer;
        }

        public void putInt(int value) {
            this.buffer.putInt(value);
            this.update(4);
        }

        public void putLong(long value) {
            this.buffer.putLong(value);
            this.update(8);
        }

        public void putDouble(double value) {
            long longValue = Double.doubleToRawLongBits(value);
            this.putLong(longValue);
        }

        public void putBoolean(boolean value) {
            this.putByte((byte)(value ? 1 : 0));
        }

        public void putString(CharSequence value) {
            this.putBytes(value.toString().getBytes(Charset.defaultCharset()));
        }

        public void putHash(HashCode hashCode) {
            hashCode.appendToHasher(this);
        }

        public HashCode hash() {
            byte[] bytes = this.getDigest().digest();
            this.digest = null;
            return HashCode.fromBytes(bytes, HashCode.Usage.SAFE_TO_REUSE_BYTES);
        }
    }

    private static class RegularMessageDigestHashFunction extends MessageDigestHashFunction {
        private final String algorithm;

        public RegularMessageDigestHashFunction(String algorithm, int hashBits) {
            super(hashBits);
            this.algorithm = algorithm;
        }

        public String getAlgorithm() {
            return this.algorithm;
        }

        protected MessageDigest createDigest() {
            try {
                return MessageDigest.getInstance(this.algorithm);
            } catch (NoSuchAlgorithmException var2) {
                NoSuchAlgorithmException e = var2;
                throw new AssertionError(e);
            }
        }
    }

    private static class CloningMessageDigestHashFunction extends MessageDigestHashFunction {
        private final MessageDigest prototype;

        public CloningMessageDigestHashFunction(MessageDigest prototype, int hashBits) {
            super(hashBits);
            this.prototype = prototype;
        }

        public String getAlgorithm() {
            return this.prototype.getAlgorithm();
        }

        protected MessageDigest createDigest() {
            try {
                return (MessageDigest)this.prototype.clone();
            } catch (CloneNotSupportedException var2) {
                CloneNotSupportedException e = var2;
                throw new AssertionError(e);
            }
        }
    }

    private abstract static class MessageDigestHashFunction implements HashFunction {
        private final int hexDigits;

        public MessageDigestHashFunction(int hashBits) {
            this.hexDigits = hashBits / 4;
        }

        public static MessageDigestHashFunction of(String algorithm) {
            MessageDigest prototype;
            try {
                prototype = MessageDigest.getInstance(algorithm);
            } catch (NoSuchAlgorithmException var5) {
                throw new IllegalArgumentException("Cannot instantiate digest algorithm: " + algorithm);
            }

            int hashBits = prototype.getDigestLength() * 8;

            try {
                prototype.clone();
                return new CloningMessageDigestHashFunction(prototype, hashBits);
            } catch (CloneNotSupportedException var4) {
                return new RegularMessageDigestHashFunction(algorithm, hashBits);
            }
        }

        public PrimitiveHasher newPrimitiveHasher() {
            MessageDigest digest = this.createDigest();
            return new MessageDigestHasher(digest);
        }

        public Hasher newHasher() {
            return new DefaultHasher(this.newPrimitiveHasher());
        }

        public HashCode hashBytes(byte[] bytes) {
            PrimitiveHasher hasher = this.newPrimitiveHasher();
            hasher.putBytes(bytes);
            return hasher.hash();
        }

        public HashCode hashString(CharSequence string) {
            PrimitiveHasher hasher = this.newPrimitiveHasher();
            hasher.putString(string);
            return hasher.hash();
        }

        public HashCode hashStream(InputStream stream) throws IOException {
            HashingOutputStream hashingOutputStream = this.primitiveStreamHasher();
            ByteStreams.copy(stream, hashingOutputStream);
            return hashingOutputStream.hash();
        }

        public HashCode hashFile(File file) throws IOException {
            if (file.exists()) {
                if (file.isDirectory()) {
                    final Hasher hasher = this.newHasher();

                    for (File listFile : Objects.requireNonNull(file.listFiles())) {
                        final HashCode innerHash = this.hashFile(listFile);
                        hasher.putHash(innerHash);
                    }

                    return hasher.hash();
                } else {
                    HashingOutputStream hashingOutputStream = this.primitiveStreamHasher();
                    Files.copy(file, hashingOutputStream);
                    return hashingOutputStream.hash();
                }
            } else {
                return HashCode.fromString("");
            }
        }

        private HashingOutputStream primitiveStreamHasher() {
            return new HashingOutputStream(this, ByteStreams.nullOutputStream());
        }

        protected abstract MessageDigest createDigest();

        public int getHexDigits() {
            return this.hexDigits;
        }

        public String toString() {
            return this.getAlgorithm();
        }
    }
}