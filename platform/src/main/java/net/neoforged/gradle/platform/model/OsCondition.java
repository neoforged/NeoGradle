package net.neoforged.gradle.platform.model;

import net.minecraftforge.gdi.BaseDSLElement;
import org.gradle.api.provider.Property;

public abstract class OsCondition implements BaseDSLElement<OsCondition> {

    public abstract Property<String> getName();

    public abstract Property<String> getArch();

    public abstract Property<String> getVersion();
}
