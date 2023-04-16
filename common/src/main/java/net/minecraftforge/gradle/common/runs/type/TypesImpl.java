package net.minecraftforge.gradle.common.runs.type;

import net.minecraftforge.gdi.NamedDSLElement;
import net.minecraftforge.gradle.dsl.common.runs.type.Type;
import net.minecraftforge.gradle.dsl.common.runs.type.Types;
import net.minecraftforge.gradle.util.StringCapitalizationUtils;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.reflect.Instantiator;

import javax.inject.Inject;

public abstract class TypesImpl extends AbstractNamedDomainObjectContainer<Type> implements NamedDomainObjectContainer<Type>, Types {

    private final Project project;

    @Inject
    public TypesImpl(final Project project) {
        super(Type.class,
                project.getObjects()::newInstance,
                NamedDSLElement::getName,
                CollectionCallbackActionDecorator.NOOP);

        this.project = project;
    }

    @Override
    protected Type doCreate(String name) {
        return project.getObjects().newInstance(TypeImpl.class, project, name);
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
