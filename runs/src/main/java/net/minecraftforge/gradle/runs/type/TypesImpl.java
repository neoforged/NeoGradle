package net.minecraftforge.gradle.runs.type;

import net.minecraftforge.gradle.base.util.ConfigurableNamedDSLObjectContainer;
import net.minecraftforge.gradle.base.util.StringUtils;
import net.minecraftforge.gradle.dsl.runs.type.Type;
import net.minecraftforge.gradle.dsl.runs.type.Types;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class TypesImpl extends ConfigurableNamedDSLObjectContainer<Types, Type> implements Types {

    @Inject
    public TypesImpl(Project project) {
        super(project, Type.class, name -> project.getObjects().newInstance(TypeImpl.class, project, name));
    }

    @Override
    public NamedDomainObjectProvider<Type> registerWithPotentialPrefix(String prefix, String name, Action<? super Type> configurationAction) {
        String nameToRegister = name;
        if (getNames().contains(nameToRegister)) {
            nameToRegister = prefix + StringUtils.capitalize(name);
        }

        if (getNames().contains(nameToRegister)) {
            throw new InvalidUserDataException("There is already a type with the name " + nameToRegister);
        }

        return register(nameToRegister, configurationAction);
    }
}
