package net.neoforged.gradle.dsl.userdev.configurations

import com.google.gson.*
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.ClosureEquivalent
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.runs.type.RunType
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
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
import java.util.function.BiFunction
import java.util.function.Function

import static net.neoforged.gradle.dsl.common.util.PropertyUtils.*

@CompileStatic
abstract class UserdevProfile implements ConfigurableDSLElement<UserdevProfile> {

    public static Gson createGson(ObjectFactory objectFactory) {
        return new GsonBuilder().disableHtmlEscaping()
                .registerTypeAdapter(UserdevProfile.class, new Serializer(objectFactory))
                .registerTypeAdapter(ToolExecution.class, new ToolExecution.Serializer(objectFactory))
                .create()
    }

    @Inject
    abstract ObjectFactory getObjectFactory();

    @Input
    @DSLProperty
    abstract Property<String> getNeoForm()

    @Input
    @DSLProperty
    abstract ListProperty<String> getAccessTransformerFiles()

    void accessTransformer(final String at) {
        getAccessTransformerFiles().add(at)
    }

    void accessTransformer(final Provider<String> at) {
        getAccessTransformerFiles().add(at)
    }

    @Input
    @DSLProperty
    abstract ListProperty<String> getSideAnnotationStripperFiles()

    void sideAnnotationStripper(final String sas) {
        getSideAnnotationStripperFiles().add(sas)
    }

    void sideAnnotationStripper(final Provider<String> sas) {
        getSideAnnotationStripperFiles().add(sas)
    }

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getBinaryPatchFile();

    @Nested
    @DSLProperty
    @Optional
    abstract Property<ToolExecution> getBinaryPatcher();

    @Input
    @DSLProperty
    abstract Property<String> getSourcePatchesDirectory();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getSourcesJarArtifactCoordinate();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getUniversalJarArtifactCoordinate();

    @Input
    @DSLProperty
    abstract ListProperty<String> getAdditionalDependencyArtifactCoordinates();

    void library(final String library) {
        getAdditionalDependencyArtifactCoordinates().add(library)
    }

    void library(final Provider<String> library) {
        getAdditionalDependencyArtifactCoordinates().add(library)
    }

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getInjectedFilesDirectory();

    @Nested
    @DSLProperty
    abstract NamedDomainObjectCollection<RunType> getRunTypes();

    @ClosureEquivalent
    void runType(final String name, Action<RunType> configurer) {
        final RunType runType = objectFactory.newInstance(RunType.class, name)
        configurer.execute(runType)
        runTypes.add(runType)
    }

    @Input
    @DSLProperty
    abstract ListProperty<String> getModules();

    void module(final Provider<String> module) {
        getModules().add(module)
    }

    @CompileStatic
    static class Serializer implements JsonSerializer<UserdevProfile>, JsonDeserializer<UserdevProfile> {

        private final ObjectFactory objectFactory;

        Serializer(ObjectFactory objectFactory) {
            this.objectFactory = objectFactory
        }

        @Override
        UserdevProfile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("Expected object, found " + jsonElement.getClass().getSimpleName())
            }

            final JsonObject object = jsonElement.getAsJsonObject()
            final UserdevProfile instance = objectFactory.newInstance(UserdevProfile.class)

            deserializeString(instance.neoForm, object, "mcp")
            deserializeList(instance.accessTransformerFiles, object, "ats", String.class, jsonDeserializationContext)
            deserializeList(instance.sideAnnotationStripperFiles, object, "sass", String.class, jsonDeserializationContext)
            deserializeString(instance.binaryPatchFile, object, "binpatches")
            deserialize(instance.binaryPatcher, object, "binpatcher", ToolExecution.class, jsonDeserializationContext)
            deserializeString(instance.sourcePatchesDirectory, object, "patches")
            deserializeString(instance.sourcesJarArtifactCoordinate, object, "sources")
            deserializeString(instance.universalJarArtifactCoordinate, object, "universal")
            deserializeList(instance.additionalDependencyArtifactCoordinates, object, "libraries", String.class, jsonDeserializationContext)
            deserializeString(instance.injectedFilesDirectory, object, "inject")
            deserializeNamedDomainCollection(instance.runTypes, object, "runs", new BiFunction<String, JsonElement, RunType>() {
                @Override
                RunType apply(String s, JsonElement element) {
                    if (!element.isJsonObject())
                        throw new JsonSyntaxException("Expected object, found " + element.getClass().getSimpleName());

                    return RunType.Serializer.deserializeNamed(
                            objectFactory,
                            s,
                            element.getAsJsonObject(),
                            jsonDeserializationContext
                    )
                }
            })
            deserializeList(instance.modules, object, "modules", String.class, jsonDeserializationContext)

