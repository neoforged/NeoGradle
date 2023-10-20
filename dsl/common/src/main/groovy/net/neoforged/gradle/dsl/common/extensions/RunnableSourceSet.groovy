package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
abstract class RunnableSourceSet implements BaseDSLElement<RunnableSourceSet> {

    public static final String NAME = "runs"

    private final Project project;
    private final Property<String> modIdentifier;

    @Inject
    RunnableSourceSet(final Project project) {
        super()
        this.project = project;
        this.modIdentifier = project.getObjects().property(String.class)

        getModIdentifier().convention(
                project.getExtensions().getByType(Minecraft.class).getModIdentifier()
        )
    }

    @Override
    Project getProject() {
        return project;
    }

    @DSLProperty
    Property<String> getModIdentifier() {
        return modIdentifier;
    }
}
