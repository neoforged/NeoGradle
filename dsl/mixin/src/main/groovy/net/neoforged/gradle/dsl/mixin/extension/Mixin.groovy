package net.neoforged.gradle.dsl.mixin.extension

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

@CompileStatic
interface Mixin extends BaseDSLElement<Mixin> {
    String EXTENSION_NAME = "mixin";

    @DSLProperty
    @Optional
    @Input
    SetProperty<String> getConfigs();
}