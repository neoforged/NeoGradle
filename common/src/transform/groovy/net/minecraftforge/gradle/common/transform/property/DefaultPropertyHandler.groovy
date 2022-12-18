package net.minecraftforge.gradle.common.transform.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.common.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.common.transform.PropertyHandler
import net.minecraftforge.gradle.common.transform.PropertyQuery
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
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

        Expression propertyGetExpr = query.getter(methodNode)
        final createDefaultMethod = utils.factory(type, annotation, propertyName)

        if (createDefaultMethod !== null) {
            propertyGetExpr = GeneralUtils.ternaryX(GeneralUtils.isNullX(propertyGetExpr), GeneralUtils.callThisX(createDefaultMethod.name), propertyGetExpr)
        }

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(type, 'val')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'set',
                        GeneralUtils.localVarX('val', type)
                ))
        )

        if (utils.getBoolean(annotation, 'isConfigurable', true)) {
            final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
            utils.createAndAddMethod(
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
            )

            utils.createAndAddMethod(
                    methodName: propertyName,
                    modifiers: ACC_PUBLIC,
                    parameters: [utils.closureParam(type)],
                    codeExpr: {
                        final List<Expression> expr = []
                        final closure = GeneralUtils.varX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE)
                        final valVar = GeneralUtils.localVarX('val', type)
                        expr.add(GeneralUtils.declX(valVar, propertyGetExpr))
                        expr.addAll(utils.delegateAndCall(closure, valVar))
                        query.setter(methodNode, valVar)?.tap { expr.add(it) }
                        return expr
                    }()
            )
        }
    }
}
