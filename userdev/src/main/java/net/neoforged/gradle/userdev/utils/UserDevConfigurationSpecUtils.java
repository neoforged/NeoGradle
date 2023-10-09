package net.neoforged.gradle.userdev.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.neoforged.gradle.common.runs.type.RunTypeImpl;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileReader;

public final class UserDevConfigurationSpecUtils {

    private UserDevConfigurationSpecUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: UserDevConfigurationSpecUtils. This is a utility class");
    }


    public static UserDevConfigurationSpecV2 get(final Project project, final File userDevConfigFile) {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(RunType.class, new RunTypeImpl.Serializer(project.getObjects()))
                .create();

        try {
            return gson.fromJson(new FileReader(userDevConfigFile), UserDevConfigurationSpecV2.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to read the userdev configuration from: %s", userDevConfigFile.getAbsolutePath()), e);
        }
    }
}
