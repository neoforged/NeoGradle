package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
abstract class RunnableSourceSet {

    public static final String NAME = "runs"

    RunnableSourceSet() {
        super()
        getModIdentifier().convention(
                getProject().getExtensions().getByType(Minecraft.class).getModIdentifier()
        )
    }

    @Inject
    @ProjectGetter
    abstract Project getProject();

    @DSLProperty
    abstract Property<String> getModIdentifier();
}
