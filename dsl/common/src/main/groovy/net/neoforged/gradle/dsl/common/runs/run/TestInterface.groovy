package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.neoforged.gdi.BaseDSLElement
import net.neoforged.gdi.NamedDSLElement
import net.neoforged.gdi.annotations.DSLProperty
import net.neoforged.gdi.annotations.DefaultMethods
import net.neoforged.gradle.dsl.common.runs.RunSpecification
import org.gradle.api.tasks.Nested

@DefaultMethods
@CompileStatic
interface TestInterface {

    @Nested
    @DSLProperty
    abstract RunRenderDocOptions getRenderDocAsTestTarget();
}