package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CompileStatic
interface RunDependency extends BaseDSLElement<RunDependency> {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @DSLProperty
    ConfigurableFileCollection getDependency();

    @Input
    @DSLProperty
    Property<String> getIdentity();
}
