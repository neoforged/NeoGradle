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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.lang.reflect.Type

abstract class FileReference<TSelf extends FileReference<TSelf>> implements ConfigurableDSLElement<TSelf> {

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getSha1();

    @Input
    @DSLProperty
    @Optional
    abstract Property<Long> getSize();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getUrl();

    @CompileStatic
    abstract static class Serializer<TResult extends FileReference<TResult>> implements JsonSerializer<TResult>, JsonDeserializer<TResult> {

        private final ObjectFactory factory
        private final Class<TResult> type

        Serializer(ObjectFactory factory, Class<TResult> type) {
            this.factory = factory
            this.type = type
        }

        @Override
        TResult deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("File reference must be a json object")

            final JsonObject payload = jsonElement.getAsJsonObject();
            final TResult instance = factory.newInstance(this.type);

            PropertyUtils.deserializeString(instance.getSha1(), payload, "sha1")
            PropertyUtils.deserializeLong(instance.getSize(), payload, "size")
            PropertyUtils.deserializeString(instance.getUrl(), payload, "url")

            return instance;
        }

        @Override
        JsonObject serialize(TResult tResult, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject();

            PropertyUtils.serializeString(tResult.getSha1(), object, "sha1")
            PropertyUtils.serializeLong(tResult.getSize(), object, "size")
            PropertyUtils.serializeString(tResult.getUrl(), object, "url")

            return object;
        }
    }
}
