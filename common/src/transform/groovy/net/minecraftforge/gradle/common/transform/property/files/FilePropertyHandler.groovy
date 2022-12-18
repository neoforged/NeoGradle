package net.minecraftforge.gradle.common.transform.property.files

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gradle.common.transform.DSLPropertyTransformer
import net.minecraftforge.gradle.common.transform.ProjectGetter
import net.minecraftforge.gradle.common.transform.PropertyHandler
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty

@CompileStatic
class FilePropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode TYPE = ClassHelper.make(RegularFileProperty)
    private static final ClassNode FILE_TYPE = ClassHelper.make(File)
    private static final ClassNode REGULAR_FILE_TYPE = ClassHelper.make(RegularFile)
    private static final ClassNode PROJECT_GETTER_TYPE = ClassHelper.make(ProjectGetter)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, TYPE)) return false

        final projectGetter = methodNode.declaringClass.methods.find {
            it.annotations.any { it.classNode == PROJECT_GETTER_TYPE }
        }
        if (projectGetter === null) {
            utils.addError('Please provide a project getter for RegularFileProperties!', methodNode)
            return true
        }

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(FILE_TYPE, 'file')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'set',
                        GeneralUtils.localVarX('file', FILE_TYPE)
                ))
        )

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(REGULAR_FILE_TYPE, 'file')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'value',
                        GeneralUtils.localVarX('file', REGULAR_FILE_TYPE)
                ))
        )

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.OBJECT_TYPE, 'file')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'set',
                        GeneralUtils.callX(
                                GeneralUtils.callThisX(projectGetter.name),
                                'file',
                                GeneralUtils.localVarX('file', ClassHelper.OBJECT_TYPE)
                        )
                ))
        )

        return true
    }
}
