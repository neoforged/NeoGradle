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

package net.neoforged.gradle.common.util;

import com.google.gson.*;

import java.io.*;

/**
 * Utility class for serializing and deserializing objects to and from JSON.
 */
public class SerializationUtils {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Loads an object from a JSON file.
     *
     * @param file The file to load from.
     * @param assetIndexClass The class of the object to load.
     * @return The loaded object.
     * @param <T> The type of the object to load.
     */
    public static <T> T fromJson(File file, Class<T> assetIndexClass) {
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            return GSON.fromJson(new InputStreamReader(fileInputStream), assetIndexClass);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find the file!", e);
        }
        finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new RuntimeException("Failed to close file stream!", e);
                }
            }
        }
    }
}
