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
import org.codehaus.groovy.transform.sc.transformers.StaticCompilationTransformer

import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class DefaultMethodsTransformer extends AbstractASTTransformation implements Opcodes {
    private static final ClassNode TRAIT = ClassHelper.make(Trait)
    private static final ClassNode CSTATIC = ClassHelper.make(CompileStatic)
    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        this.init(astNodes, sourceUnit)
        final traitHelper = (InnerClassNode)astNodes[1]
        final clazz = traitHelper.outerClass
        new ArrayList<MethodNode>(clazz.methods).each {
            final method = traitHelper.methods.find { m -> m.name == it.name && m.parameters.drop(1) == it.parameters && m.parameters[0].type == clazz }
            if (method === null) return

            clazz.removeMethod(it)
            final Expression call = GeneralUtils.callX(traitHelper, it.name, GeneralUtils.args(
                    Stream.concat(
                            Stream.<Expression>of(VariableExpression.THIS_EXPRESSION),
                            Stream.of(it.parameters)
                                    .<Expression>map { GeneralUtils.varX(it) },
                    )
                        .collect(Collectors.toList())
            ))

            final mtd = clazz.addMethod(
                    it.name, ACC_PUBLIC,
                    it.returnType, it.parameters,
                    it.exceptions,
                    (Statement) (it.returnType == ClassHelper.VOID_TYPE ? GeneralUtils.stmt(call) : GeneralUtils.returnS(call))
            )
            final ann = new AnnotationNode(CSTATIC)
            mtd.addAnnotation(ann)
            new StaticCompileTransformation().visit(new ASTNode[] {
                ann, mtd
            }, sourceUnit)
        }

        clazz.annotations.removeIf { it.classNode == TRAIT }
    }
}
