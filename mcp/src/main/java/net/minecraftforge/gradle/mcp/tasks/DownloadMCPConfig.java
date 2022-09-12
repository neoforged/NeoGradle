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

package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.tasks.DownloadingTask;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@CacheableTask
public abstract class DownloadMCPConfig extends DownloadingTask {


    public DownloadMCPConfig() {
        getOutput().convention(
                getConfig().flatMap( config ->
                        getProject().getLayout().getBuildDirectory().dir("mcp_config").map(directory -> directory.file(Path.of(config).getFileName().toString()))
                )
        );
    }

    @TaskAction
    public void downloadMCPConfig() throws IOException {
        Provider<File> configFile = getConfigFile();
        File output = getOutput().get().getAsFile();

        if (output.exists()) {
            if (FileUtils.contentEquals(configFile.get(), output)) {
                // NO-OP: The contents of both files are the same, we're up to date
                setDidWork(false);
                return;
            } else {
                output.delete();
            }
        }
        FileUtils.copyFile(configFile.get(), output);
        setDidWork(true);
    }

    @Input
    public abstract Property<String> getConfig();

    @Internal
    public Provider<File> getConfigFile() {
        return downloadConfigFile(getConfig().get());
    }

    @OutputFile
    public abstract RegularFileProperty getOutput();

    private Provider<File> downloadConfigFile(String config) {
        return getDownloader().flatMap(d -> d.manual(config, false));
    }
}
