package net.neoforged.gradle.dsl.platform.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.util.PropertyUtils
import net.neoforged.gradle.util.HashFunction
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

import java.lang.reflect.Type

abstract class Library extends WithRules<Library> {

    static Provider<Library> fromOutput(final TaskProvider<? extends WithOutput> producerTask, final Project project, final String classifier) {
        return producerTask.map { task ->
            def result = project.getObjects().newInstance(Library.class)
            def download = project.getObjects().newInstance(LibraryDownload.class)
            def artifact = project.getObjects().newInstance(Artifact.class)

            result.getName().set("${project.group}:${project.name}:${project.version}" + (classifier == '' ? '' : ':' + classifier))
            result.getDownload().set(download)

            download.artifact.set(artifact)

            artifact.path.set("${project.group.toString().replace('.', '/')}/${project.name}/${project.version}/${project.name}-${project.version}".toString() + (classifier == '' ? '' : '-' + classifier) + '.jar')
            artifact.url.set(artifact.path.map { path ->
                "https://maven.neoforged.net/releases/${path}"
            })
            artifact.sha1.set(task.output.map { file ->
                HashFunction.SHA1.hash(file.asFile)
            })
            artifact.size.set(task.output.map { file ->
                file.asFile.length()
            })

            return result
        }
    }

    static Provider<Library> fromJar(final TaskProvider<Jar> jarTask, final Project project) {
        return jarTask.map { task ->
            def result = project.getObjects().newInstance(Library.class)
            def download = project.getObjects().newInstance(LibraryDownload.class)
            def artifact = project.getObjects().newInstance(Artifact.class)

            def classifier = task.archiveClassifier.get()

            result.getName().set("${project.group}:${project.name}:${project.version}" + (classifier == '' ? '' : ':' + classifier))
            result.getDownload().set(download)

            download.artifact.set(artifact)

            artifact.path.set("${project.group.toString().replace('.', '/')}/${project.name}/${project.version}/${project.name}-${project.version}".toString() + (classifier == '' ? '' : '-' + classifier) + '.jar')
            artifact.url.set(artifact.path.map { path ->
                "https://maven.neoforged.net/releases/${path}"
            })
            artifact.sha1.set(task.archiveFile.map { file ->
                HashFunction.SHA1.hash(file.asFile)
            })
            artifact.size.set(task.archiveFile.map { file ->
                file.asFile.length()
            })

            return result
        }
    }

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getName();

    @Nested
    @DSLProperty
    @Optional
    abstract Property<LibraryDownload> getDownload();

    static class Serializer extends WithRules.Serializer<Library> {

        Serializer(ObjectFactory factory) {
            super(factory, Library.class)
        }

        @Override
        Library deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            def result = super.deserialize(jsonElement, type, jsonDeserializationContext) as Library

            PropertyUtils.deserializeString(result.getName(), jsonElement.getAsJsonObject(), "name")
            PropertyUtils.deserialize(result.getDownload(), jsonElement.getAsJsonObject(), "downloads", LibraryDownload.class, jsonDeserializationContext)

            return result;
        }

        @Override
        JsonObject serialize(Library library, Type type, JsonSerializationContext jsonSerializationContext) {
            def result = super.serialize(library, type, jsonSerializationContext) as JsonObject

            PropertyUtils.serializeString(library.getName(), result, "name")
            PropertyUtils.serializeObject(library.getDownload(), result, "downloads", jsonSerializationContext)

            return result;
        }
    }

}
