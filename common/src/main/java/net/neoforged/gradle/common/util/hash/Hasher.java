package net.neoforged.gradle.common.util.hash;


import org.jetbrains.annotations.Nullable;

public interface Hasher {
    void putBytes(byte[] var1);

    void putBytes(byte[] var1, int var2, int var3);

    void putByte(byte var1);

    void putInt(int var1);

    void putLong(long var1);

    void putDouble(double var1);

    void putBoolean(boolean var1);

    void putString(CharSequence var1);

    void putHash(HashCode var1);

    void put(Hashable var1);

    void putNull();

    void put(@Nullable Object value);

    void put(@Nullable Object value, boolean throwOnUnknownType);

    HashCode hash();
}