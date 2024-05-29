package net.neoforged.gradle.common.util.hash;

public interface PrimitiveHasher {
    void putBytes(byte[] var1);

    void putBytes(byte[] var1, int var2, int var3);

    void putByte(byte var1);

    void putInt(int var1);

    void putLong(long var1);

    void putDouble(double var1);

    void putBoolean(boolean var1);

    void putString(CharSequence var1);

    void putHash(HashCode var1);

    HashCode hash();
}