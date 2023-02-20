package net.minecraftforge.gradle.dsl.annotations

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotate a class with this interface in order to add the {@link org.gradle.api.tasks.Internal} annotation to
 * fields (including getters and setters) in the {@link InternalFields#fields() fields array}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass('net.minecraftforge.gradle.dsl.generator.transform.InternalFieldTransformer')
@interface InternalFields {
    String[] fields()
}