package net.minecraftforge.gradle.common.transform

import groovy.transform.CompileStatic
import groovy.transform.Generated
import groovy.transform.NamedParam
import groovy.transform.NamedVariant
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.gradle.api.Action
import org.gradle.api.provider.Property

import javax.annotation.Nullable

import static groovy.lang.Closure.*

@CompileStatic
@GroovyASTTransformation
class DSLPropertyTransformer extends AbstractASTTransformation {
    private static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property)
    private static final ClassNode CLOSURE_TYPE = ClassHelper.make(Closure)
    private static final ClassNode DELEGATES_TO_TYPE = ClassHelper.make(DelegatesTo)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)
        final methodNodeAST = nodes[1]
        if (methodNodeAST !instanceof MethodNode) throw new IllegalArgumentException('Unexpected non-method node!')
        final methodNode = (MethodNode) methodNodeAST
        if (!methodNode.abstract || !methodNode.public) throw new IllegalArgumentException('Methods annotated with DSLProperty can only be abstract and public!')
        visitMethod(methodNode, nodes[0] as AnnotationNode)
    }

    private static void visitMethod(MethodNode method, AnnotationNode annotation) {
        final propertyName = getPropertyName(method, annotation)
        generateDSLMethods(method, propertyName).each { method.declaringClass.addMethod(it) }
    }

    private static List<MethodNode> generateDSLMethods(MethodNode methodNode, String propertyName) {
        ClassNode type
        PropertyQuery propertyQuery
        if (GeneralUtils.isOrImplements(methodNode.returnType, PROPERTY_TYPE)) { // TODO handle maps and stuff properly
            type = methodNode.returnType.genericsTypes[0].type
            propertyQuery = fromProperty()
        } else {
            type = methodNode.returnType
            propertyQuery = fromGetter()
        }

        final List<MethodNode> methods = []

        methods.add(createMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC | ACC_FINAL,
                parameters: [new Parameter(
                        GenericsUtils.makeClassSafeWithGenerics(Action, type),
                        'action'
                )] as Parameter[],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.varX('action'),
                        'execute',
                        GeneralUtils.args(propertyQuery.query(methodNode))
                ))
        ))

        methods.add(createMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC | ACC_FINAL,
                parameters: [new Parameter(
                        GenericsUtils.makeClassSafe(Closure),
                        'closure'
                ).tap {
                    it.addAnnotation(new AnnotationNode(DELEGATES_TO_TYPE).tap {
                        it.addMember('value', GeneralUtils.classX(type))
                        it.addMember('strategy', GeneralUtils.constX(DELEGATE_FIRST))
                    })
                }] as Parameter[],
                code: {
                    final List<Expression> expr = []
                    final closure = GeneralUtils.varX('closure')

                    expr.add(GeneralUtils.callX(closure, 'setDelegate', GeneralUtils.args(propertyQuery.query(methodNode))))
                    expr.add(GeneralUtils.callX(closure, 'setResolutionStrategy', GeneralUtils.constX(DELEGATE_FIRST)))
                    expr.add(GeneralUtils.callX(closure, 'call', GeneralUtils.args(propertyQuery.query(methodNode))))

                    return GeneralUtils.block(expr.stream().map { GeneralUtils.stmt(it) }.toArray { new Statement[it] })
                }()
        ))

        return methods
    }

    private static String getPropertyName(MethodNode methodNode, AnnotationNode annotation) {
        return getMemberStringValue(annotation, 'propertyName', methodNode.name.substring('get'.size()).uncapitalize())
    }

    private static PropertyQuery fromProperty() {
        return new PropertyQuery() {
            @Override
            Expression query(MethodNode getterMethod) {
                return GeneralUtils.callX(GeneralUtils.callThisX(getterMethod.name), 'get')
            }
        }
    }

    private static PropertyQuery fromGetter() {
        return new PropertyQuery() {
            @Override
            Expression query(MethodNode getterMethod) {
                return GeneralUtils.callThisX(getterMethod.name)
            }
        }
    }

    static final AnnotationNode GENERATED_ANNOTATION = new AnnotationNode(ClassHelper.make(Generated))
    @NamedVariant
    private static MethodNode createMethod(@NamedParam(required = true) final String methodName,
                                           @NamedParam final int modifiers = ACC_PUBLIC,
                                           @NamedParam final ClassNode returnType = ClassHelper.VOID_TYPE,
                                           @NamedParam final Parameter[] parameters = Parameter.EMPTY_ARRAY,
                                           @NamedParam final ClassNode[] exceptions = ClassNode.EMPTY_ARRAY,
                                           @NamedParam final List<AnnotationNode> annotations = [GENERATED_ANNOTATION],
                                           @NamedParam final Statement code = new BlockStatement(),
                                           @NamedParam @Nullable final Closure<Statement> conditionalCode = null) {
        final MethodNode method = new MethodNode(methodName, modifiers, returnType, parameters, exceptions, conditionalCode?.call() ?: code)
        method.addAnnotations(annotations)
        return method
    }
}

@CompileStatic
interface PropertyQuery {
    Expression query(MethodNode getterMethod)
}
