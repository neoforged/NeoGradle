package net.neoforged.gradle.dsl.platform.model

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.ClosureEquivalent
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils
import net.neoforged.gradle.dsl.platform.util.LibrariesTransformer
import net.neoforged.gradle.dsl.platform.util.LibraryCollector
import net.neoforged.gradle.util.ModuleDependencyUtils
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.Nullable

import javax.inject.Inject
import java.lang.reflect.Type
import java.util.concurrent.Callable
import java.util.function.BiFunction
import java.util.stream.Collectors

import static net.neoforged.gradle.dsl.common.util.PropertyUtils.deserializeBool
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.deserializeList
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.deserializeMap
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.deserializeSet
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.deserializeString
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.serializeBool
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.serializeList
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.serializeMap
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.serializeSet
import static net.neoforged.gradle.dsl.common.util.PropertyUtils.serializeString

@CompileStatic
abstract class InstallerProfile implements ConfigurableDSLElement<InstallerProfile> {

    static Gson createGson(ObjectFactory factory) {
        return new Gson().newBuilder()
                .disableHtmlEscaping()
                .registerTypeHierarchyAdapter(InstallerProfile.class, new Serializer(factory))
                .registerTypeHierarchyAdapter(Processor.class, new Processor.Serializer(factory))
                .registerTypeHierarchyAdapter(DataFile.class, new DataFile.Serializer(factory))
                .registerTypeHierarchyAdapter(Library.class, new Library.Serializer(factory))
                .registerTypeHierarchyAdapter(LibraryDownload.class, new LibraryDownload.Serializer(factory))
                .registerTypeHierarchyAdapter(Rule.class, new Rule.Serializer(factory))
                .registerTypeHierarchyAdapter(OsCondition.class, new OsCondition.Serializer(factory))
                .registerTypeHierarchyAdapter(Artifact.class, new Artifact.Serializer(factory))
                .create()
    }

    @Inject
    abstract ObjectFactory getObjectFactory()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getProfile();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getVersion()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getIcon()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getMinecraft()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getJson()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getLogo()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getPath()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getUrlIcon()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getWelcome()

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getMirrorList()

    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getShouldHideClient()

    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getShouldHideServer()

    @Input
    @DSLProperty
    @Optional
    abstract Property<Boolean> getShouldHideExtract()

    @Input
    @DSLProperty
    @Optional
    abstract MapProperty<String, DataFile> getData();

    void data(String key, @Nullable String client, @Nullable String server) {
        getData().put(key, getObjectFactory().newInstance(DataFile.class).configure { DataFile it ->
            if (client != null)
                it.getClient().set(client)
            if (server != null)
                it.getServer().set(server)
        })
    }

    void data(String key, @Nullable Provider<String> client, @Nullable Provider<String> server) {
        getData().put(key, getObjectFactory().newInstance(DataFile.class).configure { DataFile it ->
            if (client != null)
                it.getClient().set(client)
            if (server != null)
                it.getServer().set(server)
        })
    }

    @Nested
    @DSLProperty
    @Optional
    abstract ListProperty<Processor> getProcessors();

    @ClosureEquivalent
    void processor(Project project, Action<ProcessorBuilder> configurator) {
        final ProcessorBuilder builder = getObjectFactory().newInstance(ProcessorBuilder.class, project)
        configurator.execute(builder)

        getProcessors().add(builder.asProcessor(project))

        final List<URI> registries = new ArrayList<>();

        getLibraries().addAll(
                LibrariesTransformer.transform(
                        builder.getToolClasspathArtifactIds(),
                        builder.getToolClasspathArtifactVariants(),
                        builder.getToolClasspathArtifactFiles(),
                        project.getLayout(),
                        project.getObjects()
                )
        )
    }

    @Nested
    @DSLProperty
    @Optional
    abstract SetProperty<Library> getLibraries();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getServerJarPath();

    @CompileStatic
    static class Serializer implements JsonSerializer<InstallerProfile>, JsonDeserializer<InstallerProfile> {

        private final ObjectFactory factory

        Serializer(ObjectFactory factory) {
            this.factory = factory
        }

        @Override
        InstallerProfile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("Installer profile must be a json object")

            final JsonObject payload = jsonElement.getAsJsonObject()
            final InstallerProfile instance = factory.newInstance(InstallerProfile.class)

