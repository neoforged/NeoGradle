package net.neoforged.gradle.common.caching;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class HashCodeBuilder {
    private final FileHashing fileHashing;
    private final StringBuilder sourceMaterial = new StringBuilder();
    private final MessageDigest digest;

    public HashCodeBuilder(FileHashing fileHashing) {
        this.fileHashing = fileHashing;
        // Relativize any path to gradle home or project root,
        // which will work for anything but maven local dependencies
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Standard algorithm MD5 is missing.", e);
        }
    }

    public void add(Path path) {
        byte[] fileHash = fileHashing.getMd5Hash(path);
        String fileHashHex = Hex.encodeHexString(fileHash);
        add(fileHash, "HASHED-CONTENT(" + path + ") = " + fileHashHex);
    }

    public void add(String data) {
        add(data.getBytes(StandardCharsets.UTF_8), "STRING(" + data + ")");
    }

    public void add(byte[] data, String sourceMaterial) {
        digest.update(data);
        this.sourceMaterial.append(sourceMaterial).append('\n');
    }

    public String buildHashCode() {
        return Hex.encodeHexString(digest.digest());
    }

    public String buildSourceMaterial() {
        return sourceMaterial.toString();
    }
}
