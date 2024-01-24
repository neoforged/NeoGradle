package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * A {@link org.gradle.api.tasks.SourceSet SourceSet} extension used for associating each source set with a mod identifier.
 */
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

    /**
     * Defines the mod identifier this source set belongs to.
     * <p>
     * Defaults to {@link Minecraft#getModIdentifier}.
     *
     * @return The property which holds the mod identifier this source set belongs to.
     */
    @DSLProperty
    Property<String> getModIdentifier() {
        return modIdentifier;
    }
}
