package net.neoforged.gradle.platform.model;

import net.minecraftforge.gdi.BaseDSLElement;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public abstract class Rule implements BaseDSLElement<Rule> {

    public abstract Property<String> getAction();

    public abstract MapProperty<String, Boolean> getFeatures();
}
