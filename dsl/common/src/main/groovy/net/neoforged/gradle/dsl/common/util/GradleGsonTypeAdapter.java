package net.neoforged.gradle.dsl.common.util;

import com.google.gson.*;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import java.util.List;
import java.util.Map;

public abstract class GradleGsonTypeAdapter<T> implements JsonDeserializer<T>, JsonSerializer<T> {

    @SuppressWarnings("unchecked")
    protected static <T> void deserializeProperty(final JsonObject json, final JsonDeserializationContext context, final Property<T> property, final String key, final T defaultValue) {
        final JsonElement value = json.getAsJsonObject().get(key);
        if (value != null) {
            property.set((T) context.deserialize(value, defaultValue.getClass()));
        } else {
            property.set(defaultValue);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> void deserializeProperty(final JsonObject json, final JsonDeserializationContext context, final ListProperty<T> property, final String key, final List<T> defaultValue) {
        final JsonElement value = json.getAsJsonObject().get(key);
        if (value != null) {
            property.set((List<T>) context.deserialize(value, defaultValue.getClass()));
        } else {
            property.set(defaultValue);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Map<? extends K, ? extends V>, K, V> void deserializeProperty(final JsonObject json, final JsonDeserializationContext context, final MapProperty<K, V> property, final String key, final T defaultValue) {
        final JsonElement value = json.getAsJsonObject().get(key);
        if (value != null) {
            property.set((T) context.deserialize(value, defaultValue.getClass()));
        } else {
            property.set(defaultValue);
        }
    }

    protected <E> void serializeProperty(JsonObject json, JsonSerializationContext context, Property<E> property, String key) {
        json.add(key, context.serialize(property.get()));
    }

    protected <E> void serializeProperty(JsonObject json, JsonSerializationContext context, ListProperty<E> property, String key) {
        json.add(key, context.serialize(property.get()));
    }


    protected <K, V> void serializeProperty(JsonObject json, JsonSerializationContext context, MapProperty<K, V> property, String key) {
        json.add(key, context.serialize(property.get()));
    }
}
