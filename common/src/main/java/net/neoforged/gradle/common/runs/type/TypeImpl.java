package net.neoforged.gradle.common.runs.type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.runs.type.Type;
import net.neoforged.gradle.dsl.common.util.GradleGsonTypeAdapter;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TypeImpl implements ConfigurableDSLElement<Type>, Type {

    private final Project project;
    private final String name;

    @Inject
    public TypeImpl(Project project, String name) {
        this.project = project;
        this.name = name;
        
        getIsSingleInstance().convention(true);
        getIsClient().convention(false);
        getIsServer().convention(false);
        getIsDataGenerator().convention(false);
        getIsGameTest().convention(false);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public abstract Property<Boolean> getIsSingleInstance();

    @Override
    public abstract Property<String> getMainClass();

    @Override
    public abstract ListProperty<String> getArguments();

    @Override
    public abstract ListProperty<String> getJvmArguments();

    @Override
    public abstract Property<Boolean> getIsClient();

    @Override
    public abstract MapProperty<String, String> getEnvironmentVariables();

    @Override
    public abstract ConfigurableFileCollection getClasspath();

    @Override
    public abstract MapProperty<String, String> getSystemProperties();

    @Override
    public void copyTo(Type other) {
        other.getIsSingleInstance().set(getIsSingleInstance());
        other.getMainClass().set(getMainClass());
        other.getArguments().set(getArguments());
        other.getJvmArguments().set(getJvmArguments());
        other.getIsClient().set(getIsClient());
        other.getEnvironmentVariables().set(getEnvironmentVariables());
        other.getSystemProperties().set(getSystemProperties());
        other.getClasspath().from(getClasspath());
    }
    
    @Override
    public void from(Type other) {
        other.copyTo(this);
    }
    
    public static final class Serializer extends GradleGsonTypeAdapter<Type> {

        public static Serializer scoped(final Project project) {
            return new Serializer(project);
        }

        private final Project project;

        private Serializer(Project project) {
            this.project = project;
        }

        @Override
        public Type deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject())
                throw new JsonParseException("Expected object, got " + json);

            JsonObject obj = json.getAsJsonObject();

            if (!obj.has("name"))
                throw new JsonParseException("Missing 'name' property");

            String name = obj.get("name").getAsString();

            final Type type = project.getObjects().newInstance(TypeImpl.class, project, name);

            deserializeProperty(obj, context, type.getIsSingleInstance(), "singleInstance", true);
            deserializeProperty(obj, context, type.getMainClass(), "main", "net.minecraft.client.main.Main");
            deserializeProperty(obj, context, type.getArguments(), "args", new ArrayList<>());
            deserializeProperty(obj, context, type.getJvmArguments(), "jvmArgs", new ArrayList<>());
            deserializeProperty(obj, context, type.getIsClient(), "client", true);
            deserializeProperty(obj, context, type.getEnvironmentVariables(), "env", new HashMap<>());
            deserializeProperty(obj, context, type.getSystemProperties(), "props", new HashMap<>());

            return type;
        }


        @Override
        public JsonElement serialize(Type src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();

            json.addProperty("name", src.getName());

            serializeProperty(json, context, src.getIsSingleInstance(), "singleInstance");
            serializeProperty(json, context, src.getMainClass(), "main");
            serializeProperty(json, context, src.getArguments(), "args");
            serializeProperty(json, context, src.getJvmArguments(), "jvmArgs");
            serializeProperty(json, context, src.getIsClient(), "client");
            serializeProperty(json, context, src.getEnvironmentVariables(), "env");
            serializeProperty(json, context, src.getSystemProperties(), "props");

            return json;
        }
    }
}
