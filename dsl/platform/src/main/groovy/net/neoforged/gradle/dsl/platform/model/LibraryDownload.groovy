package net.neoforged.gradle.dsl.platform.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.util.PropertyUtils
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import java.lang.reflect.Type

@CompileStatic
abstract class LibraryDownload implements ConfigurableDSLElement<LibraryDownload> {

    @Nested
    @DSLProperty
    @Optional
    abstract Property<Artifact> getArtifact();

    @CompileStatic
    static class Serializer implements JsonSerializer<LibraryDownload>, JsonDeserializer<LibraryDownload> {

        private final ObjectFactory factory;

        Serializer(ObjectFactory factory) {
            this.factory = factory
        }

        @Override
        LibraryDownload deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("Library download must be a json object")

            final JsonObject payload = jsonElement.getAsJsonObject();
            final LibraryDownload instance = factory.newInstance(LibraryDownload.class);

            PropertyUtils.deserialize(instance.getArtifact(), payload, "artifact", Artifact.class, jsonDeserializationContext)

            return instance;
        }

        @Override
        JsonElement serialize(LibraryDownload libraryDownload, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject();

            PropertyUtils.serializeObject(libraryDownload.getArtifact(), object, "artifact", jsonSerializationContext)

            return object;
        }
    }

}
