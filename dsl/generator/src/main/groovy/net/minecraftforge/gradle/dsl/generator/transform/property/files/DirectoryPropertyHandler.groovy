package net.minecraftforge.gradle.dsl.generator.transform.property.files

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.dsl.annotations.ProjectGetter
import net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.dsl.generator.transform.property.PropertyHandler
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty

@CompileStatic
class DirectoryPropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode TYPE = ClassHelper.make(DirectoryProperty)
    private static final ClassNode FILE_TYPE = ClassHelper.make(File)
    private static final ClassNode DIR_TYPE = ClassHelper.make(Directory)
    private static final ClassNode PROJECT_GETTER_TYPE = ClassHelper.make(ProjectGetter)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, TYPE)) return false

        final projectGetter = methodNode.declaringClass.methods.find {
            it.annotations.any { it.classNode == PROJECT_GETTER_TYPE }
        }
        if (projectGetter === null) {
            utils.addError('Please provide a project getter for DirectoryProperties!', methodNode)
            return true
        }

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(FILE_TYPE, 'directory')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'set',
                        GeneralUtils.localVarX('directory', FILE_TYPE)
                ))
        )

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(DIR_TYPE, 'directory')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'value',
                        GeneralUtils.localVarX('directory', DIR_TYPE)
                ))
        )

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.OBJECT_TYPE, 'directory')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'set',
                        GeneralUtils.callX(
                                GeneralUtils.callThisX(projectGetter.name),
                                'file',
                                GeneralUtils.localVarX('directory', ClassHelper.OBJECT_TYPE)
                        )
                ))
        )

        return true
    }
}