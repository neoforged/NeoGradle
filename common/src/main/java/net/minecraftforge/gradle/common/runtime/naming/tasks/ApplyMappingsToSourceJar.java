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

package net.minecraftforge.gradle.common.runtime.naming.tasks;

import net.minecraftforge.gradle.common.runtime.naming.renamer.ISourceRenamer;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.common.util.Utils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


@CacheableTask
public abstract class ApplyMappingsToSourceJar extends net.minecraftforge.gradle.common.runtime.tasks.Runtime implements Runtime {

    public ApplyMappingsToSourceJar() {
        getRemapJavadocs().convention(false);
    }

    @TaskAction
    public void apply() throws Exception {
        final ISourceRenamer renamer = getSourceRenamer().get();
        try (ZipFile zin = new ZipFile(getInput().get().getAsFile())) {
            try (FileOutputStream fos = new FileOutputStream(getOutput().get().getAsFile());
                 ZipOutputStream out = new ZipOutputStream(fos)) {

                final Enumeration<? extends ZipEntry> entries = zin.entries();
                while(entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    out.putNextEntry(Utils.getStableEntry(entry.getName()));
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
    @PathSensitive(PathSensitivity.RELATIVE)
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
