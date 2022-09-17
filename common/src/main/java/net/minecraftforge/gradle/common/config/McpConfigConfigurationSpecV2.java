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

package net.minecraftforge.gradle.common.config;

import net.minecraftforge.gradle.common.util.Utils;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

public class McpConfigConfigurationSpecV2 extends McpConfigConfigurationSpecV1 {

    public static McpConfigConfigurationSpecV2 get(File file) {
        try (final InputStream stream = new FileInputStream(file)) {
            return get(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static McpConfigConfigurationSpecV2 get(InputStream stream) {
        return Utils.fromJson(stream, McpConfigConfigurationSpecV2.class);
    }
    public static McpConfigConfigurationSpecV2 get(byte[] data) {
        return get(new ByteArrayInputStream(data));
    }

    public static McpConfigConfigurationSpecV2 getFromArchive(File path) throws IOException {
        try (ZipFile zip = new ZipFile(path)) {
            ZipEntry entry = zip.getEntry("config.json");
            if (entry == null)
                throw new IllegalStateException("Could not find 'config.json' in " + path.getAbsolutePath());

            byte[] data = IOUtils.toByteArray(zip.getInputStream(entry));
            int spec = VersionedConfiguration.getSpec(data);
            if (spec == 2 || spec == 3)
                return McpConfigConfigurationSpecV2.get(data);
            if (spec == 1)
                return new McpConfigConfigurationSpecV2(McpConfigConfigurationSpecV1.get(data));

            throw new IllegalStateException("Invalid MCP Config: " + path.getAbsolutePath() + " Unknown spec: " + spec);
        }
    }

    private boolean official = false;
    private int java_target = 8;
    @Nullable
    private String encoding = "UTF-8";

    public boolean isOfficial() {
        return this.official;
    }

    public int getJavaTarget() {
        return this.java_target;
    }

    public String getEncoding() {
        return this.encoding == null ? "UTF-8" : this.encoding;
    }

    public McpConfigConfigurationSpecV2(McpConfigConfigurationSpecV1 old) {
        this.version = old.version;
        this.data = old.data;
        this.steps = old.steps;
        this.functions = old.functions;
        this.libraries = old.libraries;
    }
}
