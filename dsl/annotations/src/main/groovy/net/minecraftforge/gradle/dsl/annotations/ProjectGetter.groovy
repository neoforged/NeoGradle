package net.minecraftforge.gradle.dsl.annotations

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotate a method returning an instance of {@link org.gradle.api.Project} with this annotation
 * in order to inform DSL properties in the class that this method should be used for accessing the project, when needed.
 * <br>
 * {@link org.gradle.api.file.DirectoryProperty DirectoryProperties} and {@link org.gradle.api.file.RegularFileProperty RegularFileProperties}
 * <strong>need</strong> to have a project getter defined for accessing files relative to the project ({@linkplain org.gradle.api.Project#file(java.lang.Object)}.
 *
 * Example:
 * <pre>
 *     {@code
 *      interface MyExtensionProperties {
 *          @ProjectGetter Project getProject()
 *          @DSLProperty RegularFileProperty getInput()
 *      }
 *     }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@interface ProjectGetter {
}