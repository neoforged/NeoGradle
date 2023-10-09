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
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import java.lang.reflect.Type

@CompileStatic
abstract class WithRules<TSelf extends WithRules<TSelf>> implements ConfigurableDSLElement<TSelf> {

    @Input
    @Optional
    @DSLProperty
    abstract ListProperty<Rule> getRules();

    @Override
    int hashCode() {
        return Objects.hash(getRules());
    }

    @Override
    boolean equals(Object obj) {
        if (obj === this) return true;
        if (!(obj instanceof WithRules)) return false;
        final WithRules other = (WithRules) obj;
        return Objects.equals(getRules(), other.getRules());
    }

    @CompileStatic
    abstract static class Serializer<TResult extends WithRules<TResult>> implements JsonSerializer<TResult>, JsonDeserializer<TResult> {

        protected final ObjectFactory factory;
        private final Class<TResult> type;

        protected Serializer(ObjectFactory factory, Class<TResult> type) {
            this.factory = factory
            this.type = type
        }

        @Override
        TResult deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            final TResult result = factory.newInstance(this.type);

            PropertyUtils.deserializeList(result.getRules(), jsonElement.getAsJsonObject(), "rules", Rule.class, jsonDeserializationContext)

            return result;
        }

        @Override
        JsonElement serialize(TResult tResult, Type type, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject();

            PropertyUtils.serializeList(tResult.getRules(), object, "rules", jsonSerializationContext)

            return object;
        }
    }
}
