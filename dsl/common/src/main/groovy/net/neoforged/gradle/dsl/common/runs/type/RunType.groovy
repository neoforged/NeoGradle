package net.neoforged.gradle.dsl.common.runs.type

import com.google.gson.*
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.NamedDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Optional

import javax.inject.Inject
import java.lang.reflect.Type

import static net.neoforged.gradle.dsl.common.util.PropertyUtils.*

/**
 * Defines an object which holds the type of run.
 * This is normally loaded from an userdev artifact.
 * <p>
 * However, for pure vanilla these objects are created in memory specifically for the run.
 */
@CompileStatic
abstract class RunType implements ConfigurableDSLElement<RunType>, NamedDSLElement, Named {

    private final String name

    @Inject
    RunType(String name) {
        this.name = name

        getIsSingleInstance().convention(getIsServer().zip(getIsDataGenerator().zip(getIsGameTest(), RunType::or), RunType::or))
        getIsClient().convention(true)
        getIsServer().convention(false)
        getIsDataGenerator().convention(false)
        getIsGameTest().convention(false)
    }

    private static Boolean or(Boolean a, Boolean b) {
        return a || b
    }

    @Override
    String getName() {
        return name
    }

    /**
     * Indicates if the run type is only allowed to run once at a time.
     *
     * @return The property which indicates if this is a single instance run type.
     */
    @DSLProperty
    abstract Property<Boolean> getIsSingleInstance();

    /**
     * Gives access to the name of the main class on the run type.
     *
     * @return The property which holds the main class name.
     */
    @DSLProperty
    abstract Property<String> getMainClass();

    /**
     * Gives access to the application arguments for the run type.
     *
     * @return The property which holds the application arguments.
     */
    @DSLProperty
    abstract ListProperty<String> getArguments();

    void arg(final String value) {
        arguments.add(value)
    }

    void arg(final Provider<String> value) {
        arguments.add(value)
    }

    /**
     * Gives access to the JVM arguments for the run type.
     *
     * @return The property which holds the JVM arguments.
     */
    @DSLProperty
    abstract ListProperty<String> getJvmArguments();

    void jvmArg(final String value) {
        jvmArguments.add(value)
    }

    void jvmArg(final Provider<String> value) {
        jvmArguments.add(value)
    }

    /**
     * Indicates if this run type is for the client.
     *
     * @return The property which indicates if this is a client run type.
     */
    @DSLProperty
    abstract Property<Boolean> getIsClient();

    void client() {
        isClient.set(true)
    }

    /**
     * Indicates if this run is a server run.
     *
     * @return {@code true} if this run is a server run; otherwise, {@code false}.
     */
    @DSLProperty
    abstract Property<Boolean> getIsServer();

    void server() {
        getIsServer().set(true)
    }

    /**
     * Indicates if this run is a data generation run.
     *
     * @return {@code true} if this run is a data generation run; otherwise, {@code false}.
     */
    @DSLProperty
    abstract Property<Boolean> getIsDataGenerator();

    void dataGenerator() {
        getIsDataGenerator().set(true)
    }

    /**
     * Indicates if this run is a game test run.
     *
     * @return {@code true} if this run is a game test run; otherwise, {@code false}.
     */
    @DSLProperty
    abstract Property<Boolean> getIsGameTest();

    void gameTest() {
        getIsGameTest().set(true)
        getSystemProperties().put("forge.enableGameTest", "true")
    }

    /**
     * Gives access to the key value pairs which are added as environment variables when an instance of this run type is executed.
     *
     * @return The property which holds the environment variables.
     */
    @DSLProperty
    abstract MapProperty<String, String> getEnvironmentVariables();

    void env(final String name, final String value) {
        environmentVariables.put(name, value)
    }

    void env(final String name, final Provider<String> value) {
        environmentVariables.put(name, value)
    }

    /**
     * Gives access to the key value pairs which are added as system properties when an instance of this run type is executed.
     *
     * @return The property which holds the system properties.
     */
    @DSLProperty
    abstract MapProperty<String, String> getSystemProperties();

    void sysProp(final String name, final String value) {
        systemProperties.put(name, value)
    }

