package net.neoforged.gradle.dsl.platform.model

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import javax.inject.Inject
import java.lang.reflect.Type

import static net.neoforged.gradle.dsl.common.util.PropertyUtils.*

@CompileStatic
abstract class LauncherProfile implements ConfigurableDSLElement<LauncherProfile> {

    @Inject
    public LauncherProfile(ObjectFactory factory) {
        this.getArguments().set(factory.newInstance(Arguments.class))
        this.getAssetIndex().set(factory.newInstance(AssetIndex.class))
        this.getJavaVersion().set(factory.newInstance(JavaVersion.class))
        this.getLoggingConfiguration().set(factory.newInstance(LoggingConfiguration.class))
    }

    static Gson createGson(ObjectFactory factory) {
        final GsonBuilder builder = new GsonBuilder().disableHtmlEscaping();

        builder.registerTypeHierarchyAdapter(LauncherProfile.class, new Serializer(factory))
        builder.registerTypeHierarchyAdapter(Arguments.class, new Arguments.Serializer(factory))
        builder.registerTypeHierarchyAdapter(Argument.class, new Argument.Serializer(factory))
        builder.registerTypeHierarchyAdapter(Library.class, new Library.Serializer(factory))
        builder.registerTypeHierarchyAdapter(LibraryDownload.class, new LibraryDownload.Serializer(factory))
        builder.registerTypeHierarchyAdapter(Rule.class, new Rule.Serializer(factory))
        builder.registerTypeHierarchyAdapter(OsCondition.class, new OsCondition.Serializer(factory))
        builder.registerTypeHierarchyAdapter(AssetIndex.class, new AssetIndex.Serializer(factory))
        builder.registerTypeHierarchyAdapter(Download.class, new Download.Serializer(factory))
        builder.registerTypeHierarchyAdapter(Artifact.class, new Artifact.Serializer(factory))
        builder.registerTypeHierarchyAdapter(NamedFile.class, new NamedFile.Serializer(factory))
        builder.registerTypeHierarchyAdapter(JavaVersion.class, new JavaVersion.Serializer(factory))
        builder.registerTypeHierarchyAdapter(LoggingConfiguration.class, new LoggingConfiguration.Serializer(factory))

        return builder.create()
    }

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getId();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getTime();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getReleaseTime();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getType();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getMainClass();

    @Input
    @DSLProperty
    @Optional
    abstract Property<Integer> getMinimumLauncherVersion();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getInheritsFrom();

    @DSLProperty
    @Nested
    @Optional
    abstract Property<Arguments> getArguments();

    @DSLProperty
    @Nested
    @Optional
    abstract Property<AssetIndex> getAssetIndex();

    @Input
    @DSLProperty
    @Optional
    abstract Property<Integer> getAssets();

    @Input
    @DSLProperty
    @Optional
    abstract Property<Integer> getComplianceLevel();

    @Nested
    @DSLProperty
    @Optional
    abstract MapProperty<String, LibraryDownload> getDownloads();

    @Nested
    @DSLProperty
    @Optional
    abstract Property<JavaVersion> getJavaVersion();

    @Nested
    @DSLProperty
    @Optional
    abstract ListProperty<Library> getLibraries();

    @Nested
    @DSLProperty
    @Optional
    abstract Property<LoggingConfiguration> getLoggingConfiguration();

    @CompileStatic
    static class Serializer implements JsonSerializer<LauncherProfile>, JsonDeserializer<LauncherProfile> {

        private final ObjectFactory factory;

        Serializer(ObjectFactory factory) {
            this.factory = factory
        }

        @Override
        LauncherProfile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject())
                throw new JsonSyntaxException("A launcher profile needs to be a json object");

            final JsonObject payload = jsonElement.getAsJsonObject();
            final LauncherProfile instance = factory.newInstance(LauncherProfile.class);

            deserializeString(instance.getId(), payload, "id")
            deserializeString(instance.getTime(), payload, "time")
            deserializeString(instance.getReleaseTime(), payload, "releaseTime")
            deserializeString(instance.getType(), payload, "type")
            deserializeString(instance.getMainClass(), payload, "mainClass")
            deserializeInt(instance.getMinimumLauncherVersion(), payload, "minimumLauncherVersion")
            deserializeString(instance.getInheritsFrom(), payload, "inheritsFrom")
            deserialize(instance.getArguments(), payload, "arguments", Arguments.class, jsonDeserializationContext)
            deserialize(instance.getAssetIndex(), payload, "assetIndex", AssetIndex.class, jsonDeserializationContext)
            deserializeInt(instance.getAssets(), payload, "assetIndex")
            deserializeInt(instance.getComplianceLevel(), payload, "complianceLevel")
            deserialize(instance.getJavaVersion(), payload, "javaVersion", JavaVersion.class, jsonDeserializationContext)
            deserialize(instance.getLoggingConfiguration(), payload, "logging", LoggingConfiguration.class, jsonDeserializationContext)
            deserializeList(instance.getLibraries(), payload, "libraries", Library.class, jsonDeserializationContext)

