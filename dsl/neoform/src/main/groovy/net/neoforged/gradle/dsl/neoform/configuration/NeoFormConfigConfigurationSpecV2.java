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

package net.neoforged.gradle.dsl.neoform.configuration;

import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.Input;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

public class NeoFormConfigConfigurationSpecV2 extends NeoFormConfigConfigurationSpecV1 {

    public static NeoFormConfigConfigurationSpecV2 get(File file) {
        try (final InputStream stream = new FileInputStream(file)) {
            return get(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static NeoFormConfigConfigurationSpecV2 get(InputStream stream) {
        try(final InputStreamReader reader = new InputStreamReader(stream)) {
            return GSON.fromJson(reader, NeoFormConfigConfigurationSpecV2.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
    }
    public static NeoFormConfigConfigurationSpecV2 get(byte[] data) {
        return get(new ByteArrayInputStream(data));
    }

    public static NeoFormConfigConfigurationSpecV2 getFromArchive(File path) throws IOException {
        try (ZipFile zip = new ZipFile(path)) {
            ZipEntry entry = zip.getEntry("config.json");
            if (entry == null)
                throw new IllegalStateException("Could not find 'config.json' in " + path.getAbsolutePath());

            byte[] data = IOUtils.toByteArray(zip.getInputStream(entry));
            int spec = getSpec(data);
            if (spec == 2 || spec == 3)
                return NeoFormConfigConfigurationSpecV2.get(data);
            if (spec == 1)
                return new NeoFormConfigConfigurationSpecV2(NeoFormConfigConfigurationSpecV1.get(data));

            throw new IllegalStateException("Invalid NeoForm Config: " + path.getAbsolutePath() + " Unknown spec: " + spec);
        }
    }

    private boolean official = false;
    private int java_target = 8;
    @Nullable
    private String encoding = "UTF-8";

    @Input
    public boolean isOfficial() {
        return this.official;
    }

    @Input
    public int getJavaTarget() {
        return this.java_target;
    }

    @Input
    public String getEncoding() {
        return this.encoding == null ? "UTF-8" : this.encoding;
    }

    public NeoFormConfigConfigurationSpecV2(NeoFormConfigConfigurationSpecV1 old) {
        this.version = old.version;
        this.data = old.data;
        this.steps = old.steps;
        this.functions = old.functions;
        this.libraries = old.libraries;
    }
}
