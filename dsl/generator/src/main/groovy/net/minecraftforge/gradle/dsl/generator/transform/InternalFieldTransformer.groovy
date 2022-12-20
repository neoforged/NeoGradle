//file:noinspection EnhancedGroovy.ASTTransformerShouldCallInit
package net.minecraftforge.gradle.dsl.generator.transform

import groovy.transform.CompileStatic
import groovy.transform.Trait
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.sc.StaticCompileTransformation
import org.gradle.api.tasks.Internal

import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class InternalFieldTransformer extends AbstractASTTransformation implements Opcodes {
    private static final ClassNode INTERNAL_TYPE = ClassHelper.make(Internal)
    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        this.init(astNodes, sourceUnit)
        if (astNodes[1] !instanceof ClassNode) return
        ClassNode clazz = (ClassNode) astNodes[1]
        if (clazz instanceof InnerClassNode && clazz.name.endsWith('Trait$Helper')) {
            clazz = ((InnerClassNode) clazz).outerClass
        }
        getMemberStringList((AnnotationNode) astNodes[0], 'fields').each {
            clazz.getGetterMethod('get' + it.capitalize())?.addAnnotation(new AnnotationNode(INTERNAL_TYPE))
        }
    }
}
