package net.neoforged.gradle.common.util.hash;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface HashFunction {
    String getAlgorithm();

    PrimitiveHasher newPrimitiveHasher();

    Hasher newHasher();

    HashCode hashBytes(byte[] var1);

    HashCode hashString(CharSequence var1);

    HashCode hashStream(InputStream var1) throws IOException;

    HashCode hashFile(File var1) throws IOException;

    int getHexDigits();
}
