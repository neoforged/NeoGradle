package net.neoforged.gradle.dsl.common.util


import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.reflect.TypeToken
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

import java.lang.reflect.Type
import java.util.function.BiFunction
import java.util.function.BooleanSupplier
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

@CompileStatic
class PropertyUtils {

    static <T> void setIf(final Property<T> property, Supplier<T> supplier, BooleanSupplier predicate) {
        if (!predicate.asBoolean)
            return

        property.set(supplier.get())
    }

    static <T> void setIf(final ListProperty<T> property, Supplier<List<T>> supplier, BooleanSupplier predicate) {
        if (!predicate.asBoolean)
            return

        property.set(supplier.get())
    }

    static <T> void setIf(final SetProperty<T> property, Supplier<Set<T>> supplier, BooleanSupplier predicate) {
        if (!predicate.asBoolean)
            return

        property.set(supplier.get())
    }


    static <K, V> void setIf(final MapProperty<K, V> property, Supplier<Map<K, V>> supplier, BooleanSupplier predicate) {
        if (!predicate.asBoolean)
            return

        property.set(supplier.get())
    }

    static <V> void setIf(final NamedDomainObjectCollection<V> property, Supplier<List<V>> supplier, BooleanSupplier predicate) {
        if (!predicate.asBoolean)
            return

        property.addAll(supplier.get())
    }

    static <T> void deserialize(final Property<T> property, JsonObject object, String key, Function<JsonElement, T> parser) {
        setIf(
                property,
                () -> parser.apply(object.get(key)),
                () -> object.has(key)
        )
    }

    static <T> void deserialize(final ListProperty<T> property, JsonObject object, String key, Function<JsonElement, List<T>> parser) {
        setIf(
                property,
                () -> parser.apply(object.get(key)),
                () -> object.has(key)
        )
    }

    static <T> void deserialize(final SetProperty<T> property, JsonObject object, String key, Function<JsonElement, Set<T>> parser) {
        setIf(
                property,
                () -> parser.apply(object.get(key)),
                () -> object.has(key)
        )
    }

    static <K, V> void deserializeMap(final MapProperty<K, V> property, JsonObject object, String key, BiFunction<String, JsonElement, V> parser, Function<V, K> keyExtractor) {
        setIf(
                property,
                () -> {
                    JsonObject map = object.get(key).getAsJsonObject()
                    map.entrySet().stream()
                            .map {entry -> parser.apply(entry.key, entry.value)}
                            .collect(Collectors.toMap(
                            (V entry) -> keyExtractor.apply(entry),
                            (V entry) -> entry
                    ))
                },
                () -> object.has(key)
        )
    }


    static <K, I, V> void deserializeMap(final MapProperty<K, V> property, JsonObject object, String key, BiFunction<String, JsonElement, I> parser, Function<I, V> finalizer, Function<I, K> keyExtractor) {
        setIf(
                property,
                () -> {
                    JsonObject map = object.get(key).getAsJsonObject()
                    map.entrySet().stream()
                            .map {entry -> parser.apply(entry.key, entry.value)}
                            .collect(Collectors.toMap(
                                    (I entry) -> keyExtractor.apply(entry),
                                    (I entry) -> finalizer.apply(entry)
                            ))
                },
                () -> object.has(key)
        )
    }


    static <I, V> void deserializeNamedDomainCollection(final NamedDomainObjectCollection<V> property, JsonObject object, String key, BiFunction<String, JsonElement, I> parser, Function<I, V> finalizer, Function<I, String> keyExtractor) {
        setIf(
                property,
                () -> {
                    JsonObject map = object.get(key).getAsJsonObject()
                    map.entrySet().stream()
                            .map {entry -> parser.apply(entry.key, entry.value)}
                            .collect(Collectors.toMap(
                                    (I entry) -> keyExtractor.apply(entry),
                                    (I entry) -> finalizer.apply(entry)
                            ))
                },
                () -> object.has(key)
        )
    }

    static void deserializeString(final Property<String> property, JsonObject object, String key) {
        deserialize(
                property,
                object,
                key,
                element -> element.asString
        )
    }

