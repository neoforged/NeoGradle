package net.minecraftforge.gradle.dsl.generator.transform.property

import net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode

interface PropertyHandler {
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils)
}