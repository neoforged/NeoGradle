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

package net.neoforged.gradle.dsl.neoform.configuration;

import com.google.gson.*;

import net.neoforged.gradle.dsl.common.configuration.VersionedConfiguration;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class LegacyNeoFormSdk extends VersionedConfiguration {
    protected static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LegacyNeoFormSdk.Step.class, new LegacyNeoFormSdk.Step.Deserializer())
            .registerTypeAdapter(LegacyNeoFormSdk.StepType.class, new LegacyNeoFormSdk.StepType.Deserializer())
            .setPrettyPrinting().create();

    public static LegacyNeoFormSdk get(InputStream stream) {
        return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), LegacyNeoFormSdk.class);
    }
    public static LegacyNeoFormSdk get(byte[] data) {
        return get(new ByteArrayInputStream(data));
    }

    protected String version; // Minecraft version
    @Nullable
    protected Map<String, Object> data;
    @Nullable
    protected Map<String, List<Step>> steps;
    @Nullable
    protected Map<String, Function> functions;
    @Nullable
    protected Map<String, List<String>> libraries;

    @Input
    public String minecraftVersion() {
        return version;
    }

    @Nested
    @Optional
    public Map<String, Object> getData() {
        return data == null ? Collections.emptyMap() : data;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public String getData(String... path) {
        if (data == null)
            return null;
        Map<String, Object> level = data;
        for (String part : path) {
            if (!level.containsKey(part))
                return null;
            Object val = level.get(part);
            if (val instanceof String)
                return (String)val;
            if (val instanceof Map)
                level = (Map<String, Object>)val;
        }
        return null;
    }

    @Optional
    @Nested
    @Nullable
    public Map<String, List<Step>> getSteps() {
        return steps;
    }

    public List<Step> getSteps(String side) {
        List<Step> ret = steps == null ? null : steps.get(side);
        return ret == null ? Collections.emptyList() : ret;
    }

    @Nullable
    public Function getFunction(String name) {
        return functions == null ? null : functions.get(name);
    }

    @Nested
    @Optional
    public Map<String, Function> getFunctions() {
        return functions == null ? Collections.emptyMap() : functions;
    }

    public List<String> getLibraries(String side) {
        List<String> ret = libraries == null ? null : libraries.get(side);
        return ret == null ? Collections.emptyList() : ret;
    }

    @Nested
    @Optional
    @Nullable
    public Map<String, List<String>> getLibraries() {
        return libraries;
    }

    public enum StepType {
        DOWNLOAD_MANIFEST,
        DOWNLOAD_JSON,
        DOWNLOAD_CLIENT,
        DOWNLOAD_SERVER,
        STRIP,
        LIST_LIBRARIES,
        INJECT,
        PATCH,
        DOWNLOAD_CLIENT_MAPPINGS,
        DOWNLOAD_SERVER_MAPPINGS,
        FUNCTION;

        public String toString() {
            final String name = name();
            final String[] sections = name.split("_");
            if (sections.length == 1)
                return name.toLowerCase();
                        
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sections.length; i++) {
                String section = sections[i];
                if (i == 0) {
                    sb.append(section.toLowerCase());
                } else {
                    sb.append(section.charAt(0));
                    sb.append(section.substring(1).toLowerCase());
                }
            }
            return sb.toString();
        }
        
        public static final class Deserializer implements JsonDeserializer<StepType> {

            @Override
            public StepType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (!json.isJsonPrimitive())
                    throw new JsonParseException("Could not parse step type: Expected a string");

                String value = json.getAsString();
                if (value.equals("function"))
                    throw new JsonParseException("Could not parse step type: Expected a valid type, not a function reference!");

                for (StepType type : StepType.values()) {
                    if (type.toString().equalsIgnoreCase(value))
                        return type;
                }

                //If we do not know the type, it will always be a function
                return FUNCTION;
            }
        }
    }
    
    public static class Step {
        private final StepType type;
        @Nullable
        private final String function;
        private final String name;
        @Nullable
        private final Map<String, String> values;

        private Step(StepType type, @Nullable String function, String name, @Nullable Map<String, String> values) {
            this.type = type;
            this.function = function;
            this.name = name;
            this.values = values;
        }

        @Input
        public StepType getType() {
            return type;
        }

        @Input
        @Optional
        @Nullable
        public String getFunction() {
            return function;
        }

        @Input
        public String getName() {
            return name;
        }

        @Nested
        public Map<String, String> getValues() {
            return values == null ? Collections.emptyMap() : values;
        }

        @Nullable
        public String getValue(String key) {
            return values == null ? null : values.get(key);
        }

        public static class Deserializer implements JsonDeserializer<Step> {
            @Override
            public Step deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject obj = json.getAsJsonObject();
                if (!obj.has("type"))
                    throw new JsonParseException("Could not parse step: Missing 'type'");
                //Deserialize the type string field to an enum entry.
                StepType type = context.deserialize(obj.get("type"), StepType.class);

                //See if we have a function in the first place.
                String function = obj.has("function") ? obj.get("function").getAsString() : null;

                //Functions are special their type is a direct reference to the function.
                //We however make that explicit, so unwrap the function key again, so we keep track of what function to pass in.
                if (type == StepType.FUNCTION)
                    function = obj.get("type").getAsString();

                //If an identifier is given use that
                //If not check, if we have a function key, if that is present use it
                //Else use the display key for the step type (which is its toString value)
                String name = obj.has("identifier") ?
                        obj.get("identifier").getAsString() :
                        function == null ?
                                type.toString() :
                                function;

                //Get the values.
                Map<String, String> values = obj.entrySet().stream()
                        .filter(e -> !"type".equals(e.getKey()) && !"identifier".equals(e.getKey()))
                        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getAsString()));

                return new Step(type, function, name, values);
            }
        }
    }

    public static class Function {
        protected String version; //Maven artifact for the jar to run
        @Nullable
        protected String repo; //Maven repo to download the jar from
        @Nullable
        protected List<String> args;
        @Nullable
        protected List<String> jvmargs;

        @Input
        public String getVersion() {
            return version;
        }
        public void setVersion(String value) {
            this.version = value;
        }

        @Input
        @Optional
        public String getRepo() {
            return repo == null ? UrlConstants.MOJANG_MAVEN : repo;
        }
        public void setRepo(String value) {
            this.repo = value;
        }

        @Input
        @Optional
        public List<String> getArgs() {
            return args == null ? Collections.emptyList() : args;
        }
        public void setArgs(List<String> value) {
            this.args = value;
        }

        @Input
        @Optional
        public List<String> getJvmArgs() {
            return jvmargs == null ? Collections.emptyList() : jvmargs;
        }
        public void setJvmArgs(List<String> value) {
            this.jvmargs = value;
        }
    }
}
