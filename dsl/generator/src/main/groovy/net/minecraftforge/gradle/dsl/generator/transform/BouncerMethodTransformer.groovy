package net.minecraftforge.gradle.dsl.generator.transform

import groovy.transform.CompileDynamic
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

import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class BouncerMethodTransformer extends AbstractASTTransformation implements Opcodes {
    private static final ClassNode TRAIT = ClassHelper.make(Trait)
    private static final ClassNode CSTATIC = ClassHelper.make(CompileStatic)
    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        this.init(astNodes, sourceUnit)
        if (astNodes[1] !instanceof MethodNode) return
        final method = (MethodNode) astNodes[1]
        ClassNode clazz = method.declaringClass
        if (clazz instanceof InnerClassNode && clazz.name.endsWith('Trait$Helper')) {
            clazz = ((InnerClassNode) clazz).outerClass
        }

        // TODO - Use ASM to call the actual method
        final Expression call = GeneralUtils.callX(method.declaringClass, method.name, GeneralUtils.args(
                    Stream.of(method.parameters)
                            .<Expression>map { it.name == '$self' ? VariableExpression.THIS_EXPRESSION : GeneralUtils.varX(it) }
                        .collect(Collectors.toList())
        ))

        final mtd = new MethodNode(
                method.name, ACC_BRIDGE | ACC_SYNTHETIC | ACC_PUBLIC,
                getMemberClassValue((AnnotationNode)astNodes[0], 'returnType'),
                method.parameters.drop(1), method.exceptions,
                GeneralUtils.returnS(call)
        )

        clazz.addMethod(mtd)

        final ann = new AnnotationNode(CSTATIC)
        mtd.addAnnotation(ann)
        new StaticCompileTransformation().visit(new ASTNode[] {
                ann, mtd
        }, sourceUnit)
    }
}
