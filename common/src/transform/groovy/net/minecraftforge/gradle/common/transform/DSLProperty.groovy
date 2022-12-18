package net.minecraftforge.gradle.common.transform

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass('net.minecraftforge.gradle.common.transform.DSLPropertyTransformer')
@interface DSLProperty {
    String propertyName() default ''

    Class<Closure> factory() default Closure.class

    boolean isConfigurable() default true
}
