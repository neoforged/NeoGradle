package net.minecraftforge.gradle.legacy.util;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.extensibility.ExtensionsStorage;

import java.util.Map;

public final class ExtensionUtils {

    private ExtensionUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ExtensionUtils. This is a utility class");
    }

    @SuppressWarnings("unchecked")
    public static void removeExtension(Project extensionAware, String name) {
        final ExtensionContainer container = extensionAware.getExtensions();
        final ExtensionsStorage extensionsStorage = (ExtensionsStorage) ReflectionUtils.getFieldContent(container, "extensionsStorage");
        final Map<String, Object> extensions = (Map<String, Object>) ReflectionUtils.getFieldContent(extensionsStorage, "extensions");

        extensionAware.getLogger().lifecycle("Current extensions: " + extensions.keySet());

        extensions.remove(name);

        extensionAware.getLogger().lifecycle("Removed extension: " + extensions.keySet());
    }
}
