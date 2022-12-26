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

public class IMappingFileSourceRenamer extends RegexBasedSourceRenamer {

    private final Map<String, String> names;
    private final Map<String, String> docs;

    private IMappingFileSourceRenamer(Map<String, String> names, Map<String, String> docs) {
        this.names = names;
        this.docs = docs;
    }

    public static IMappingFileSourceRenamer from(File clientFile, final File serverFile) throws IOException {
        IMappingFile pg_client = IMappingFile.load(clientFile);
        IMappingFile pg_server = IMappingFile.load(serverFile);

        return from(pg_client, pg_server);
    }

    public static IMappingFileSourceRenamer from(IMappingFile pg_client, final IMappingFile pg_server) throws IOException {
        Map<String, String> names = new ConcurrentHashMap<>();

        Map<String, String> cfields = new ConcurrentHashMap<>();
        Map<String, String> sfields = new ConcurrentHashMap<>();
        Map<String, String> cmethods = new ConcurrentHashMap<>();
        Map<String, String> smethods = new ConcurrentHashMap<>();

        pg_client.getClasses().parallelStream().forEach(cls -> processClass(cfields, cmethods, cls));
        pg_server.getClasses().parallelStream().forEach(cls -> processClass(sfields, smethods, cls));

        cfields.keySet().parallelStream().forEach(name -> processName(names, cfields, sfields, name));
        cmethods.keySet().parallelStream().forEach(name -> processName(names, cmethods, smethods, name));

        return new IMappingFileSourceRenamer(names, Collections.emptyMap());
    }

    private static void processName(Map<String, String> names, Map<String, String> clientData, Map<String, String> serverData, String name) {
        String cname = clientData.get(name);
        String sname = serverData.get(name);
        names.put(name, cname);
        if (cname.equals(sname)) {
            serverData.remove(name);
        }
    }

    private static void processClass(Map<String, String> fields, Map<String, String> methods, IMappingFile.IClass classData) {
        for (IMappingFile.IField fld : classData.getFields()) {
            fields.put(fld.getMapped(), fld.getOriginal());
        }
        for (IMappingFile.IMethod mtd : classData.getMethods()) {
            methods.put(mtd.getMapped(), mtd.getOriginal());
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
