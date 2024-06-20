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
import net.neoforged.gradle.dsl.platform.util.LibraryCollector
import net.neoforged.gradle.util.ModuleDependencyUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.Nullable

import javax.inject.Inject
import java.lang.reflect.Type
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

    /**
     * Track which tools were already added to avoid re-resolving the download URLs for the same tool over and over.
     */
    private final Set<String> toolLibrariesAdded = new HashSet<>();

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

    private NamedDomainObjectProvider<Configuration> getOrCreateConfigurationForTool(Project project, String tool) {
        var configName = "neoForgeInstallerTool" + ModuleDependencyUtils.toConfigurationName(tool)
        try {
            return project.configurations.named(configName)
        } catch (UnknownDomainObjectException ignored) {
            return project.configurations.register(configName, (Configuration spec) -> {
                spec.canBeConsumed = false
                spec.canBeResolved = true
                spec.dependencies.add(project.getDependencies().create(tool))
            })
        }
    }

    private static Provider<Set<Library>> gatherLibrariesFromConfiguration(Project project, Provider<Configuration> configurationProvider) {
        var repositoryUrls = project.getRepositories()
                .withType(MavenArtifactRepository).stream().map { it.url }.collect(Collectors.toList())
        var logger = project.logger

        var objectFactory = project.objects

        // We use a property because it is *not* re-evaluated when queried, while a normal provider is
        var property = project.objects.setProperty(Library.class)
        property.set(configurationProvider.flatMap { config ->
            logger.info("Finding download URLs for configuration ${config.name}")
            config.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                var libraryCollector = new LibraryCollector(objectFactory, repositoryUrls, logger)

                for (ResolvedArtifactResult resolvedArtifact in artifacts) {
                    libraryCollector.visit(resolvedArtifact)
                }

                libraryCollector.getLibraries()
            }
        })
        property.finalizeValueOnRead()
        property.disallowChanges()
        return property
    }

    private static Provider<Set<String>> gatherLibraryIdsFromConfiguration(Project project, Provider<Configuration> configurationProvider) {
        // We use a property because it is *not* re-evaluated when queried, while a normal provider is
        var property = project.objects.setProperty(String.class)
        def logger = project.logger
        property.set(configurationProvider.flatMap { config ->
            config.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                artifacts.collect {
                    def componentId = it.id.componentIdentifier
                    if (componentId instanceof ModuleComponentIdentifier) {
                        var group = componentId.getGroup()
                        var module = componentId.getModule()
                        var version = componentId.getVersion()
                        var classifier = LibraryCollector.guessMavenClassifier(it.file, componentId)
                        var extension = FilenameUtils.getExtension(it.file.name)
                        if (classifier != "") {
                            version += ":" + classifier
                        }
                        return "$group:$module:$version@$extension".toString()
                    } else {
                        logger.warn("Cannot handle component: " + componentId)
                        return null
                    }
                }
            }
        })
        property.finalizeValueOnRead()
        property.disallowChanges()
        return property
    }

    @ClosureEquivalent
    void processor(Project project, String tool, Action<Processor> configurator) {
        var processor = getObjectFactory().newInstance(Processor.class).configure(configurator);
        processor.jar.set(tool)
        getProcessors().add(processor)

        var toolConfiguration = getOrCreateConfigurationForTool(project, tool)
        processor.classpath.set(gatherLibraryIdsFromConfiguration(project, toolConfiguration))
        if (toolLibrariesAdded.add(tool)) {
            getLibraries().addAll gatherLibrariesFromConfiguration(project, toolConfiguration)
        }
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
    abstract static class Processor implements ConfigurableDSLElement<Processor> {
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
