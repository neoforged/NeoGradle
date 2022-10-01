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

import net.minecraftforge.gradle.common.util.Utils;

import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class ApplyMappingsToSourceJarTask extends ForgeGradleBaseTask implements ITaskWithOutput {

    public ApplyMappingsToSourceJarTask() {
        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(s -> s.file("output.zip")));
        getRemapJavadocs().convention(false);
    }

    @TaskAction
    public void apply() throws IOException {
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
                        final byte[] toRemap = inputStream.readAllBytes();
                        inputStream.close();
                        out.write(createRemappedOutputOfSourceFile(toRemap, getRemapJavadocs().getOrElse(false)));
                    }
                    out.closeEntry();
                }
            }
        }
    }

    protected abstract byte[] createRemappedOutputOfSourceFile(final byte[] inputStream, final boolean shouldRemapJavadocs) throws IOException;

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<Boolean> getRemapJavadocs();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
