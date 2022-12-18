package net.minecraftforge.gradle.common.transform.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.common.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.common.transform.PropertyHandler
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

@CompileStatic
class NamedDomainObjectContainerHandler implements PropertyHandler, Opcodes {
    private static final ClassNode MAP_PROPERTY_TYPE = ClassHelper.make(NamedDomainObjectContainer)
    private static final ClassNode UTILS_CLASS = ClassHelper.make('net.minecraftforge.gradle.common.util')

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, MAP_PROPERTY_TYPE)) return false
        final singularName = propertyName.endsWith('s') ? propertyName.substring(0, propertyName.size() - 1) : propertyName
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
                        'create', // TODO - Use register in the future
                        GeneralUtils.args(
                                GeneralUtils.localVarX('name', ClassHelper.STRING_TYPE),
                                GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE)
                        )
                )))
        )

        return true
    }
}