    void sysProp(final String name, final Provider<String> value) {
        systemProperties.put(name, value)
    }

    /**
     * Gives access to the classpath for this run type.
     * Does not contain the full classpath since that is dependent on the actual run environment, but contains the additional classpath elements
     * needed to run the game with this run type.
     *
     * @return The property which holds the classpath.
     */
    @DSLProperty
    abstract ConfigurableFileCollection getClasspath();

    /**
     * An optional configurable run adapter which can be used to change the behaviour of already configured runs when the type is applied to them.
     *
     * @return The run adapter.
     */
    @DSLProperty
    @Optional
    abstract Property<RunAdapter> getRunAdapter();

    /**
     * Copies this run type into a new instance.
     *
     * @param other The other run type to copy into.
     */
    void copyTo(RunType other) {
        other.getIsSingleInstance().set(getIsSingleInstance())
        other.getMainClass().set(getMainClass())
        other.getArguments().set(getArguments())
        other.getJvmArguments().set(getJvmArguments())
        other.getIsClient().set(getIsClient())
        other.getEnvironmentVariables().set(getEnvironmentVariables())
        other.getSystemProperties().set(getSystemProperties())
        other.getClasspath().from(getClasspath())
    }

    /**
     * Copies the data from the given other type into this type.
     *
     * @param other The type to copy from.
     */
    void from(RunType other) {
        other.copyTo(this)
    }

    @CompileStatic
    static class Serializer implements JsonSerializer<RunType>, JsonDeserializer<RunType> {

        private final ObjectFactory objectFactory

        Serializer(ObjectFactory objectFactory) {
            this.objectFactory = objectFactory
        }

        @Override
        RunType deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (!jsonElement.isJsonObject()) {
                throw new JsonParseException("Expected a JSON object, but got " + jsonElement)
            }

            final JsonObject object = jsonElement.getAsJsonObject()
            if (!object.has("name")) {
                throw new JsonParseException("Expected a 'name' property, but got " + object)
            }

            final String name = object.get("name").getAsString()

            return deserializeNamed(objectFactory, name, object, jsonDeserializationContext)
        }

        static RunType deserializeNamed(ObjectFactory objectFactory, String name, JsonObject object, JsonDeserializationContext jsonDeserializationContext) {
            final RunType instance = objectFactory.newInstance(RunType.class, name)

            deserializeBool(instance.isSingleInstance, object, "singleInstance")
            deserializeString(instance.mainClass, object, "main")
            deserializeList(instance.arguments, object, "args", String.class, jsonDeserializationContext)
            deserializeList(instance.jvmArguments, object, "jvmArgs", String.class, jsonDeserializationContext)
            deserializeBool(instance.isClient, object, "client")
            deserializeBool(instance.isServer, object, "server")
            deserializeBool(instance.isDataGenerator, object, "dataGenerator")
            deserializeBool(instance.isGameTest, object, "gameTest")
            deserializeMap(instance.environmentVariables, object, "env", String.class, jsonDeserializationContext)
            deserializeMap(instance.systemProperties, object, "props", String.class, jsonDeserializationContext)

            return instance
        }

        @Override
        JsonElement serialize(RunType runType, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject object = serializedNamed(runType, jsonSerializationContext)
            object.addProperty("name", runType.getName())
            return object
        }

        static JsonObject serializedNamed(RunType runType, JsonSerializationContext jsonSerializationContext) {
            final JsonObject object = new JsonObject()

            serializeBool(runType.isSingleInstance, object, "singleInstance")
            serializeString(runType.mainClass, object, "main")
            serializeList(runType.arguments, object, "args", jsonSerializationContext)
            serializeList(runType.jvmArguments, object, "jvmArgs", jsonSerializationContext)
            serializeBool(runType.isClient, object, "client")
            serializeBool(runType.isServer, object, "server")
            serializeBool(runType.isDataGenerator, object, "dataGenerator")
            serializeBool(runType.isGameTest, object, "gameTest")
            serializeMap(runType.environmentVariables, object, "env", jsonSerializationContext)
            serializeMap(runType.systemProperties, object, "props", jsonSerializationContext)

            return object
        }
    }
}
