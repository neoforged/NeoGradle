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

package net.minecraftforge.gradle.common.util;

import com.google.gson.*;
import groovy.lang.Closure;
import net.minecraftforge.gradle.base.util.HashFunction;
import net.minecraftforge.gradle.common.util.VersionJson.Download;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static final boolean ENABLE_FILTER_REPOS = Boolean.parseBoolean(System.getProperty("net.minecraftforge.gradle.filter_repos", "true"));
    static final int CACHE_TIMEOUT = 1000 * 60 * 60; //1 hour, Timeout used for version_manifest.json so we dont ping their server every request.
                                                          //manifest doesn't include sha1's so we use this for the per-version json as well

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void extractFile(ZipFile zip, String name, File output) throws IOException {
        extractFile(zip, zip.getEntry(name), output);
    }

    public static void extractFile(ZipFile zip, ZipEntry entry, File output) throws IOException {
        File parent = output.getParentFile();
        if (!parent.exists())
            parent.mkdirs();

        try (InputStream stream = zip.getInputStream(entry)) {
            Files.copy(stream, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void extractDirectory(Function<String, File> fileLocator, ZipFile zip, String directory) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            if (e.isDirectory()) continue;
            if (!e.getName().startsWith(directory)) continue;
            extractFile(zip, e, fileLocator.apply(e.getName()));
        }
    }

    public static Set<String> copyZipEntries(ZipOutputStream zout, ZipInputStream zin, Predicate<String> filter) throws IOException {
        Set<String> added = new HashSet<>();
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            if (!filter.test(entry.getName())) continue;
            ZipEntry _new = new ZipEntry(entry.getName());
            _new.setTime(0); //SHOULD be the same time as the main entry, but NOOOO _new.setTime(entry.getTime()) throws DateTimeException, so you get 0, screw you!
            zout.putNextEntry(_new);
            IOUtils.copy(zin, zout);
            added.add(entry.getName());
        }
        return added;
    }

    public static byte[] base64DecodeStringList(List<String> strings) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (String string : strings) {
            bos.write(Base64.getDecoder().decode(string));
        }
        return bos.toByteArray();
    }

    public static File delete(File file) {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (file.exists()) file.delete();
        return file;
    }

    public static File createEmpty(File file) throws IOException {
        delete(file);
        file.createNewFile();
        return file;
    }

    public static Path getCacheBase(Project project) {
        File gradleUserHomeDir = project.getGradle().getGradleUserHomeDir();
        return Paths.get(gradleUserHomeDir.getPath(), "caches", "forge_gradle");
    }

    public static File getCache(Project project, String... tail) {
        return Paths.get(getCacheBase(project).toString(), tail).toFile();
    }

    public static void extractZip(File source, File target, boolean overwrite) throws IOException {
        extractZip(source, target, overwrite, false);
    }

    public static void extractZip(File source, File target, boolean overwrite, boolean deleteExtras) throws IOException {
        extractZip(source, target, overwrite, deleteExtras, name -> name);
    }

    public static void extractZip(File source, File target, boolean overwrite, boolean deleteExtras, Function<String, String> renamer) throws IOException {
        Set<File> extra = deleteExtras ? Files.walk(target.toPath()).filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toSet()) : new HashSet<>();

        try (ZipFile zip = new ZipFile(source)) {
            Enumeration<? extends ZipEntry> enu = zip.entries();
            while (enu.hasMoreElements()) {
                ZipEntry e = enu.nextElement();
                if (e.isDirectory()) continue;

                String name = renamer.apply(e.getName());
                if (name == null) continue;
                File out = new File(target, name);

                File parent = out.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                extra.remove(out);

                if (out.exists()) {
                    if (!overwrite)
                        continue;

                    //Reading is fast, and prevents Disc wear, so check if it's equals before writing.
                    try (FileInputStream fis = new FileInputStream(out)){
                        if (IOUtils.contentEquals(zip.getInputStream(e), fis))
                            continue;
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(out)) {
                    IOUtils.copy(zip.getInputStream(e), fos);
                }
            }
        }

        if (deleteExtras) {
            extra.forEach(File::delete);

            //Delete empty directories
            Files.walk(target.toPath())
            .filter(Files::isDirectory)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .filter(f -> f.list().length == 0)
            .forEach(File::delete);
        }
    }

    public static File updateDownload(Project project, File target, Download dl) throws IOException {
        if (!target.exists() || !HashFunction.SHA1.hash(target).equals(dl.getSha1())) {
            project.getLogger().lifecycle("Downloading: " + dl.getUrl());

            if (!target.getParentFile().exists()) {
                target.getParentFile().mkdirs();
            }

            FileUtils.copyURLToFile(dl.getUrl(), target);
        }
        return target;
    }

    public static void updateHash(File target) throws IOException {
        updateHash(target, HashFunction.values());
    }
    public static void updateHash(File target, HashFunction... functions) throws IOException {
        for (HashFunction function : functions) {
            File cache = new File(target.getAbsolutePath() + "." + function.getExtension());
            if (target.exists()) {
                String hash = function.hash(target);
                Files.write(cache.toPath(), hash.getBytes());
            } else if (cache.exists()) {
                cache.delete();
            }
        }
    }

    public static void forZip(ZipFile zip, IOConsumer<ZipEntry> consumer) throws IOException {
        for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
            consumer.accept(entries.nextElement());
        }
    }

    public static String replace(Map<String, ?> vars, String value) {
        if (value.length() <= 2 || value.indexOf('{') == -1) {
            return value;
        }

        return replaceTokens(vars, value);
    }

    public static <T> T fromJson(File file, Class<T> assetIndexClass) {
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            return GSON.fromJson(new InputStreamReader(fileInputStream), assetIndexClass);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find the file!", e);
        }
        finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new RuntimeException("Failed to close file stream!", e);
                }
            }
        }
    }

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T value) throws IOException;
    }

    /**
     * Resolves the supplied object to a string.
     * If the input is null, this will return null.
     * Closures and Callables are called with no arguments.
     * Arrays use Arrays.toString().
     * File objects return their absolute paths.
     * All other objects have their toString run.
     * @param obj Object to resolve
     * @return resolved string
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public static String resolveString(@Nullable Object obj) {
        if (obj == null)
            return null;
        else if (obj instanceof String) // stop early if its the right type. no need to do more expensive checks
            return (String)obj;
        else if (obj instanceof Closure)
            return resolveString(((Closure)obj).call());// yes recursive.
        else if (obj instanceof Callable) {
            try {
                return resolveString(((Callable)obj).call());
            } catch (Exception e) {
                return null;
            }
        } else if (obj instanceof File)
            return ((File) obj).getAbsolutePath();
        else if (obj.getClass().isArray()) { // arrays
            if (obj instanceof Object[])
                return Arrays.toString(((Object[]) obj));
            else if (obj instanceof byte[])
                return Arrays.toString(((byte[]) obj));
            else if (obj instanceof char[])
                return Arrays.toString(((char[]) obj));
            else if (obj instanceof int[])
                return Arrays.toString(((int[]) obj));
            else if (obj instanceof float[])
                return Arrays.toString(((float[]) obj));
            else if (obj instanceof double[])
                return Arrays.toString(((double[]) obj));
            else if (obj instanceof long[])
                return Arrays.toString(((long[]) obj));
            else
                return obj.getClass().getSimpleName();
        }
        return obj.toString();
    }

    public static <T> T[] toArray(JsonArray array, Function<JsonElement, T> adapter, IntFunction<T[]> arrayFactory) {
        return StreamSupport.stream(array.spliterator(), false).map(adapter).toArray(arrayFactory);
    }

    public static byte[] getZipData(File file, String name) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null)
                throw new IOException("Zip Missing Entry: " + name + " File: " + file);

            return IOUtils.toByteArray(zip.getInputStream(entry));
        }
    }

    public static ZipEntry getStableEntry(String name) {
        return getStableEntry(name, Constants.ZIPTIME);
    }

    public static ZipEntry getStableEntry(String name, long time) {
        TimeZone _default = TimeZone.getDefault();
        TimeZone.setDefault(Constants.GMT);
        ZipEntry ret = new ZipEntry(name);
        ret.setTime(time);
        TimeZone.setDefault(_default);
        return ret;
    }

    public static File getMCDir()
    {
        switch (VersionJson.OS.getCurrent()) {
            case OSX:
                return new File(System.getProperty("user.home") + "/Library/Application Support/minecraft");
            case WINDOWS:
                return new File(System.getenv("APPDATA") + "\\.minecraft");
            case LINUX:
            default:
                return new File(System.getProperty("user.home") + "/.minecraft");
        }
    }

    public static String replaceTokens(Map<String, ?> tokens, String value) {
        StringBuilder buf = new StringBuilder();

        for (int x = 0; x < value.length(); x++) {
            char c = value.charAt(x);
            if (c == '\\') {
                if (x == value.length() - 1)
                    throw new IllegalArgumentException("Illegal pattern (Bad escape): " + value);
                buf.append(value.charAt(++x));
            } else if (c == '{' || c ==  '\'') {
                StringBuilder key = new StringBuilder();
                for (int y = x + 1; y <= value.length(); y++) {
                    if (y == value.length())
                        throw new IllegalArgumentException("Illegal pattern (Unclosed " + c + "): " + value);
                    char d = value.charAt(y);
                    if (d == '\\') {
                        if (y == value.length() - 1)
                            throw new IllegalArgumentException("Illegal pattern (Bad escape): " + value);
                        key.append(value.charAt(++y));
                    } else if (c == '{' && d == '}') {
                        x = y;
                        break;
                    } else if (c == '\'' && d == '\'') {
                        x = y;
                        break;
                    } else
                        key.append(d);
                }
                if (c == '\'')
                    buf.append(key);
                else {
                    Object v = tokens.get(key.toString());
                    if (v instanceof Supplier)
                        v = ((Supplier<?>) v).get();

                    buf.append(v == null ? "{" + key + "}" : v);
                }
            } else {
                buf.append(c);
            }
        }

        return buf.toString();
    }
}
