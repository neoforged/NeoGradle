package net.minecraftforge.gradle.common

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.common.transform.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet

@CompileStatic
abstract class TestExtension {
    @DSLProperty
    abstract Property<SourceSet> getSourceSet()
}
