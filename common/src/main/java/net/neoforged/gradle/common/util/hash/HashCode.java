package net.neoforged.gradle.common.util.hash;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigInteger;

public abstract class HashCode implements Serializable, Comparable<HashCode> {
    private static final int MIN_NUMBER_OF_BYTES = 4;
    private static final int MAX_NUMBER_OF_BYTES = 255;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private HashCode() {
    }

    static HashCode fromBytes(byte[] bytes, Usage usage) {
        switch (bytes.length) {
            case 16:
                return new HashCode128(bytesToLong(bytes, 0), bytesToLong(bytes, 8));
            default:
                return new ByteArrayBackedHashCode(usage == Usage.CLONE_BYTES_IF_NECESSARY ? (byte[])bytes.clone() : bytes);
        }
    }

    public static HashCode fromBytes(byte[] bytes) {
        if (bytes.length >= MIN_NUMBER_OF_BYTES && bytes.length <= MAX_NUMBER_OF_BYTES) {
            return fromBytes(bytes, Usage.CLONE_BYTES_IF_NECESSARY);
        } else {
            throw new IllegalArgumentException(String.format("Invalid hash code length: %d bytes", bytes.length));
        }
    }

    public static HashCode fromString(String string) {
        int length = string.length();
        if (length % 2 == 0 && length >= 8 && length <= 510) {
            byte[] bytes = new byte[length / 2];

            for(int i = 0; i < length; i += 2) {
                int ch1 = decode(string.charAt(i)) << 4;
                int ch2 = decode(string.charAt(i + 1));
                bytes[i / 2] = (byte)(ch1 + ch2);
            }

            return fromBytes(bytes, Usage.SAFE_TO_REUSE_BYTES);
        } else {
            throw new IllegalArgumentException(String.format("Invalid hash code length: %d characters", length));
        }
    }

