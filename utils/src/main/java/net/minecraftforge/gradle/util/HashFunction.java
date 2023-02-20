/*
 * ForgeGradle
 * Copyright (C) 2018 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.minecraftforge.gradle.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;

/**
 * Different hash functions.
 *
 * <p>All of these hashing functions are standardized, and is required to be implemented by all JREs. However, {@link
 * MessageDigest#getInstance(String)} declares as throwing the checked exception of {@link NoSuchAlgorithmException}.</p>
 *
 * <p>This class offers a cleaner method to retrieve an instance of these hashing functions, without having to wrap in a
 * {@code try}-{@code catch} block.</p>
 */
public enum HashFunction {
    MD5("md5", 32),
    SHA1("SHA-1", 40),
    SHA256("SHA-256", 64),
    SHA512("SHA-512", 128);

    private final String algo;
    private final String pad;

    HashFunction(String algo, int length) {
        this.algo = algo;
        this.pad = String.format(Locale.ROOT, "%0" + length + "d", 0);
    }

    /**
     * The file extension of this hash function.
     *
     * @return The file extension.
     */
    public String getExtension() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Retrieves the internal {@link MessageDigest} instance of this hash function.
     * Throws a {@link RuntimeException} if the algorithm is not supported by the JRE.
     *
     * @return The {@link MessageDigest} instance.
     */
    public MessageDigest get() {
        try {
            return MessageDigest.getInstance(algo);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Never happens
        }
    }

    /**
     * Hashes the given file.
     *
     * @param file The file to hash.
     * @return The hash of the file.
     * @throws IOException If an I/O error occurs.
     */
    public String hash(File file) throws IOException {
        return hash(file.toPath());
    }

    /**
     * Hashes the given file.
     *
     * @param file The file to hash.
     * @return The hash of the file.
     * @throws IOException If an I/O error occurs.
     */
    public String hash(Path file) throws IOException {
        return hash(Files.readAllBytes(file));
    }

    /**
     * Hashes the given files.
     *
     * @param files The files to hash.
     * @return The hash of the files.
     * @throws IOException If an I/O error occurs.
     */
    public String hash(Iterable<File> files) throws IOException {
        MessageDigest hash = get();

        for (File file : files) {
            if (!file.exists())
                continue;
            hash.update(Files.readAllBytes(file.toPath()));
        }
        return pad(new BigInteger(1, hash.digest()).toString(16));
    }

    /**
     * Hashes the given string.
     *
     * @param data The string to hash.
     * @return The hash of the string.
     */
    public String hash(@Nullable String data) {
        return hash(data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hashes the given stream.
     *
     * @param stream The stream to hash.
     * @return The hash of the stream.
     * @throws IOException If an I/O error occurs.
     */
    public String hash(InputStream stream) throws IOException {
        return hash(IOUtils.toByteArray(stream));
    }

    /**
     * Hashes the given bytes.
     *
     * @param data The bytes to hash.
     * @return The hash of the bytes.
     */
    public String hash(byte[] data) {
        return pad(new BigInteger(1, get().digest(data)).toString(16));
    }

    /**
     * Pads the hash with leading zeroes, so that it matches the length of the hash function.
     *
     * @param hash The hash to pad.
     * @return The padded hash.
     */
    public String pad(String hash) {
        return (pad + hash).substring(hash.length());
    }
}
