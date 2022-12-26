package net.minecraftforge.gradle.common.runtime.naming.renamer;

import java.io.IOException;

/**
 * Defines a renamer which renames a source file or parts of it.
 * Generally source renamers only support renaming source files which have unique, type, field and method names.
 */
public interface ISourceRenamer {
    /**
     * Renames an entire source file, and optionally its javadocs or lambdas.
     *
     * @param classFile The classes source file contents to rename.
     * @param javadocs Whether to rename javadocs.
     * @param lambdas Whether to rename lambdas.
     * @return The renamed source file.
     * @throws IOException If an error occurs while renaming.
     */
    byte[] rename(byte[] classFile, boolean javadocs, boolean lambdas) throws IOException;
}
