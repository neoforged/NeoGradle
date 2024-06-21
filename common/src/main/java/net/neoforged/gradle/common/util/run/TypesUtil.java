package net.neoforged.gradle.common.util.run;

import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.*;

public class TypesUtil {
    
    private TypesUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static NamedDomainObjectProvider<RunType> registerWithPotentialPrefix(NamedDomainObjectContainer<RunType> runTypes, String prefix, String name, Action<? super RunType> configurationAction) {
        String nameToRegister = name;
        if (runTypes.getNames().contains(nameToRegister)) {
            nameToRegister = prefix + StringCapitalizationUtils.capitalize(name);
        }
        
        if (runTypes.getNames().contains(nameToRegister)) {
            throw new InvalidUserDataException("There is already a type with the identifier " + nameToRegister);
        }
        
        return runTypes.register(nameToRegister, configurationAction);
    }
}