    static void deserializeBool(final Property<Boolean> property, JsonObject object, String key) {
        deserialize(
                property,
                object,
                key,
                element -> element.asBoolean
        )
    }

    static void deserializeInt(final Property<Integer> property, JsonObject object, String key) {
        deserialize(
                property,
                object,
                key,
                element -> element.asInt
        )
    }

    static void deserializeLong(final Property<Long> property, JsonObject object, String key) {
        deserialize(
                property,
                object,
                key,
                element -> element.asLong
        )
    }

    static <T> void deserialize(final Property<T> property, JsonObject object, String key, Class<T> type, JsonDeserializationContext context) {
        deserialize(
                property,
                object,
                key,
                element -> (T) context.deserialize(element, type)
        )
    }

    static <T> void deserializeList(final ListProperty<T> property, JsonObject object, String key, Type listElementType, JsonDeserializationContext context) {
        final TypeToken<List<T>> typeToken = TypeToken.getParameterized(List.class, listElementType) as TypeToken<List<T>>;

        deserialize(
                property,
                object,
                key,
                element -> (List<T>) context.deserialize(element, typeToken.getType())
        )
    }

    static <T> void deserializeSet(final SetProperty<T> property, JsonObject object, String key, Type setElementType, JsonDeserializationContext context) {
        final TypeToken<List<T>> typeToken = TypeToken.getParameterized(List.class, setElementType) as TypeToken<List<T>>;

        deserialize(
                property,
                object,
                key,
                element -> (Set<T>) context.deserialize(element, typeToken.getType())
        )
    }

    static <V> void deserializeMap(final MapProperty<String, V> property, JsonObject object, String key, BiFunction<String, JsonElement, V> parser) {
        deserializeMap(
                property,
                object,
                key,
                (name, element) -> {
                   return Tuple.<String, V>tuple(name, parser.apply(name, element))
                },
                (Tuple2<String, V> tuple) -> {
                    return tuple.v2;
                },
                (Tuple2<String, V> tuple) -> tuple.v1
        )
    }



    static <V> void deserializeNamedDomainCollection(final NamedDomainObjectCollection<V> property, JsonObject object, String key, BiFunction<String, JsonElement, V> parser) {
        deserializeNamedDomainCollection(
                property,
                object,
                key,
                (name, element) -> {
                    return Tuple.<String, V>tuple(name, parser.apply(name, element))
                },
                (Tuple2<String, V> tuple) -> {
                    return tuple.v2;
                },
                (Tuple2<String, V> tuple) -> tuple.v1
        )
    }

    static <V> void deserializeMap(final MapProperty<String, V> property, JsonObject object, String key, Function<JsonElement, V> parser) {
        deserializeMap(
                property,
                object,
                key,
                (name, element) -> {
                    return Tuple.<String, V>tuple(name, parser.apply(element))
                },
                (Tuple2<String, V> tuple) -> {
                    return tuple.v2;
                },
                (Tuple2<String, V> tuple) -> tuple.v1
        )
    }

    static <V> void deserializeMap(final MapProperty<String, V> property, JsonObject object, String key, Type valueType, JsonDeserializationContext context) {
        deserializeMap(
                property,
                object,
                key,
                (name, element) -> {
                    return Tuple.<String, V>tuple(name, (V) context.deserialize(element, valueType))
                },
                (Tuple2<String, V> tuple) -> {
                    return tuple.v2;
                },
                (Tuple2<String, V> tuple) -> tuple.v1
        )
    }


    static <T> void deserializeNamedMap(final MapProperty<String, T> property, JsonObject object, String key, Type valueType, JsonDeserializationContext context, Function<T, Property<String>> nameExtractor) {
        deserializeMap(
                property,
                object,
                key,
                (name, element) -> {
                    return Tuple.<String, T>tuple(name, (T) context.deserialize(element, valueType))
                },
                (Tuple2<String, T> tuple) -> {
                    def nameProp = nameExtractor.apply(tuple.v2)
                    nameProp.set(tuple.v1)
                },
                (Tuple2<String, T> tuple) -> tuple.v1
        )
    }

