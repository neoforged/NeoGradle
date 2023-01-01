package net.minecraftforge.gradle.dsl.generator.transform.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.dsl.generator.transform.PropertyQuery
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.provider.Property

@CompileStatic
class DefaultPropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (GeneralUtils.isOrImplements(methodNode.returnType, PROPERTY_TYPE)) {
            return generateDirectProperty(methodNode.returnType.genericsTypes[0].type, PropertyQuery.PROPERTY, methodNode, annotation, propertyName, utils)
        } else {
            generateDirectProperty(methodNode.returnType, PropertyQuery.GETTER, methodNode, annotation, propertyName, utils)
        }
        return true
    }

    static void generateDirectProperty(ClassNode type, PropertyQuery query, MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        utils.visitPropertyType(type, annotation)
        type = DSLPropertyTransformer.WRAPPER_TO_PRIMITIVE.getOrDefault(type, type)

        Expression propertyGetExpr = query.getter(methodNode)
        final createDefaultMethod = utils.factory(type, annotation, propertyName)

        if (createDefaultMethod !== null) {
            propertyGetExpr = query.getOrElse(methodNode, GeneralUtils.callThisX(createDefaultMethod.name))
        }

        final delegationStrategy = new DSLPropertyTransformer.OverloadDelegationStrategy(0, propertyGetExpr)

        final defaultSetter = { String methodName ->
            utils.createAndAddMethod(
                    methodName: methodName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(type, propertyName)],
                    code: GeneralUtils.stmt(GeneralUtils.callX(
                            GeneralUtils.callThisX(methodNode.name),
                            'set',
                            GeneralUtils.localVarX(propertyName, type)
                    ))
            )
        }

        if (propertyName.startsWith('is')) {
            final name = propertyName.substring(2)
            defaultSetter("set$name")
            defaultSetter(name.uncapitalize())
        } else {
            defaultSetter(propertyName)
        }

        if (utils.getBoolean(annotation, 'isConfigurable', true)) {
            final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
            utils.createAndAddMethod(
                    methodName: propertyName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(type, propertyName), new Parameter(
                            actionClazzType,
                            'action'
                    )],
                    codeExpr: {
                        final valVar = GeneralUtils.localVarX(propertyName, type)
                        [
                                GeneralUtils.callX(
                                        GeneralUtils.varX('action', actionClazzType),
                                        'execute',
                                        valVar
                                ),
                                GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'set', valVar)
                        ]
                    }(),
                    delegationStrategies: { [delegationStrategy] }
            )

            utils.createAndAddMethod(
                    methodName: propertyName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(type, propertyName), utils.closureParam(type)],
                    codeExpr: {
                        final List<Expression> expr = []
                        final closure = GeneralUtils.varX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE)
                        final valVar = GeneralUtils.localVarX(propertyName, type)
                        expr.addAll(utils.delegateAndCall(closure, valVar))
                        query.setter(methodNode, valVar)?.tap { expr.add(it) }
                        return expr
                    }(),
                    delegationStrategies: { [delegationStrategy] }
            )
        }
    }
}
