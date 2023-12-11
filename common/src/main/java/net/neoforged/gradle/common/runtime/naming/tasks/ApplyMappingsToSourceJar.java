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

package net.neoforged.gradle.common.runtime.naming.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CacheKey;
import net.neoforged.gradle.common.caching.SharedCacheService;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.common.runtime.naming.renamer.ISourceRenamer;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


@CacheableTask
public abstract class ApplyMappingsToSourceJar extends DefaultRuntime {

    public ApplyMappingsToSourceJar() {
        getRemapJavadocs().convention(false);
    }

    @TaskAction
    public void apply() throws Exception {
        File inputFile = getInput().get().getAsFile();
        File outputFile = getOutput().get().getAsFile();
        boolean remapJavadocs = getRemapJavadocs().getOrElse(false);
        boolean remapLambdas = getRemapLambdas().getOrElse(true);
        final ISourceRenamer renamer = getSourceRenamer().get();

        // TODO: This is not safe since we do not account for code-changes to ISourceRenamer
        SharedCacheService cacheService = getSharedCacheService().get();
        CacheKey cacheKey = cacheService.cacheKeyBuilder(getProject())
                .cacheDomain(getStepName().get())
                .arguments(Lists.newArrayList("javadocs:" + remapJavadocs, "lambdas:" + remapLambdas))
                .inputFiles(getInputs().getFiles().getFiles())
                .build();

        boolean usedCache = cacheService.cacheOutput(getProject(), cacheKey, outputFile.toPath(), () -> {
            try (ZipInputStream zin = new ZipInputStream(new FileInputStream(inputFile));
                 FileOutputStream fos = new FileOutputStream(outputFile);
                 ZipOutputStream zout = new ZipOutputStream(fos)) {

                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    zout.putNextEntry(FileUtils.getStableEntry(entry.getName()));
                    if (!entry.getName().endsWith(".java")) {
                        IOUtils.copyLarge(zin, zout);
                    } else {
                        // If the uncompressed size is known, use it to read the data more efficiently
                        byte[] toRemap = entry.getSize() >= 0 ? IOUtils.toByteArray(zin, entry.getSize()) : IOUtils.toByteArray(zin);
                        zout.write(renamer.rename(toRemap, remapJavadocs, remapLambdas));
                    }
                    zout.closeEntry();
                }
            }

            getLogger().debug("Applying mappings to source jar complete");
        });

        if (usedCache) {
            setDidWork(false);
        }
    }

    @ServiceReference(CommonProjectPlugin.NEOFORM_CACHE_SERVICE)
    protected abstract Property<SharedCacheService> getSharedCacheService();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<Boolean> getRemapJavadocs();

    @Input
    public abstract Property<Boolean> getRemapLambdas();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Internal
    public abstract Property<ISourceRenamer> getSourceRenamer();
}
