package net.neoforged.gradle.dsl.mixin.extension

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@CompileStatic
interface Mixin extends BaseDSLElement<Mixin> {
    String EXTENSION_NAME = "mixin";

    @DSLProperty
    SetProperty<String> getConfigs();

    @DSLProperty
    Property<Boolean> getDisableTargetValidator();

    @DSLProperty
    Property<Boolean> getDisableTargetExport();

    @DSLProperty
    Property<Boolean> getDisableOverwriteChecker();

    @DSLProperty
    Property<Boolean> getQuiet();

    @DSLProperty
    Property<Boolean> getShowMessageTypes();
}