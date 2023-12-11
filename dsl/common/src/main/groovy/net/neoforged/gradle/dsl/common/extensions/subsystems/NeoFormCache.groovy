package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import groovy.transform.Internal
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional

import javax.inject.Inject

/**
 * Allows the shared cache for intermediate and final NeoForm results to be configured.
 */
@CompileStatic
interface NeoFormCache extends ConfigurableDSLElement<NeoFormCache> {

    @Internal
    @Optional
    @DSLProperty
    Property<Boolean> getEnabled();

    @Internal
    @Optional
    @DSLProperty
    DirectoryProperty getCacheDirectory();

    @ProjectGetter
    @Inject
    Project getProject();

}
