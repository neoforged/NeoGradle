package net.minecraftforge.gradle.dsl.generator.transform.property.files

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.dsl.generator.transform.Unpluralizer
import net.minecraftforge.gradle.dsl.generator.transform.property.PropertyHandler
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.gradle.api.file.ConfigurableFileCollection

@CompileStatic
class FileCollectionPropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode TYPE = ClassHelper.make(ConfigurableFileCollection)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, TYPE)) return false
        final singularName = Unpluralizer.unpluralize(propertyName)

        final objArray = ClassHelper.OBJECT_TYPE.makeArray()
        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(objArray, 'paths')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'from',
                        GeneralUtils.localVarX('paths', objArray)
                ))
        )

        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.OBJECT_TYPE, 'path')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'from',
                        GeneralUtils.localVarX('path', ClassHelper.OBJECT_TYPE)
                ))
        )

        return true
    }
}
