package net.minecraftforge.gradle.dsl.generator.transform.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.provider.ListProperty

@CompileStatic
class ListPropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode LIST_PROPERTY_TYPE = ClassHelper.make(ListProperty)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, LIST_PROPERTY_TYPE)) return false
        final singularName = propertyName.endsWith('s') ? propertyName.substring(0, propertyName.size() - 1) : propertyName
        final type = methodNode.returnType.genericsTypes[0].type
        utils.visitPropertyType(type, annotation)
        final factoryMethod = utils.factory(type, annotation, singularName)
        final delegation = factoryMethod === null ? null : new DSLPropertyTransformer.OverloadDelegationStrategy(0, GeneralUtils.callThisX(factoryMethod.name))

        if (utils.getBoolean(annotation, 'isConfigurable', true)) {
            final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
            utils.createAndAddMethod(
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
                    }(),
                    delegationStrategies: { factoryMethod === null ? [] : [delegation] }
            )

            utils.createAndAddMethod(
                    methodName: singularName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(type, 'val'), utils.closureParam(type)],
                    codeExpr: {
                        final valVar = GeneralUtils.localVarX('val', type)
                        utils.delegateAndCall(GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE), valVar).tap {
                            it.add(GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', valVar))
                        }
                    }(),
                    delegationStrategies: { factoryMethod === null ? [] : [delegation] }
            )
        }

        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(type, 'val')],
                codeExpr: [GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', GeneralUtils.localVarX('val', type))]
        )
        return true
    }
}
