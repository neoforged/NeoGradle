package net.minecraftforge.gradle.common.util;

import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.plugins.ExtensionContainer;

import java.util.Collection;

public final class GradleInternalUtils {

    private GradleInternalUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GradleInternalUtils. This is a utility class");
    }

    public static Collection<Object> getExtensions(final ExtensionContainer container) {
        return ((ExtensionContainerInternal) container).getAsMap().values();
    }
}
