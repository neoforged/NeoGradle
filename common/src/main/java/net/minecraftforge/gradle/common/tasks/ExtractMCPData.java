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

import net.minecraftforge.gradle.common.config.McpConfigConfigurationSpecV2;
import net.minecraftforge.gradle.common.extensions.RemappingExtensions;
import net.minecraftforge.srgutils.IMappingFile;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@CacheableTask
public abstract class ExtractMCPData extends DownloadingTask {
    private boolean allowEmpty = false;

    public ExtractMCPData() {
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(s -> s.file("output.srg")));
        getKey().convention("mappings");
    }

    @TaskAction
    public void run() throws IOException {
        McpConfigConfigurationSpecV2 cfg = McpConfigConfigurationSpecV2.getFromArchive(getConfig().get().getAsFile());

        String key = getKey().get();
        File output = getOutput().get().getAsFile();
        try (ZipFile zip = new ZipFile(getConfig().get().getAsFile())) {
            String path = cfg.getData(key.split("/"));
            if (path == null && "statics".equals(key))
                path = "config/static_methods.txt";

            if (path == null) {
                error("Could not find data entry for '" + key + "'");
                return;
            }

            ZipEntry entry = zip.getEntry(path);
            if (entry == null) {
                error("Invalid config zip, Missing path '" + path + "'");
                return;
            }

            try (OutputStream out = new FileOutputStream(output)) {
                IOUtils.copy(zip.getInputStream(entry), out);
            }
        }

        if (cfg.isOfficial() && output.exists() && "mappings".equals(key)) {
            IMappingFile obfToSrg = IMappingFile.load(output);
            final RemappingExtensions remappingExtensions = getProject().getExtensions().getByType(RemappingExtensions.class);
            final Provider<IMappingFile> remappedFile = remappingExtensions.remapSrgClasses(cfg, obfToSrg);
            if (remappedFile.isPresent()) {
                remappedFile.get().write(output.toPath(), IMappingFile.Format.TSRG2, false);
            }
        }
    }

    private void error(String message) throws IOException {
        if (!isAllowEmpty())
            throw new IllegalStateException(message);

        File outputFile = getOutput().get().getAsFile();
        if (outputFile.exists())
            outputFile.delete();

        outputFile.createNewFile();
    }

    @Input
    public abstract Property<String> getKey();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getConfig();

    @Input
    public boolean isAllowEmpty() {
        return allowEmpty;
    }

    public void setAllowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
    }

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
