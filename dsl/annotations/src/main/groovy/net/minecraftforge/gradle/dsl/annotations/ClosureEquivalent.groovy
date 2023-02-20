package net.minecraftforge.gradle.dsl.annotations

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotate a method with an {@link org.gradle.api.Action} parameter with this annotation in order to add
 * a delegate method with the same name and parameters but with a {@link Closure} parameter instead of the action.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass('net.minecraftforge.gradle.dsl.generator.transform.ClosureEquivalentTransformer')
@interface ClosureEquivalent {

}