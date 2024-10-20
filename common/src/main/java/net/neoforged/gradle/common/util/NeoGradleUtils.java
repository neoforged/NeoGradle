package net.neoforged.gradle.common.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class NeoGradleUtils {
    public static @NotNull String getNeogradleVersion() {

        final String neogradleVersion;
        try(final InputStream stream = Objects.requireNonNull(NeoGradleUtils.class.getClassLoader().getResource("version.neogradle")).openStream()) {
            neogradleVersion = new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read NeoGradle version", e);
        }
        return neogradleVersion;
    }
}
