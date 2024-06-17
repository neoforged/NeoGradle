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

package net.neoforged.gradle.common.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import net.neoforged.gradle.dsl.common.util.Artifact;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public class VersionJson implements Serializable {

    protected static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(VersionJson.Argument.class, new VersionJson.Argument.Deserializer())
            .setPrettyPrinting().create();


    public static VersionJson get(Path path) throws IOException {
        return get(path.toFile());
    }

    public static VersionJson get(@Nullable File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("VersionJson File can not be null!");
        }
        try (InputStream in = new FileInputStream(file)) {
            return get(in);
        }
    }

    public static VersionJson get(InputStream stream) {
        return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), VersionJson.class);
    }

    private String id;
    @Nullable
    private Arguments arguments;
    private AssetIndex assetIndex;
    private String assets;
    @Nullable
    private Map<String, Download> downloads;
    private Library[] libraries;
    private JavaVersion javaVersion;

    private List<LibraryDownload> _natives = null;
    private List<Library> _libraries = null;

    private String mainClass;

    private String type;

    public List<LibraryDownload> getNatives() {


        if (_natives == null) {
            Map<String, Entry> natives = new HashMap<>();

            OS os = OS.getCurrent();
            for (Library lib : libraries) {
                if (!lib.isAllowed())
                    continue;
                String key = lib.getArtifact().getGroup() + ':' + lib.getArtifact().getName() + ':' + lib.getArtifact().getVersion();

                if (lib.getNatives() != null && lib.getDownloads().getClassifiers() != null && lib.getNatives().containsKey(os.getName())) {
                    LibraryDownload l = lib.getDownloads().getClassifiers().get(lib.getNatives().get(os.getName()));
                    if (l != null) {
                        natives.put(key, new Entry(2, l));
                    }
                }
            }

            _natives = natives.values().stream().map(Entry::download).collect(Collectors.toList());
        }
        return _natives;
    }

    public List<String> getPlatformJvmArgs() {
        if (arguments == null || arguments.jvm == null)
            return Collections.emptyList();

        return Stream.of(arguments.jvm).filter(arg -> arg.getRules() != null && arg.isAllowed()).
                flatMap(arg -> arg.value.stream()).
                map(s -> {
                    if (s.indexOf(' ') != -1)
                        return "\"" + s + "\"";
                    else
                        return s;
                }).collect(Collectors.toList());
    }
    
    public String getId() {
        return id;
    }
    
    public Arguments getArguments() {
        return arguments == null ? new Arguments() : arguments;
    }

    public AssetIndex getAssetIndex() {
        return assetIndex;
    }

    public String getAssets() {
        return assets;
    }

    @Nullable
    public Map<String, Download> getDownloads() {
        return downloads;
    }

    public JavaVersion getJavaVersion() {
        return javaVersion;
    }

    public List<Library> getLibraries() {
        if (this._libraries == null) {
            this._libraries = new ArrayList<>();
            for (Library lib : libraries) {
                if (lib.isAllowed())
                    this._libraries.add(lib);
            }
            this._libraries = ImmutableList.copyOf(this._libraries);
        }

        return _libraries;
    }

    public String getType() {
        return type;
    }

    public String getMainClass() {
        return mainClass;
    }

    public static class JavaVersion implements Serializable {
        private String component;
        private String majorVersion;

        public String getComponent() {
            return component;
        }

        public String getMajorVersion() {
            return majorVersion;
        }
    }

    public static class Arguments implements Serializable {
        private Argument[] game;
        @Nullable
        private Argument[] jvm;

        public Argument[] getGame() {
            return game;
        }

        public Argument[] getJvm() {
            return jvm == null ? new Argument[0] : jvm;
        }
    }

    public static class Argument extends RuledObject implements Serializable {
        public List<String> value;

        public Argument(@Nullable Rule[] rules, List<String> value) {
            this.rules = rules;
            this.value = value;
        }

        public static class Deserializer implements JsonDeserializer<VersionJson.Argument> {
            @Override
            public Argument deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive()) {
                    return new Argument(null, Collections.singletonList(json.getAsString()));
                }

                JsonObject obj = json.getAsJsonObject();
                if (!obj.has("rules") || !obj.has("value"))
                    throw new JsonParseException("Error parsing arguments in version json. File is corrupt or its format has changed.");

                JsonElement val = obj.get("value");
                Rule[] rules = GSON.fromJson(obj.get("rules"), Rule[].class);
                @SuppressWarnings("unchecked")
                List<String> value = val.isJsonPrimitive() ? Collections.singletonList(val.getAsString()) : GSON.fromJson(val, List.class);

                return new Argument(rules, value);
            }
        }
    }

    public static class RuledObject implements Serializable {
        @Nullable
        protected Rule[] rules;

        public boolean isAllowed() {
            if (getRules() != null) {
                for (Rule rule : getRules()) {
                    if (!rule.allowsAction()) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Nullable
        public Rule[] getRules() {
            return rules;
        }
    }

    public static class Rule implements Serializable {
        private String action;
        @org.jetbrains.annotations.Nullable
        private OsCondition os;

        public boolean allowsAction() {
            return (getOs() == null || getOs().platformMatches()) == getAction().equals("allow");
        }

        public String getAction() {
            return action;
        }

        @org.jetbrains.annotations.Nullable
        public OsCondition getOs() {
            return os;
        }
    }

    public static class OsCondition implements Serializable {
        @Nullable
        private String name;
        @Nullable
        private String version;
        @Nullable
        private String arch;

        public boolean nameMatches() {
            return getName() == null || OS.getCurrent().getName().equals(getName());
        }

        public boolean versionMatches() {
            return getVersion() == null || Pattern.compile(getVersion()).matcher(System.getProperty("os.version")).find();
        }

        public boolean archMatches() {
            return getArch() == null || Pattern.compile(getArch()).matcher(System.getProperty("os.arch")).find();
        }

        public boolean platformMatches() {
            return nameMatches() && versionMatches() && archMatches();
        }

        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public String getVersion() {
            return version;
        }

        @Nullable
        public String getArch() {
            return arch;
        }
    }

    public static class AssetIndex extends Download implements Serializable {
        private String id;
        private int totalSize;

        public String getId() {
            return id;
        }

        public int getTotalSize() {
            return totalSize;
        }
    }

    public static class Download implements Serializable {
        private String sha1;
        private int size;
        private URL url;

        public String getSha1() {
            return sha1;
        }

        public int getSize() {
            return size;
        }

        public URL getUrl() {
            return url;
        }
    }

    public static class LibraryDownload extends Download implements Serializable {
        private String path;

        public String getPath() {
            return path;
        }
    }

    public static class Downloads implements Serializable {
        @Nullable
        private Map<String, LibraryDownload> classifiers;
        @Nullable
        private LibraryDownload artifact;

        @Nullable
        public Map<String, LibraryDownload> getClassifiers() {
            return classifiers;
        }

        @Nullable
        public LibraryDownload getArtifact() {
            return artifact;
        }
    }

    public static class Library extends RuledObject implements Serializable {
        //Extract? rules?
        private String name;
        private Map<String, String> natives;
        private Downloads downloads;
        private Artifact _artifact;

        public Artifact getArtifact() {
            if (_artifact == null) {
                _artifact = Artifact.from(getName());
            }
            return _artifact;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getNatives() {
            return natives;
        }

        public Downloads getDownloads() {
            return downloads;
        }
    }

    public enum OS {
        WINDOWS("windows", "win"),
        LINUX("linux", "linux", "unix"),
        OSX("osx", "mac"),
        UNKNOWN("unknown");

        private final String name;
        private final String[] keys;

        OS(String name, String... keys) {
            this.name = name;
            this.keys = keys;
        }

        public String getName() {
            return this.name;
        }

        public static OS getCurrent() {
            String prop = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            for (OS os : OS.values()) {
                for (String key : os.keys) {
                    if (prop.contains(key)) {
                        return os;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    private static final class Entry implements Serializable {
        private final int priority;
        private final LibraryDownload download;

        Entry(int priority, LibraryDownload download) {
            this.priority = priority;
            this.download = download;
        }

        public int priority() {
            return priority;
        }

        public LibraryDownload download() {
            return download;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            final Entry that = (Entry) obj;
            return this.priority == that.priority &&
                    Objects.equals(this.download, that.download);
        }

        @Override
        public int hashCode() {
            return Objects.hash(priority, download);
        }

        @Override
        public String toString() {
            return "Entry[" +
                    "priority=" + priority + ", " +
                    "download=" + download + ']';
        }
    }
}
