package net.neoforged.gradle.platform.model;

import net.minecraftforge.gdi.BaseDSLElement;
import org.gradle.api.provider.ListProperty;

public abstract class Arguments implements BaseDSLElement<Arguments> {

    public abstract ListProperty<String> getProgram();

    public abstract ListProperty<String> getJVM();
}
