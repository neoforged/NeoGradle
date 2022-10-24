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

package net.minecraftforge.gradle.common.runtime.naming.renamer;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.tasks.Nested;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OfficialSourceRenamer extends RegexBasedSourceRenamer {

    private final Map<String, String> names;
    private final Map<String, String> docs;

    private OfficialSourceRenamer(Map<String, String> names, Map<String, String> docs) {
        this.names = names;
        this.docs = docs;
    }

    public static OfficialSourceRenamer from(File clientFile, final File serverFile, final File tsrgFile) throws IOException {
        Map<String, String> names = new ConcurrentHashMap<>();

        IMappingFile pg_client = IMappingFile.load(clientFile);
        IMappingFile pg_server = IMappingFile.load(serverFile);
        IMappingFile srg = IMappingFile.load(tsrgFile);

        Map<String, String> cfields = new ConcurrentHashMap<>();
        Map<String, String> sfields = new ConcurrentHashMap<>();
        Map<String, String> cmethods = new ConcurrentHashMap<>();
        Map<String, String> smethods = new ConcurrentHashMap<>();

        pg_client.getClasses().parallelStream().forEach(cls -> processClass(srg, cfields, cmethods, cls));
        pg_server.getClasses().parallelStream().forEach(cls -> processClass(srg, sfields, smethods, cls));

        cfields.keySet().parallelStream().forEach(name -> processName(names, cfields, sfields, name));
        cmethods.keySet().parallelStream().forEach(name -> processName(names, cmethods, smethods, name));

        return new OfficialSourceRenamer(names, Collections.emptyMap());
    }

    private static void processName(Map<String, String> names, Map<String, String> clientData, Map<String, String> serverData, String name) {
        String cname = clientData.get(name);
        String sname = serverData.get(name);
        names.put(name, cname);
        if (cname.equals(sname)) {
            serverData.remove(name);
        }
    }

    private static void processClass(IMappingFile tsrgMappingData, Map<String, String> fields, Map<String, String> methods, IMappingFile.IClass classData) {
        IMappingFile.IClass obf = tsrgMappingData.getClass(classData.getMapped());
        if (obf == null) // Class exists in official source, but doesn't make it past obfusication so it's not in our mappings.
            return;
        for (IMappingFile.IField fld : classData.getFields()) {
            String name = obf.remapField(fld.getMapped());
            if (name.startsWith("field_") || name.startsWith("f_"))
                fields.put(name, fld.getOriginal());
        }
        for (IMappingFile.IMethod mtd : classData.getMethods()) {
            String name = obf.remapMethod(mtd.getMapped(), mtd.getMappedDescriptor());
            if (name.startsWith("func_") || name.startsWith("m_"))
                methods.put(name, mtd.getOriginal());
        }
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
