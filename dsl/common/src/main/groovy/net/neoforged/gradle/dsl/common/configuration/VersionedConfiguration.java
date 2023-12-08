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

package net.neoforged.gradle.dsl.common.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class VersionedConfiguration {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int spec = 1;

    public static int getSpec(InputStream stream) throws IOException {
        return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), VersionedConfiguration.class).spec;
    }
    public static int getSpec(byte[] data) throws IOException {
        return getSpec(new ByteArrayInputStream(data));
    }

    public final int getSpec() {
        return spec;
    }
}
