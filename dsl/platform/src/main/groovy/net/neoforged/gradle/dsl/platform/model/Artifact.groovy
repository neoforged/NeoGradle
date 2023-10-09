package net.neoforged.gradle.dsl.platform.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.util.PropertyUtils
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.lang.reflect.Type

abstract class Artifact extends FileReference<Artifact> {

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getPath();

    @CompileStatic
    static class Serializer extends FileReference.Serializer<Artifact> {

        Serializer(ObjectFactory factory) {
            super(factory, Artifact.class)
        }

        @Override
        Artifact deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            def result = super.deserialize(jsonElement, type, jsonDeserializationContext) as Artifact;

            PropertyUtils.deserializeString(result.getPath(), jsonElement.getAsJsonObject(), "path")

            return result;
        }

        @Override
        JsonObject serialize(Artifact artifact, Type type, JsonSerializationContext jsonSerializationContext) {
            def object = super.serialize(artifact, type, jsonSerializationContext)

            PropertyUtils.serializeString(artifact.getPath(), object, "path")

            return object;
        }
    }
}
