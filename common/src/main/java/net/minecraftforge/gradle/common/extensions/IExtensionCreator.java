package net.minecraftforge.gradle.common.extensions;

import org.gradle.api.Project;

import java.util.function.Function;

/**
 * Custom function definition for creating an extension.
 *
 * @param <T> The type of the extension
 */
public interface IExtensionCreator<T> extends Function<Project, T> {
}
