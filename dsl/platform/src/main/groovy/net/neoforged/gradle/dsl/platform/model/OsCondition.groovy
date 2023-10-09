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

@CompileStatic
abstract class OsCondition implements ConfigurableDSLElement<OsCondition> {

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getName();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getArch();

    @Input
    @DSLProperty
    @Optional
    abstract Property<String> getVersion();

    @CompileStatic
    static class Serializer implements JsonSerializer<OsCondition>, JsonDeserializer<OsCondition> {

        private final ObjectFactory factory;

        Serializer(ObjectFactory factory) {
            this.factory = factory
        }

        @Override
        OsCondition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("OS condition must be a json object")

            final JsonObject payload = jsonElement.getAsJsonObject();
            final OsCondition instance = factory.newInstance(OsCondition.class);

            PropertyUtils.deserializeString(instance.getName(), payload, "name")
            PropertyUtils.deserializeString(instance.getArch(), payload, "arch")
            PropertyUtils.deserializeString(instance.getVersion(), payload, "version")

            return instance;
        }

        @Override
        JsonElement serialize(OsCondition osCondition, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject result = new JsonObject();

            PropertyUtils.serializeString(osCondition.getName(), result, "name")
            PropertyUtils.serializeString(osCondition.getArch(), result, "arch")
            PropertyUtils.serializeString(osCondition.getVersion(), result, "version")

            return result;
        }
    }
}
