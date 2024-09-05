package net.neoforged.gradle.dsl.common.runs.type

import com.google.gson.*
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.NamedDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.runs.RunSpecification
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.Nullable

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
abstract class RunType implements ConfigurableDSLElement<RunType>, NamedDSLElement, Named, RunSpecification {

    private final String name

    private Run runTemplate;

    @Inject
    RunType(String name) {
        this.name = name

        getIsSingleInstance().convention(getIsServer().zip(getIsDataGenerator().zip(getIsGameTest(), RunType::or), RunType::or))
        getIsClient().convention(false)
        getIsServer().convention(false)
        getIsDataGenerator().convention(false)
        getIsGameTest().convention(false)
        getIsJUnit().convention(false)
    }

    private static Boolean or(Boolean a, Boolean b) {
        return a || b
    }

    @Override
    String getName() {
        return name
    }

    /**
     * A run template provides an ability for common projects that define their own run to define a template
     *
     * @return The run template
     */
    @Internal
    @DSLProperty
    Run getRunTemplate() {
        return runTemplate
    }

    void setRunTemplate(Run runTemplate) {
        this.runTemplate = runTemplate

        if (this.runTemplate != null) {
            runTemplate.getConfigureAutomatically().set(false)
            runTemplate.getConfigureFromDependencies().set(false)
            runTemplate.getConfigureFromTypeWithName().set(false)

            runTemplate.configure(this);

            runTemplate.isClient.set(isClient)
            runTemplate.isServer.set(isServer)
            runTemplate.isDataGenerator.set(isDataGenerator)
            runTemplate.isGameTest.set(isGameTest)
            runTemplate.isJUnit.set(isJUnit)
        }
    }

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
        other.getIsServer().set(getIsServer())
        other.getIsDataGenerator().set(getIsDataGenerator())
        other.getIsGameTest().set(getIsGameTest())
        other.getIsJUnit().set(getIsJUnit())
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
            deserializeBool(instance.isJUnit, object, "unitTest")
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
            serializeBool(runType.isJUnit, object, "unitTest")
            serializeMap(runType.environmentVariables, object, "env", jsonSerializationContext)
            serializeMap(runType.systemProperties, object, "props", jsonSerializationContext)

            return object
        }
    }
}
