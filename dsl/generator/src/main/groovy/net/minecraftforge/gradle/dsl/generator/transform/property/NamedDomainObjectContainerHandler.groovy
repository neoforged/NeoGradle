package net.minecraftforge.gradle.dsl.generator.transform.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.dsl.generator.transform.ClosureEquivalentTransformer
import net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.dsl.generator.transform.Unpluralizer
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

@CompileStatic
class NamedDomainObjectContainerHandler implements PropertyHandler, Opcodes {
    private static final ClassNode PROPERTY_TYPE = ClassHelper.make(NamedDomainObjectContainer)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, PROPERTY_TYPE)) return false

        final singularName = Unpluralizer.unpluralize(propertyName)
        final type = methodNode.returnType.genericsTypes[0].type

        final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)

        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.STRING_TYPE, 'name'), new Parameter(actionClazzType, 'action')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'register',
                        GeneralUtils.args(
                                GeneralUtils.localVarX('name', ClassHelper.STRING_TYPE),
                                GeneralUtils.localVarX('action', actionClazzType)
                        )
                ))
        )

        final scope = new VariableScope()
        scope.putDeclaredVariable(GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE))
        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.STRING_TYPE, 'name'), utils.closureParam(type)],
                code: GeneralUtils.block(scope, GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'register',
                        GeneralUtils.args(
                                GeneralUtils.localVarX('name', ClassHelper.STRING_TYPE),
                                ClosureEquivalentTransformer.asAction(GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE))
                        )
                )))
        )

        return true
    }
}
