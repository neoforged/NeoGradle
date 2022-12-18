package net.minecraftforge.gradle.common.transform

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode

interface PropertyHandler {
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils)
}