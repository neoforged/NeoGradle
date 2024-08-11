package net.neoforged.gradle.common.util.run;

import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.*;
import org.jetbrains.annotations.Nullable;

public class TypesUtil {
    
    private TypesUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }

    public static void registerWithPotentialPrefix(NamedDomainObjectContainer<RunType> runTypes, String prefix, String name, Action<? super RunType> configurationAction) {
        String nameToRegister = name;
        if (runTypes.getNames().contains(nameToRegister)) {
            nameToRegister = prefix + StringCapitalizationUtils.capitalize(name);
        }
        
        if (runTypes.getNames().contains(nameToRegister)) {
            throw new InvalidUserDataException("There is already a type with the name " + nameToRegister);
        }

        try {
            runTypes.register(nameToRegister, configurationAction);
        } catch (IllegalStateException ignored) {
            //This happens when we try tro register elements in a moment where the container is locked.
            //Usually when the user uses version catalogues, should not matter, however.
        }
    }
}
