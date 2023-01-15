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

package net.minecraftforge.gradle.common.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.gradle.base.util.ManifestJson;
import net.minecraftforge.gradle.base.util.UrlConstants;
import net.minecraftforge.gradle.dsl.common.tasks.ForgeGradleBase;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

@CacheableTask
public abstract class DownloadMCMeta extends ForgeGradleBase {
    // TODO: convert this into a property?
    private static final Gson GSON = new GsonBuilder().create();

    public DownloadMCMeta() {
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(s -> s.file("version.json")));
    }

    @TaskAction
    public void downloadMCMeta() throws IOException {
        try (InputStream manifestStream = new URL(UrlConstants.MOJANG_MANIFEST).openStream()) {
            URL url = GSON.fromJson(new InputStreamReader(manifestStream), ManifestJson.class).getUrl(getMinecraftVersion().get());
            if (url != null) {
                FileUtils.copyURLToFile(url, getOutput().get().getAsFile());
            } else {
                throw new RuntimeException("Missing version from manifest: " + getMinecraftVersion().get());
            }
        }
    }

    @Input
    public abstract Property<String> getMinecraftVersion();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
