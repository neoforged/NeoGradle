package net.minecraftforge.gradle.common.runs.type;

import net.minecraftforge.gradle.common.util.DelegatingDomainObjectContainer;
import net.minecraftforge.gradle.dsl.common.runs.type.Type;
import net.minecraftforge.gradle.dsl.common.runs.type.Types;
import net.minecraftforge.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class TypesImpl extends DelegatingDomainObjectContainer<Type> implements NamedDomainObjectContainer<Type>, Types {

    @Inject
    public TypesImpl(final Project project) {
        super(project, Type.class, name -> project.getObjects().newInstance(TypeImpl.class, project, name));
    }

    @Override
    public NamedDomainObjectProvider<Type> registerWithPotentialPrefix(String prefix, String name, Action<? super Type> configurationAction) {
        String nameToRegister = name;
        if (getNames().contains(nameToRegister)) {
            nameToRegister = prefix + StringCapitalizationUtils.capitalize(name);
        }

        if (getNames().contains(nameToRegister)) {
            throw new InvalidUserDataException("There is already a type with the name " + nameToRegister);
        }

        return register(nameToRegister, configurationAction);
    }
}