            return instance
        }

        @Override
        JsonElement serialize(UserdevProfile userdevProfile, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject();

            object.addProperty("spec", 2);

            serializeString(userdevProfile.neoForm, object, "mcp")
            serializeList(userdevProfile.accessTransformerFiles, object, "ats", jsonSerializationContext)
            serializeList(userdevProfile.sideAnnotationStripperFiles, object, "sass", jsonSerializationContext)
            serializeString(userdevProfile.binaryPatchFile, object, "binpatches")
            serialize(userdevProfile.binaryPatcher, object, "binpatcher", jsonSerializationContext)
            serializeString(userdevProfile.sourcePatchesDirectory, object, "patches")
            serializeString(userdevProfile.sourcesJarArtifactCoordinate, object, "sources")
            serializeString(userdevProfile.universalJarArtifactCoordinate, object, "universal")
            serializeList(userdevProfile.additionalDependencyArtifactCoordinates, object, "libraries", jsonSerializationContext)
            serializeString(userdevProfile.injectedFilesDirectory, object, "inject")
            serializeNamedDomainCollection(userdevProfile.runTypes, object, "runs", new Function<RunType, JsonElement>() {
                @Override
                JsonElement apply(RunType v) {
                    return RunType.Serializer.serializedNamed(v, jsonSerializationContext)
                }
            })
            serializeList(userdevProfile.modules, object, "modules", jsonSerializationContext)

            return object
        }
    }

    @CompileStatic
    static abstract class ToolExecution implements ConfigurableDSLElement<UserdevProfile> {

        @Input
        @DSLProperty
        abstract Property<String> getTool();

        @Input
        @DSLProperty
        abstract ListProperty<String> getArgs();

        @Input
        @DSLProperty
        abstract ListProperty<String> getJvmArgs();

        @Input
        @DSLProperty
        abstract MapProperty<String, String> getData();

        @CompileStatic
        static class Serializer implements JsonSerializer<ToolExecution>, JsonDeserializer<ToolExecution> {

            private final ObjectFactory objectFactory

            Serializer(ObjectFactory objectFactory) {
                this.objectFactory = objectFactory
            }

            @Override
            ToolExecution deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (!jsonElement.isJsonObject())
                    throw new JsonSyntaxException("Expected object, found " + jsonElement.getClass().getSimpleName())

                final JsonObject object = jsonElement.getAsJsonObject()
                final ToolExecution instance = objectFactory.newInstance(ToolExecution.class)

                deserializeString(instance.tool, object, "version")
                deserializeList(instance.args, object, "args", String.class, jsonDeserializationContext)
                deserializeList(instance.jvmArgs, object, "jvmArgs", String.class, jsonDeserializationContext)
                deserializeMap(instance.data, object, "data", String.class, jsonDeserializationContext)

                return instance
            }

            @Override
            JsonElement serialize(ToolExecution toolExecution, Type type, JsonSerializationContext jsonSerializationContext) {
                final JsonObject object = new JsonObject();

                serializeString(toolExecution.tool, object, "version")
                serializeList(toolExecution.args, object, "args", jsonSerializationContext)
                serializeList(toolExecution.jvmArgs, object, "jvmArgs", jsonSerializationContext)
                serializeMap(toolExecution.data, object, "data", jsonSerializationContext)

                return object
            }
        }
    }
}
