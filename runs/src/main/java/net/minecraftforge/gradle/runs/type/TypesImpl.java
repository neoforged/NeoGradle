package net.minecraftforge.gradle.runs.type;

import net.minecraftforge.gradle.common.util.ConfigurableNamedDSLObjectContainer;
import net.minecraftforge.gradle.dsl.runs.type.Type;
import net.minecraftforge.gradle.dsl.runs.type.Types;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class TypesImpl extends ConfigurableNamedDSLObjectContainer<Types, Type> implements Types {

    @Inject
    public TypesImpl(Project project) {
        super(project, Type.class, name -> project.getObjects().newInstance(TypeImpl.class, project, name));
    }
}
