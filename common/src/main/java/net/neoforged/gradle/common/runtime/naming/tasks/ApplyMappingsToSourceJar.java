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

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.common.runtime.naming.renamer.ISourceRenamer;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


@CacheableTask
public abstract class ApplyMappingsToSourceJar extends DefaultRuntime {

    public ApplyMappingsToSourceJar() {
        getRemapJavadocs().convention(false);
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public final void execute() throws Throwable {
        getCacheService().get()
                        .cached(
                                this,
                                ICacheableJob.Default.file(getOutput(), this::apply)
                        ).execute();
    }

    protected final void apply() throws Exception {
        final ISourceRenamer renamer = getSourceRenamer().get();
        try (ZipFile zin = new ZipFile(getInput().get().getAsFile())) {
            try (FileOutputStream fos = new FileOutputStream(getOutput().get().getAsFile());
                 ZipOutputStream out = new ZipOutputStream(fos)) {

                final Enumeration<? extends ZipEntry> entries = zin.entries();
                while(entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    out.putNextEntry(FileUtils.getStableEntry(entry.getName()));
                    if (!entry.getName().endsWith(".java")) {
                        IOUtils.copy(zin.getInputStream(entry), out);
                    } else {
                        final InputStream inputStream = zin.getInputStream(entry);
                        final byte[] toRemap = IOUtils.toByteArray(inputStream);
                        inputStream.close();
                        out.write(renamer.rename(toRemap, getRemapJavadocs().getOrElse(false), getRemapLambdas().getOrElse(true)));
                    }
                    out.closeEntry();
                }
            }
        }

        getLogger().debug("Applying mappings to source jar complete");
    }

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