            return instance
        }

        @Override
        JsonElement serialize(LauncherProfile launcherProfile, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject();

            serializeString(launcherProfile.getId(), object, "id")
            serializeString(launcherProfile.getTime(), object, "time")
            serializeString(launcherProfile.getReleaseTime(), object, "releaseTime")
            serializeString(launcherProfile.getType(), object, "type")
            serializeString(launcherProfile.getMainClass(), object, "mainClass")
            serializeInt(launcherProfile.getMinimumLauncherVersion(), object, "minimumLauncherVersion")
            serializeString(launcherProfile.getInheritsFrom(), object, "inheritsFrom")
            serializeObject(launcherProfile.getArguments(), object, "arguments", jsonSerializationContext)
            serializeObject(launcherProfile.getAssetIndex(), object, "assetIndex", jsonSerializationContext)
            serializeInt(launcherProfile.getAssets(), object, "assetIndex")
            serializeInt(launcherProfile.getComplianceLevel(), object, "complianceLevel")
            serializeObject(launcherProfile.getJavaVersion(), object, "javaVersion", jsonSerializationContext)
            serializeObject(launcherProfile.getLoggingConfiguration(), object, "logging", jsonSerializationContext)
            serializeList(launcherProfile.getLibraries(), object, "libraries", jsonSerializationContext)

            return object
        }
    }

    @CompileStatic
    abstract static class Arguments implements ConfigurableDSLElement<Arguments> {

        @Inject
        abstract ObjectFactory getObjectFactory();

        @Nested
        @DSLProperty
        @Optional
        abstract ListProperty<Argument> getGame();

        Argument game(final String value) {
            final Argument argument = getObjectFactory().newInstance(Argument.class);
            argument.getValue().set(value);
            getGame().add(argument);
            return argument;
        }

        Argument game(final Provider<String> value) {
            final Argument argument = getObjectFactory().newInstance(Argument.class);
            argument.getValue().set(value);
            getGame().add(argument);
            return argument;
        }

        @DSLProperty
        @Nested
        @Optional
        abstract ListProperty<Argument> getJVM();

        Argument jvm(final String value) {
            final Argument argument = getObjectFactory().newInstance(Argument.class);
            argument.getValue().set(value);
            getJVM().add(argument);
            return argument;
        }

        Argument jvm(final Provider<String> value) {
            final Argument argument = getObjectFactory().newInstance(Argument.class);
            argument.getValue().set(value);
            getJVM().add(argument);
            return argument;
        }

        @CompileStatic
        static class Serializer implements JsonSerializer<Arguments>, JsonDeserializer<Arguments> {

            private final ObjectFactory factory

            Serializer(ObjectFactory factory) {
                this.factory = factory
            }

            @Override
            Arguments deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (!jsonElement.isJsonObject())
                    throw new JsonParseException("Arguments must be a json object")

                final JsonObject payload = jsonElement.getAsJsonObject();
                final Arguments instance = factory.newInstance(Arguments.class);

                deserializeList(instance.getGame(), payload, "game", Argument.class, jsonDeserializationContext)
                deserializeList(instance.getJVM(), payload, "jvm", Argument.class, jsonDeserializationContext)

                return instance;
            }

            @Override
            JsonElement serialize(Arguments arguments, Type type, JsonSerializationContext jsonSerializationContext) {
                final JsonObject object = new JsonObject();

                serializeList(arguments.getGame(), object, "game", jsonSerializationContext)
                serializeList(arguments.getJVM(), object, "jvm", jsonSerializationContext)

                return object;
            }
        }
    }

    @CompileStatic
    abstract static class Argument extends WithRules<Argument> {

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getValue();

        @CompileStatic
        static class Serializer extends WithRules.Serializer<Argument> {

            Serializer(ObjectFactory factory) {
                super(factory, Argument.class)
            }

            @Override
            Argument deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (jsonElement.isJsonPrimitive()) {
                    final Argument argument = factory.newInstance(Argument.class);
                    argument.getValue().set(jsonElement.getAsString());
                    return argument;
                }

                def result = super.deserialize(jsonElement, type, jsonDeserializationContext) as Argument

                deserializeString(result.getValue(), jsonElement.getAsJsonObject(), "value")

                return result;
            }

            @Override
            JsonElement serialize(Argument argument, Type type, JsonSerializationContext jsonSerializationContext) {
                if (argument.getRules().isPresent() && argument.getRules().get().isEmpty()) {
                    return new JsonPrimitive(argument.getValue().get())
                }

                def result = super.serialize(argument, type, jsonSerializationContext) as JsonObject

                serializeString(argument.getValue(), result, "value")

                return result;
            }
        }

    }

    @CompileStatic
    abstract static class AssetIndex extends FileReference<AssetIndex> {

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getId();

        @Input
        @DSLProperty
        @Optional
        abstract Property<Integer> getTotalSize();

        @CompileStatic
        static class Serializer extends FileReference.Serializer<AssetIndex> {

            Serializer(ObjectFactory factory) {
                super(factory, AssetIndex.class)
            }

            @Override
            AssetIndex deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                def result = super.deserialize(jsonElement, type, jsonDeserializationContext) as AssetIndex;

                deserializeString(result.getId(), jsonElement.getAsJsonObject(), "id")
                deserializeInt(result.getTotalSize(), jsonElement.getAsJsonObject(), "totalSize")

                return result;
            }

            @Override
            JsonObject serialize(AssetIndex assetIndex, Type type, JsonSerializationContext jsonSerializationContext) {
                def object = super.serialize(assetIndex, type, jsonSerializationContext)

                serializeString(assetIndex.getId(), object, "id")
                serializeInt(assetIndex.getTotalSize(), object, "totalSize")

                return object;
            }
        }
    }

    @CompileStatic
    abstract static class Download extends FileReference<Download> {

        @CompileStatic
        static class Serializer extends FileReference.Serializer<Download> {

            Serializer(ObjectFactory factory) {
                super(factory, Download.class)
            }
        }
    }

    @CompileStatic
    abstract static class NamedFile extends FileReference<NamedFile> implements ConfigurableDSLElement<NamedFile> {

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getId();

        @CompileStatic
        static class Serializer extends FileReference.Serializer<NamedFile> {

            Serializer(ObjectFactory factory) {
                super(factory, NamedFile.class)
            }

            @Override
            NamedFile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                def result = super.deserialize(jsonElement, type, jsonDeserializationContext) as NamedFile;

                deserializeString(result.getId(), jsonElement.getAsJsonObject(), "id")

                return result;
            }

            @Override
            JsonObject serialize(NamedFile namedFile, Type type, JsonSerializationContext jsonSerializationContext) {
                def object = super.serialize(namedFile, type, jsonSerializationContext)

                serializeString(namedFile.getId(), object, "id")

                return object;
            }
        }
    }

    @CompileStatic
    abstract static class JavaVersion implements ConfigurableDSLElement<JavaVersion> {

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getComponent();

        @Input
        @DSLProperty
        @Optional
        abstract Property<Integer> getMajorVersion();

        @CompileStatic
        static class Serializer implements JsonSerializer<JavaVersion>, JsonDeserializer<JavaVersion> {

            private final ObjectFactory factory;

            Serializer(ObjectFactory factory) {
                this.factory = factory
            }

            @Override
            JavaVersion deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (!jsonElement.isJsonObject())
                    throw new JsonParseException("Java version must be a json object")

                final JsonObject payload = jsonElement.getAsJsonObject();
                final JavaVersion instance = factory.newInstance(JavaVersion.class);

                deserializeString(instance.getComponent(), payload, "component")
                deserializeInt(instance.getMajorVersion(), payload, "majorVersion")

                return instance;
            }

            @Override
            JsonElement serialize(JavaVersion javaVersion, Type type, JsonSerializationContext jsonSerializationContext) {
                final JsonObject object = new JsonObject();

                serializeString(javaVersion.getComponent(), object, "component")
                serializeInt(javaVersion.getMajorVersion(), object, "majorVersion")

                return object;
            }
        }
    }

    @CompileStatic
    abstract static class LoggingConfiguration implements ConfigurableDSLElement<LoggingConfiguration> {

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getArgument();

        @Nested
        @DSLProperty
        @Optional
        abstract Property<NamedFile> getFile();

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getType();

        @CompileStatic
        static class Serializer implements JsonSerializer<LoggingConfiguration>, JsonDeserializer<LoggingConfiguration> {

            private final ObjectFactory factory;

            Serializer(ObjectFactory factory) {
                this.factory = factory
            }

            @Override
            LoggingConfiguration deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (!jsonElement.isJsonObject())
                    throw new JsonParseException("Logging configuration must be a json object")

                final JsonObject payload = jsonElement.getAsJsonObject();
                final LoggingConfiguration instance = factory.newInstance(LoggingConfiguration.class);

                deserializeString(instance.getArgument(), payload, "argument")
                deserialize(instance.getFile(), payload, "file", NamedFile.class, jsonDeserializationContext)
                deserializeString(instance.getType(), payload, "type")

                return instance;
            }

            @Override
            JsonElement serialize(LoggingConfiguration loggingConfiguration, Type type, JsonSerializationContext jsonSerializationContext) {
                final JsonObject object = new JsonObject();

                serializeString(loggingConfiguration.getArgument(), object, "argument")
                serializeObject(loggingConfiguration.getFile(), object, "file", jsonSerializationContext)
                serializeString(loggingConfiguration.getType(), object, "type")

                return object;
            }
        }
    }
}

