package net.minecraftforge.gradle.dsl.annotations

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass('net.minecraftforge.gradle.dsl.generator.transform.BouncerMethodTransformer')
@interface BouncerMethod {
    Class<?> returnType()
}