    static <T> void serialize(final Property<T> property, JsonObject object, String key, Function<T, JsonElement> writer) {
        if (property.isPresent()) {
            object.add(key, writer.apply(property.get()))
        }
    }

    static <T> void serialize(final Property<T> property, JsonObject object, String key, JsonSerializationContext context) {
        serialize(property, object, key, new Function<T, JsonElement>() {
            @Override
            JsonElement apply(T t) {
                return context.serialize(t)
            }
        })
    }

    static <T> void serialize(final ListProperty<T> property, JsonObject object, String key, Function<List<T>, JsonElement> writer) {
        if (property.isPresent() && !property.get().isEmpty()) {
            object.add(key, writer.apply(property.get()))
        }
    }

    static <T> void serialize(final SetProperty<T> property, JsonObject object, String key, Function<Set<T>, JsonElement> writer) {
        if (property.isPresent() && !property.get().isEmpty()) {
            object.add(key, writer.apply(property.get()))
        }
    }

    static <K, V> void serialize(final MapProperty<K, V> property, JsonObject object, String key, Function<Map<K, V>, JsonElement> writer) {
        if (property.isPresent() && !property.get().isEmpty()) {
            object.add(key, writer.apply(property.get()))
        }
    }

    static <V> void serialize(final NamedDomainObjectCollection<V> property, JsonObject object, String key, Function<List<V>, JsonElement> writer) {
        if (!property.isEmpty()) {
            object.add(key, writer.apply(property.asList()))
        }
    }

    static void serializeString(final Property<String> property, JsonObject object, String key) {
        serialize(property, object, key, value -> new JsonPrimitive(value))
    }

    static void serializeInt(final Property<Integer> property, JsonObject object, String key) {
        serialize(property, object, key, value -> new JsonPrimitive(value))
    }

    static void serializeLong(final Property<Long> property, JsonObject object, String key) {
        serialize(property, object, key, value -> new JsonPrimitive(value))
    }

    static void serializeBool(final Property<Boolean> property, JsonObject object, String key) {
        serialize(property, object, key, value -> new JsonPrimitive(value))
    }

    static <T> void serializeObject(final Property<T> property, JsonObject object, String key, JsonSerializationContext context) {
        serialize(property, object, key, value -> context.serialize(value))
    }

    static <T> void serializeList(final ListProperty<T> property, JsonObject object, String key, JsonSerializationContext context) {
        serialize(property, object, key, value -> context.serialize(value))
    }

    static <T> void serializeSet(final SetProperty<T> property, JsonObject object, String key, JsonSerializationContext context) {
        serialize(property, object, key, value -> context.serialize(value))
    }

    static <K, V> void serializeMap(final MapProperty<K, V> property, JsonObject object, String key, Function<K, String> keyWriter, Function<V, JsonElement> valueWriter) {
        serialize(
                property,
                object,
                key,
                (Map<K, V> map) -> {
                    def result = new JsonObject()

                    map.forEach { k, v ->
                        result.add(keyWriter.apply(k), valueWriter.apply(v))
                    }

                    return result
                }
        )
    }

    static <V> void serializeNamedDomainCollection(final NamedDomainObjectCollection<V> property, JsonObject object, String key, Function<V, JsonElement> valueWriter) {
        serialize(
                property,
                object,
                key,
                (List<V> map) -> {
                    def result = new JsonObject()

                    map.forEach { v ->
                        result.add(property.namer.determineName(v), valueWriter.apply(v))
                    }

                    return result
                }
        )
    }

    static <V> void serializeMap(final MapProperty<String, V> property, JsonObject object, String key, Function<V, JsonElement> valueWriter) {
        serialize(
                property,
                object,
                key,
                (Map<String, V> map) -> {
                    def result = new JsonObject()

                    map.forEach { k, v ->
                        result.add(k, valueWriter.apply(v))
                    }

                    return result
                }
        )
    }

    static <V> void serializeMap(final MapProperty<String, V> property, JsonObject object, String key, JsonSerializationContext context) {
        serialize(
                property,
                object,
                key,
                (Map<String, V> map) -> {
                    def result = new JsonObject()

                    map.forEach { k, v ->
                        result.add(k, context.serialize(v))
                    }

                    return result
                }
        )
    }
}
