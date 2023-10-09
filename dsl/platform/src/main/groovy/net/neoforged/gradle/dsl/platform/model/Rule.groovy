package net.neoforged.gradle.dsl.platform.model

import com.google.gson.*
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.util.PropertyUtils
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import java.lang.reflect.Type
import java.util.function.BiFunction

@CompileStatic
abstract class Rule implements ConfigurableDSLElement<Rule> {

    @Input
    @DSLProperty
    @Optional
    abstract Property<RuleAction> getAction();

    @Nested
    @DSLProperty
    @Optional
    abstract Property<OsCondition> getOs();

    @Input
    @Optional
    @DSLProperty
    abstract MapProperty<String, Boolean> getFeatures();

    @CompileStatic
    static class Serializer implements JsonSerializer<Rule>, JsonDeserializer<Rule> {

        private final ObjectFactory factory;

        Serializer(ObjectFactory factory) {
            this.factory = factory
        }

        @Override
        Rule deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject())
                throw new JsonParseException("Rule must be a json object")

            final JsonObject payload = jsonElement.getAsJsonObject();
            final Rule instance = factory.newInstance(Rule.class);

            PropertyUtils.deserialize(instance.getAction(), payload, "action", RuleAction.class, jsonDeserializationContext)
            PropertyUtils.deserialize(instance.getOs(), payload, "os", OsCondition.class, jsonDeserializationContext)
            PropertyUtils.deserializeMap(instance.getFeatures(), payload, "features", new BiFunction<String, JsonElement, Boolean>() {
                @Override
                Boolean apply(String s, JsonElement element) {
                    return element.getAsBoolean();
                }
            })

            return instance;
        }

        @Override
        JsonElement serialize(Rule rule, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject result = new JsonObject();

            PropertyUtils.serializeObject(rule.getAction(), result, "action", jsonSerializationContext)
            PropertyUtils.serializeObject(rule.getOs(), result, "os", jsonSerializationContext)
            PropertyUtils.serializeMap(rule.getFeatures(), result, "features", key -> key, value -> new JsonPrimitive(value))

            return result;
        }
    }
}