    private static int decode(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - 48;
        } else if (ch >= 'a' && ch <= 'f') {
            return ch - 97 + 10;
        } else if (ch >= 'A' && ch <= 'F') {
            return ch - 65 + 10;
        } else {
            throw new IllegalArgumentException("Illegal hexadecimal character: " + ch);
        }
    }

    public abstract int length();

    public abstract byte[] toByteArray();

    public abstract int hashCode();

    public abstract boolean equals(@Nullable Object var1);

    public String toString() {
        StringBuilder sb = toStringBuilder(2 * this.length(), this.bytes());
        return sb.toString();
    }

    public String toZeroPaddedString(int length) {
        StringBuilder sb = toStringBuilder(length, this.bytes());

        while(sb.length() < length) {
            sb.insert(0, '0');
        }

        return sb.toString();
    }

    private static StringBuilder toStringBuilder(int capacity, byte[] bytes) {
        StringBuilder sb = new StringBuilder(capacity);
        byte[] var3 = bytes;
        int var4 = bytes.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            byte b = var3[var5];
            sb.append(HEX_DIGITS[b >> 4 & 15]).append(HEX_DIGITS[b & 15]);
        }

        return sb;
    }

    public String toCompactString() {
        return (new BigInteger(1, this.bytes())).toString(36);
    }

    abstract void appendToHasher(PrimitiveHasher var1);

    abstract byte[] bytes();

    private static int compareLong(long a, long b) {
        return a < b ? -1 : (a == b ? 0 : 1);
    }

    private static int compareBytes(byte[] a, byte[] b) {
        int len1 = a.length;
        int len2 = b.length;
        int length = Math.min(len1, len2);

        for(int idx = 0; idx < length; ++idx) {
            int result = a[idx] - b[idx];
            if (result != 0) {
                return result;
            }
        }

        return len1 - len2;
    }

    private static long bytesToLong(byte[] bytes, int offset) {
        return (long)bytes[offset] & 255L | ((long)bytes[offset + 1] & 255L) << 8 | ((long)bytes[offset + 2] & 255L) << 16 | ((long)bytes[offset + 3] & 255L) << 24 | ((long)bytes[offset + 4] & 255L) << 32 | ((long)bytes[offset + 5] & 255L) << 40 | ((long)bytes[offset + 6] & 255L) << 48 | ((long)bytes[offset + 7] & 255L) << 56;
    }

    private static void longToBytes(long value, byte[] bytes, int offset) {
        bytes[offset] = (byte)((int)(value & 255L));
        bytes[offset + 1] = (byte)((int)(value >>> 8 & 255L));
        bytes[offset + 2] = (byte)((int)(value >>> 16 & 255L));
        bytes[offset + 3] = (byte)((int)(value >>> 24 & 255L));
        bytes[offset + 4] = (byte)((int)(value >>> 32 & 255L));
        bytes[offset + 5] = (byte)((int)(value >>> 40 & 255L));
        bytes[offset + 6] = (byte)((int)(value >>> 48 & 255L));
        bytes[offset + 7] = (byte)((int)(value >>> 56 & 255L));
    }

    private static class ByteArrayBackedHashCode extends HashCode {
        private final byte[] bytes;

        public ByteArrayBackedHashCode(byte[] bytes) {
            super();
            this.bytes = bytes;
        }

        public int length() {
            return this.bytes.length;
        }

        byte[] bytes() {
            return this.bytes;
        }

        public byte[] toByteArray() {
            return (byte[])this.bytes.clone();
        }

        void appendToHasher(PrimitiveHasher hasher) {
            hasher.putBytes(this.bytes);
        }

        public int hashCode() {
            return this.bytes[0] & 255 | (this.bytes[1] & 255) << 8 | (this.bytes[2] & 255) << 16 | (this.bytes[3] & 255) << 24;
        }

        public boolean equals(@Nullable Object obj) {
            if (obj == this) {
                return true;
            } else if (obj != null && obj.getClass() == ByteArrayBackedHashCode.class) {
                byte[] a = this.bytes;
                byte[] b = ((ByteArrayBackedHashCode)obj).bytes;
                int length = a.length;
                if (b.length != length) {
                    return false;
                } else {
                    for(int i = 0; i < length; ++i) {
                        if (a[i] != b[i]) {
                            return false;
                        }
                    }

                    return true;
                }
            } else {
                return false;
            }
        }

        public int compareTo(@Nonnull HashCode o) {
            return compareBytes(this.bytes, o.bytes());
        }
    }

    static class HashCode128 extends HashCode {
        private final long bits1;
        private final long bits2;

        public HashCode128(long bits1, long bits2) {
            super();
            this.bits1 = bits1;
            this.bits2 = bits2;
        }

        public int length() {
            return 16;
        }

        byte[] bytes() {
            return this.toByteArray();
        }

        public byte[] toByteArray() {
            byte[] bytes = new byte[16];
            longToBytes(this.bits1, bytes, 0);
            longToBytes(this.bits2, bytes, 8);
            return bytes;
        }

        void appendToHasher(PrimitiveHasher hasher) {
            hasher.putLong(this.bits1);
            hasher.putLong(this.bits2);
        }

        public int hashCode() {
            return (int)this.bits1;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && o.getClass() == HashCode128.class) {
                HashCode128 other = (HashCode128)o;
                return this.bits1 == other.bits1 && this.bits2 == other.bits2;
            } else {
                return false;
            }
        }

        public int compareTo(HashCode o) {
            if (o.getClass() != HashCode128.class) {
                return compareBytes(this.bytes(), o.bytes());
            } else {
                HashCode128 other = (HashCode128)o;
                int result = compareLong(this.bits1, other.bits1);
                if (result == 0) {
                    result = compareLong(this.bits2, other.bits2);
                }

                return result;
            }
        }
    }

    static enum Usage {
        CLONE_BYTES_IF_NECESSARY,
        SAFE_TO_REUSE_BYTES;

        private Usage() {
        }
    }
}