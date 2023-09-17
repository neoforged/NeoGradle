package net.neoforged.gradle.platform.model;

import net.minecraftforge.gdi.BaseDSLElement;
import org.gradle.api.provider.Property;

public abstract class Argument implements BaseDSLElement<Argument> {


    public abstract Property<String> getValue();

    public abstract Property<OsCondition> getOS();
}
