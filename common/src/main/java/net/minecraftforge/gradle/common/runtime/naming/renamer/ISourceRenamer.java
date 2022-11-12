package net.minecraftforge.gradle.common.runtime.naming.renamer;

import java.io.IOException;
import java.io.InputStream;

public interface ISourceRenamer {
    byte[] rename(byte[] classFile, boolean javadocs, boolean lambdas) throws IOException;
}
