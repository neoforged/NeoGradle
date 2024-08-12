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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.lang.reflect.Type
import java.util.regex.Pattern

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

    Provider<Boolean> isActive() {
        def nameMatches = name.map { n ->
            if (n == "windows") {
                return System.getProperty("os.name").toLowerCase().contains("win")
            } else if (n == "linux") {
                return System.getProperty("os.name").toLowerCase().contains("unix") || System.getProperty("os.name").toLowerCase().contains("linux")
            } else if (n == "osx") {
                return System.getProperty("os.name").toLowerCase().contains("mac")
            } else {
                return false
            }
        }.orElse(true)

        def versionMatches = version.map { v -> return Pattern.compile(v).matcher(System.getProperty("os.version")).find()
        }.orElse(true)

        def archMatches = arch.map { a -> return Pattern.compile(a).matcher(System.getProperty("os.arch")).find()
        }.orElse(true)

        return nameMatches.zip(versionMatches.zip(archMatches,
                { v, a -> v && a }),
                { n, va -> n && va })
    }

    @CompileStatic
    static class Serializer implements JsonSerializer<OsCondition>, JsonDeserializer<OsCondition> {

        private final ObjectFactory factory;

        Serializer(ObjectFactory factory) {
            this.factory = factory
        }

        @Override
        OsCondition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject()) throw new JsonParseException("OS condition must be a json object")

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
