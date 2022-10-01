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

package net.minecraftforge.gradle.mcp.naming.renamer;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class McpSourceRenamer extends RegexBasedSourceRenamer {

    public static McpSourceRenamer from(File data) throws IOException {
        Map<String, String> names = new HashMap<>();
        Map<String, String> docs = new HashMap<>();
        try (ZipFile zip = new ZipFile(data)) {
            List<ZipEntry> entries = zip.stream().filter(e -> e.getName().endsWith(".csv")).collect(Collectors.toList());
            for (ZipEntry entry : entries) {
                try (NamedCsvReader reader = NamedCsvReader.builder().build(new InputStreamReader(zip.getInputStream(entry)))) {
                    String obf = reader.getHeader().contains("searge") ? "searge" : "param";
                    boolean hasDesc = reader.getHeader().contains("desc");
                    reader.forEach(row -> {
                        String searge = row.getField(obf);
                        names.put(searge, row.getField("name"));
                        if (hasDesc) {
                            String desc = row.getField("desc");
                            if (!desc.isEmpty())
                                docs.put(searge, desc);
                        }
                    });
                }
            }
        }

        return new McpSourceRenamer(names, docs);
    }

    private final Map<String, String> names;

    private final Map<String, String> docs;

    private McpSourceRenamer(Map<String, String> names, Map<String, String> docs) {
        this.names = names;
        this.docs = docs;
    }

    @Override
    @Nested
    public Map<String, String> getNames() {
        return names;
    }

    @Override
    @Nested
    public Map<String, String> getDocs() {
        return docs;
    }
}
