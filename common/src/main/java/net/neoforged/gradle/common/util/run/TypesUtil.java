package net.neoforged.gradle.common.util.run;

import net.neoforged.gradle.dsl.common.runs.type.Type;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.*;

public class TypesUtil {
    
    private TypesUtil() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static NamedDomainObjectProvider<Type> registerWithPotentialPrefix(NamedDomainObjectContainer<Type> types, String prefix, String name, Action<? super Type> configurationAction) {
        String nameToRegister = name;
        if (types.getNames().contains(nameToRegister)) {
            nameToRegister = prefix + StringCapitalizationUtils.capitalize(name);
        }
        
        if (types.getNames().contains(nameToRegister)) {
            throw new InvalidUserDataException("There is already a type with the name " + nameToRegister);
        }
        
        return types.register(nameToRegister, configurationAction);
    }
}