            deserializeString(instance.getProfile(), payload, "profile")
            deserializeString(instance.getVersion(), payload, "version")
            deserializeString(instance.getIcon(), payload, "icon")
            deserializeString(instance.getMinecraft(), payload, "minecraft")
            deserializeString(instance.getJson(), payload, "json")
            deserializeString(instance.getLogo(), payload, "logo")
            deserializeString(instance.getPath(), payload, "path")
            deserializeString(instance.getUrlIcon(), payload, "urlIcon")
            deserializeString(instance.getWelcome(), payload, "welcome")
            deserializeString(instance.getMirrorList(), payload, "mirrorList")
            deserializeBool(instance.getShouldHideClient(), payload, "hideClient")
            deserializeBool(instance.getShouldHideServer(), payload, "hideServer")
            deserializeBool(instance.getShouldHideExtract(), payload, "hideExtract")
            deserializeMap(instance.getData(), payload, "data", DataFile.class, jsonDeserializationContext)
            deserializeList(instance.getProcessors(), payload, "processors", Processor.class, jsonDeserializationContext)
            deserializeSet(instance.getLibraries(), payload, "libraries", Library.class, jsonDeserializationContext)
            deserializeString(instance.getServerJarPath(), payload, "serverJarPath")

            return instance
        }

        @Override
        JsonElement serialize(InstallerProfile installerProfile, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject()

            object.addProperty("spec", 1)
            serializeString(installerProfile.getProfile(), object, "profile")
            serializeString(installerProfile.getVersion(), object, "version")
            serializeString(installerProfile.getIcon(), object, "icon")
            serializeString(installerProfile.getMinecraft(), object, "minecraft")
            serializeString(installerProfile.getJson(), object, "json")
            serializeString(installerProfile.getLogo(), object, "logo")
            serializeString(installerProfile.getPath(), object, "path")
            serializeString(installerProfile.getUrlIcon(), object, "urlIcon")
            serializeString(installerProfile.getWelcome(), object, "welcome")
            serializeString(installerProfile.getMirrorList(), object, "mirrorList")
            serializeBool(installerProfile.getShouldHideClient(), object, "hideClient")
            serializeBool(installerProfile.getShouldHideServer(), object, "hideServer")
            serializeBool(installerProfile.getShouldHideExtract(), object, "hideExtract")
            serializeMap(installerProfile.getData(), object, "data", jsonSerializationContext)
            serializeList(installerProfile.getProcessors(), object, "processors", jsonSerializationContext)
            serializeSet(installerProfile.getLibraries(), object, "libraries", jsonSerializationContext)
            serializeString(installerProfile.getServerJarPath(), object, "serverJarPath")

            return object
        }
    }

    @CompileStatic
    abstract static class ProcessorBuilder implements ConfigurableDSLElement<ProcessorBuilder> {

        private final Project project;

        @Inject
        ProcessorBuilder(Project project) {
            this.project = project
        }

        void withToolFrom(final Configuration configuration) {
            getToolComponent().set(configuration.getIncoming().getResolutionResult().getRootComponent())
            
            getToolClasspathArtifacts().set(configuration.getIncoming().getArtifacts().getArtifacts())
            
            getToolClasspathArtifactIds().set(getToolClasspathArtifacts().map(new LibrariesTransformer.IdExtractor()))
            getToolClasspathArtifactVariants().set(getToolClasspathArtifacts().map(new LibrariesTransformer.VariantExtractor()))
            getToolClasspathArtifactFiles().set(getToolClasspathArtifacts().map(new LibrariesTransformer.FileExtractor(project.getLayout())))
        }

        @Input
        @DSLProperty
        @Optional
        abstract Property<ResolvedComponentResult> getToolComponent();

        @Internal
        @DSLProperty
        @Optional
        abstract SetProperty<ResolvedArtifactResult> getToolClasspathArtifacts();

        @Input
        @DSLProperty
        @Optional
        abstract ListProperty<ComponentArtifactIdentifier> getToolClasspathArtifactIds();

        @Input
        @DSLProperty
        @Optional
        abstract ListProperty<ResolvedVariantResult> getToolClasspathArtifactVariants();

        @InputFiles
        @DSLProperty
        @Optional
        @Classpath
        abstract ListProperty<RegularFile> getToolClasspathArtifactFiles();

        @Input
        @DSLProperty
        @Optional
        abstract ListProperty<String> getSides();

        void server() {
            getSides().add("server")
        }

        void client() {
            getSides().add("client")
        }

        @Input
        @DSLProperty
        @Optional
        abstract ListProperty<String> getArguments();

        @Input
        @DSLProperty
        @Optional
        abstract MapProperty<String, String> getOutputs();

        private Processor asProcessor(final Project project) {
            final Processor processor = project.getObjects().newInstance(Processor.class)

            processor.getSides().addAll(getSides())
            processor.getJar().set(getToolComponent().map { it -> it.id.displayName })
            processor.getClasspath().addAll(getToolComponent().map { it -> collectIds(it) })
            processor.getArguments().addAll(getArguments())
            processor.getOutputs().putAll(getOutputs())

            return processor
        }

        private Iterable<? extends String> collectIds(ResolvedComponentResult libraries) {
            final Set<String> seen = new HashSet<>();
            collectIds(libraries, seen);
            return seen;
        }

        private void collectIds(ResolvedComponentResult result, Set<String> seen) {
            if (seen.add(result.id.displayName)) {
                for (DependencyResult dependency : result.getDependencies()) {
                    if (dependency instanceof ResolvedDependencyResult) {
                        final ResolvedDependencyResult resolvedDependency = (ResolvedDependencyResult) dependency;
                        collectIds(resolvedDependency.getSelected(), seen);
                    } else {
                        throw new IllegalStateException("Unresolved dependency type: " + dependency.getRequested().getDisplayName());
                    }
                }
            }
        }
    }

    @CompileStatic
    abstract static class Processor implements ConfigurableDSLElement<Processor> {
        @Input
        @DSLProperty
        @Optional
        abstract ListProperty<String> getSides();

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getJar();

        @Input
        @DSLProperty
        @Optional
        abstract SetProperty<String> getClasspath();

        @Input
        @DSLProperty
        @Optional
        abstract ListProperty<String> getArguments();

        @Input
        @DSLProperty
        @Optional
        abstract MapProperty<String, String> getOutputs();

        @CompileStatic
        static class Serializer implements JsonSerializer<Processor>, JsonDeserializer<Processor> {

            private final ObjectFactory factory

            Serializer(ObjectFactory factory) {
                this.factory = factory
            }

            @Override
            Processor deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (!jsonElement.isJsonObject())
                    throw new JsonParseException("Processor must be a json object")

                final JsonObject payload = jsonElement.getAsJsonObject()
                final Processor instance = factory.newInstance(Processor.class)

                deserializeList(instance.getSides(), payload, "sides", String.class, jsonDeserializationContext)
                deserializeString(instance.getJar(), payload, "jar")
                deserializeSet(instance.getClasspath(), payload, "classpath", String.class, jsonDeserializationContext)
                deserializeList(instance.getArguments(), payload, "args", String.class, jsonDeserializationContext)
                deserializeMap(instance.getOutputs(), payload, "outputs", String.class, jsonDeserializationContext)

                return instance
            }

            @Override
            JsonElement serialize(Processor processor, Type type, JsonSerializationContext jsonSerializationContext) {
                final JsonObject object = new JsonObject()

                serializeList(processor.getSides(), object, "sides", jsonSerializationContext)
                serializeString(processor.getJar(), object, "jar")
                serializeSet(processor.getClasspath(), object, "classpath", jsonSerializationContext)
                serializeList(processor.getArguments(), object, "args", jsonSerializationContext)
                serializeMap(processor.getOutputs(), object, "outputs", jsonSerializationContext)

                return object
            }
        }
    }

    @CompileStatic
    abstract static class DataFile implements ConfigurableDSLElement<DataFile> {
        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getClient()

        @Input
        @DSLProperty
        @Optional
        abstract Property<String> getServer()

        @CompileStatic
        static class Serializer implements JsonSerializer<DataFile>, JsonDeserializer<DataFile> {

            private final ObjectFactory factory

            Serializer(ObjectFactory factory) {
                this.factory = factory
            }

            @Override
            DataFile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                if (!jsonElement.isJsonObject())
                    throw new JsonParseException("Data file must be a json object")

                final JsonObject payload = jsonElement.getAsJsonObject()
                final DataFile instance = factory.newInstance(DataFile.class)

                deserializeString(instance.getClient(), payload, "client")
                deserializeString(instance.getServer(), payload, "server")

                return instance
            }

            @Override
            JsonElement serialize(DataFile dataFile, Type type, JsonSerializationContext jsonSerializationContext) {
                final JsonObject object = new JsonObject()

                serializeString(dataFile.getClient(), object, "client")
                serializeString(dataFile.getServer(), object, "server")

                return object
            }
        }
    }
}
