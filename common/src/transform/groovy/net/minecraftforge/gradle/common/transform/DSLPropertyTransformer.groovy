//file:noinspection UnnecessaryQualifiedReference
package net.minecraftforge.gradle.common.transform

import groovy.transform.CompileStatic
import groovy.transform.Generated
import groovy.transform.NamedParam
import groovy.transform.NamedVariant
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.annotation.Nullable
import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class DSLPropertyTransformer extends AbstractASTTransformation {
    private static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property)
    private static final ClassNode LIST_PROPERTY_TYPE = ClassHelper.make(ListProperty)
    private static final ClassNode DELEGATES_TO_TYPE = ClassHelper.make(DelegatesTo)

    private static final ClassNode RAW_GENERIC_CLOSURE = GenericsUtils.makeClassSafe(Closure)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)
        final methodNodeAST = nodes[1]
        if (methodNodeAST !instanceof MethodNode) throw new IllegalArgumentException('Unexpected non-method node!')
        final methodNode = (MethodNode) methodNodeAST
        if (!methodNode.public) throw new IllegalArgumentException('Methods annotated with DSLProperty can only be abstract and public!')
        visitMethod(methodNode, nodes[0] as AnnotationNode)
    }

    private void visitMethod(MethodNode method, AnnotationNode annotation) {
        final propertyName = getPropertyName(method, annotation)
        generateDSLMethods(method, annotation, propertyName).each {
            method.declaringClass.addMethod(it)
        }
    }

    private List<MethodNode> generateDSLMethods(MethodNode methodNode, AnnotationNode annotation, String propertyName) {
        if ((GeneralUtils.isOrImplements(methodNode.returnType, LIST_PROPERTY_TYPE))) {
            return generateListProperty(methodNode.returnType.genericsTypes[0].type, PropertyQuery.PROPERTY, methodNode, annotation, propertyName)
        } else if (GeneralUtils.isOrImplements(methodNode.returnType, PROPERTY_TYPE)) { // TODO handle maps and stuff properly
            return generateDirectProperty(methodNode.returnType.genericsTypes[0].type, PropertyQuery.PROPERTY, methodNode, annotation, propertyName)
        } else {
            generateDirectProperty(methodNode.returnType, PropertyQuery.GETTER, methodNode, annotation, propertyName)
        }
    }

    private List<MethodNode> generateListProperty(ClassNode type, PropertyQuery query, MethodNode methodNode, AnnotationNode annotation, String propertyName) {
        final factoryMethod = factory(type, annotation, propertyName)
        if (factoryMethod === null) {
            addError('List property must define factory method!', methodNode); return []
        }

        final singularName = propertyName.substring(0, propertyName.size() - 1) // TODO - capitalisation
        final listPropertyType = GenericsUtils.makeClassSafeWithGenerics(ListProperty, type)
        final List<MethodNode> methods = [factoryMethod]

        final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
        methods.add(createMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(type, 'val'), new Parameter(actionClazzType, 'action')],
                codeExpr: {
                    final valVar = GeneralUtils.localVarX('val', type)
                    [
                            GeneralUtils.callX(
                                    GeneralUtils.varX('action', actionClazzType),
                                    'execute',
                                    valVar
                            ),
                            GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', valVar)
                    ]
                }()
        ))
        methods.add(delegateToOverload(0, GeneralUtils.callThisX(factoryMethod.name), methods[1]))

        methods.add(createMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(type, 'val'), closureParam(type)],
                codeExpr: {
                    final valVar = GeneralUtils.localVarX('val', type)
                    delegateAndCall(GeneralUtils.localVarX('closure', RAW_GENERIC_CLOSURE), valVar).tap {
                        it.add(GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', valVar))
                    }
                }()
        ))
        methods.add(delegateToOverload(0, GeneralUtils.callThisX(factoryMethod.name), methods[3]))

        return methods
    }

    private static MethodNode delegateToOverload(int overloadIndex, Expression overloadParam, MethodNode target) {
        final otherParamName = target.parameters[overloadIndex].name

        return createMethod(
                methodName: target.name,
                returnType: target.returnType,
                parameters: Stream.of(target.parameters).filter { it.name !== otherParamName }.collect(Collectors.toList()),
                code: GeneralUtils.stmt(GeneralUtils.callThisX(target.name, GeneralUtils.args(
                        Stream.of(target.parameters).map {
                            if (it.name == otherParamName) return overloadParam
                            return GeneralUtils.varX(it)
                        }.collect(Collectors.toList())
                )))
        )
    }

    private static List<MethodNode> generateDirectProperty(ClassNode type, PropertyQuery query, MethodNode methodNode, AnnotationNode annotation, String propertyName) {
        final List<MethodNode> methods = []

        Expression propertyGetExpr = query.getter(methodNode)
        final createDefaultMethod = factory(type, annotation, propertyName)
        if (createDefaultMethod !== null) {
            methods.add(createDefaultMethod)
            propertyGetExpr = GeneralUtils.ternaryX(GeneralUtils.isNullX(propertyGetExpr), GeneralUtils.callThisX(createDefaultMethod.name), propertyGetExpr)
        }

        final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
        methods.add(createMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(
                        actionClazzType,
                        'action'
                )],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.varX('action', actionClazzType),
                        'execute',
                        GeneralUtils.args(query.getter(methodNode))
                ))
        ))

        methods.add(createMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [closureParam(type)],
                codeExpr: {
                    final List<Expression> expr = []
                    final closure = GeneralUtils.varX('closure', RAW_GENERIC_CLOSURE)
                    final valVar = GeneralUtils.localVarX('val', type)
                    expr.add(GeneralUtils.declX(valVar, propertyGetExpr))
                    expr.addAll(delegateAndCall(closure, valVar))
                    query.setter(methodNode, valVar)?.tap { expr.add(it) }
                    return expr
                }()
        ))

        return methods
    }

    private static List<? extends Expression> delegateAndCall(VariableExpression closure, VariableExpression delegate) {
        return [
                GeneralUtils.callX(closure, 'setDelegate', GeneralUtils.args(delegate)),
                GeneralUtils.callX(closure, 'setResolveStrategy', GeneralUtils.constX(Closure.DELEGATE_FIRST)),
                GeneralUtils.callX(closure, 'call', GeneralUtils.args(delegate))
        ]
    }

    private static Parameter closureParam(ClassNode type, String name = 'closure') {
        new Parameter(
                RAW_GENERIC_CLOSURE,
                name
        ).tap {
            it.addAnnotation(new AnnotationNode(DELEGATES_TO_TYPE).tap {
                it.addMember('value', GeneralUtils.classX(type))
                it.addMember('strategy', GeneralUtils.constX(Closure.DELEGATE_FIRST))
            })
        }
    }

    private static MethodNode factory(ClassNode expectedType, AnnotationNode annotation, String propertyName) {
        final fac = annotation.members.get('factory')
        if (fac !== null) return createMethod(
                methodName: "default${propertyName.capitalize()}",
                modifiers: ACC_PUBLIC,
                code: GeneralUtils.returnS(GeneralUtils.callX(annotation.members.get('factory'), 'call')),
                returnType: expectedType
        )
        return null
    }

    private static String getPropertyName(MethodNode methodNode, AnnotationNode annotation) {
        return getMemberStringValue(annotation, 'propertyName', methodNode.name.substring('get'.size()).uncapitalize())
    }

    static final AnnotationNode GENERATED_ANNOTATION = new AnnotationNode(ClassHelper.make(Generated))
    @NamedVariant
    private static MethodNode createMethod(@NamedParam(required = true) final String methodName,
                                           @NamedParam final int modifiers = ACC_PUBLIC,
                                           @NamedParam final ClassNode returnType = ClassHelper.VOID_TYPE,
                                           @NamedParam final List<Parameter> parameters = new ArrayList<>(),
                                           @NamedParam final ClassNode[] exceptions = ClassNode.EMPTY_ARRAY,
                                           @NamedParam final List<AnnotationNode> annotations = [GENERATED_ANNOTATION],
                                           @NamedParam Statement code = new BlockStatement(),
                                           @NamedParam final List<? extends Expression> codeExpr = null) {
        if (codeExpr !== null) code = GeneralUtils.block(new VariableScope(), codeExpr.stream().map { GeneralUtils.stmt(it) }.toArray { new Statement[it] })

        final MethodNode method = new MethodNode(methodName, modifiers, returnType, parameters.stream().toArray { new Parameter[it] }, exceptions, code)
        method.addAnnotations(annotations)
        method.addAnnotation(new AnnotationNode(ClassHelper.make(CompileStatic)))
        return method
    }
}

@CompileStatic
interface PropertyQuery {
    PropertyQuery PROPERTY = new PropertyQuery() {
        @Override
        Expression getter(MethodNode getterMethod) {
            return GeneralUtils.callX(GeneralUtils.callThisX(getterMethod.name), 'getOrNull')
        }

        @Override
        Expression setter(MethodNode getterMethod, Expression args) {
            return GeneralUtils.callX(GeneralUtils.callThisX(getterMethod.name), 'set', args)
        }
    }

    PropertyQuery GETTER = new PropertyQuery() {
        @Override
        Expression getter(MethodNode getterMethod) {
            return GeneralUtils.callThisX(getterMethod.name)
        }

        @Override
        Expression setter(MethodNode node, Expression args) {
            return null
        }
    }

    Expression getter(MethodNode getterMethod)

    @Nullable
    Expression setter(MethodNode node, Expression args)
}