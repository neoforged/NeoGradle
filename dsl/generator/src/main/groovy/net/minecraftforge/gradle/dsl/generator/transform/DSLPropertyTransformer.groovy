//file:noinspection UnnecessaryQualifiedReference
package net.minecraftforge.gradle.dsl.generator.transform

import groovy.transform.*
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import net.minecraftforge.gradle.dsl.generator.transform.property.*
import net.minecraftforge.gradle.dsl.generator.transform.property.files.DirectoryPropertyHandler
import net.minecraftforge.gradle.dsl.generator.transform.property.files.FileCollectionPropertyHandler
import net.minecraftforge.gradle.dsl.generator.transform.property.files.FilePropertyHandler
import org.codehaus.groovy.ast.*
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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.util.Configurable

import javax.annotation.Nullable
import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class DSLPropertyTransformer extends AbstractASTTransformation {
    private static final ClassNode DELEGATES_TO_TYPE = ClassHelper.make(DelegatesTo)
    private static final ClassNode CLOSURE_PARAMS_TYPE = ClassHelper.make(ClosureParams)
    private static final ClassNode CONFIGURABLE_TYPE = ClassHelper.make(Configurable)

    public static final ClassNode RAW_GENERIC_CLOSURE = GenericsUtils.makeClassSafe(Closure)

    private static final List<PropertyHandler> HANDLERS = [
            new MapPropertyHandler(),
            new CollectionPropertyHandler(ListProperty, SetProperty),
            new FileCollectionPropertyHandler(),
            new FilePropertyHandler(),
            new DirectoryPropertyHandler(),
            new NamedDomainObjectContainerHandler(),
            new DefaultPropertyHandler(),
    ] as List<PropertyHandler>

    private static final Set<ClassNode> NON_CONFIGURABLE_TYPES = new HashSet<>([
            ClassHelper.STRING_TYPE,
            ClassHelper.int_TYPE, ClassHelper.Integer_TYPE,
            ClassHelper.byte_TYPE, ClassHelper.Byte_TYPE,
            ClassHelper.short_TYPE, ClassHelper.Short_TYPE,
            ClassHelper.long_TYPE, ClassHelper.Long_TYPE,
            ClassHelper.char_TYPE, ClassHelper.Character_TYPE,
            ClassHelper.boolean_TYPE, ClassHelper.Boolean_TYPE,
            ClassHelper.float_TYPE, ClassHelper.Float_TYPE,
            ClassHelper.double_TYPE, ClassHelper.Double_TYPE,
    ])

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

        final List<MethodNode> methods = []
        final Utils utils = new Utils() {
            @Override
            List<MethodNode> getMethods() {
                return methods
            }

            @Override
            boolean getBoolean(AnnotationNode an, String name, boolean defaultValue = true) {
                final value = getMemberValue(an, name)
                if (value === null) return defaultValue
                return (boolean)value
            }

            @Override
            void addError(String message, ASTNode node) {
                DSLPropertyTransformer.this.addError(message, node)
            }
        }
        HANDLERS.find { it.handle(method, annotation, propertyName, utils) }
        methods.each(method.declaringClass.&addMethod)
    }

    private static String getPropertyName(MethodNode methodNode, AnnotationNode annotation) {
        return getMemberStringValue(annotation, 'propertyName', methodNode.name.substring('get'.size()).uncapitalize())
    }

    static final AnnotationNode GENERATED_ANNOTATION = new AnnotationNode(ClassHelper.make(Generated))

    @CompileStatic
    abstract class Utils {
        abstract List<MethodNode> getMethods()

        @NamedVariant
        MethodNode createAndAddMethod(@NamedParam(required = true) final String methodName,
                     @NamedParam final int modifiers = ACC_PUBLIC,
                     @NamedParam final ClassNode returnType = ClassHelper.VOID_TYPE,
                     @NamedParam final List<Parameter> parameters = new ArrayList<>(),
                     @NamedParam final ClassNode[] exceptions = ClassNode.EMPTY_ARRAY,
                     @NamedParam final List<AnnotationNode> annotations = [GENERATED_ANNOTATION],
                     @NamedParam Statement code = new BlockStatement(),
                     @NamedParam final List<? extends Expression> codeExpr = null,
                     @NamedParam final Closure<List<OverloadDelegationStrategy>> delegationStrategies = { [] as List<OverloadDelegationStrategy> }) {
            if (codeExpr !== null) code = GeneralUtils.block(new VariableScope(), codeExpr.stream().map { GeneralUtils.stmt(it) }.toArray { new Statement[it] })

            final MethodNode method = new MethodNode(methodName, modifiers, returnType, parameters.stream().toArray { new Parameter[it] }, exceptions, code)
            method.addAnnotations(annotations)
            method.addAnnotation(new AnnotationNode(ClassHelper.make(CompileStatic)))

            getMethods().add(method)

            delegationStrategies.call().each { strategy ->
                final otherParamName = method.parameters[strategy.paramIndex].name

                this.createAndAddMethod(
                        methodName: method.name,
                        returnType: method.returnType,
                        parameters: Stream.of(method.parameters).filter { it.name !== otherParamName }.collect(Collectors.toList()),
                        code: GeneralUtils.stmt(GeneralUtils.callThisX(method.name, GeneralUtils.args(
                                Stream.of(method.parameters).map {
                                    if (it.name == otherParamName) return strategy.overload
                                    return GeneralUtils.varX(it)
                                }.collect(Collectors.toList())
                        )))
                )
            }

            return method
        }

        MethodNode factory(ClassNode expectedType, AnnotationNode annotation, String propertyName) {
            final fac = annotation.members.get('factory')
            if (fac !== null) return this.createAndAddMethod(
                    methodName: "_default${propertyName.capitalize()}",
                    modifiers: ACC_PUBLIC,
                    code: GeneralUtils.returnS(GeneralUtils.callX(annotation.members.get('factory'), 'call')),
                    returnType: expectedType
            )
            return null
        }

        Parameter closureParam(ClassNode type, String name = 'closure') {
            new Parameter(
                    RAW_GENERIC_CLOSURE,
                    name
            ).tap {
                it.addAnnotation(new AnnotationNode(DELEGATES_TO_TYPE).tap {
                    it.addMember('value', GeneralUtils.classX(type))
                    it.addMember('strategy', GeneralUtils.constX(Closure.DELEGATE_FIRST))
                })
                it.addAnnotation(new AnnotationNode(CLOSURE_PARAMS_TYPE).tap {
                    it.addMember('value', GeneralUtils.classX(SimpleType))
                    it.addMember('options', GeneralUtils.constX(type.name.replace('$', '.')))
                })
            }
        }

        List<? extends Expression> delegateAndCall(VariableExpression closure, VariableExpression delegate) {
            return [
                    GeneralUtils.callX(closure, 'setDelegate', GeneralUtils.args(delegate)),
                    GeneralUtils.callX(closure, 'setResolveStrategy', GeneralUtils.constX(Closure.DELEGATE_FIRST)),
                    GeneralUtils.callX(closure, 'call', GeneralUtils.args(delegate))
            ]
        }

        void visitPropertyType(ClassNode type, AnnotationNode annotation) {
            if (annotation.members.containsKey('isConfigurable')) return
            if (type in NON_CONFIGURABLE_TYPES || !GeneralUtils.isOrImplements(type, CONFIGURABLE_TYPE)) {
                annotation.addMember('isConfigurable', GeneralUtils.constX(false, true))
            }
        }

        abstract boolean getBoolean(AnnotationNode annotation, String name, boolean defaultValue = true)
        abstract void addError(String message, ASTNode node)
    }

    @CompileStatic
    @TupleConstructor
    static final class OverloadDelegationStrategy {
        final int paramIndex
        final Expression overload
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

        @Override
        Expression getOrElse(MethodNode node, Expression orElse) {
            return GeneralUtils.callX(GeneralUtils.callThisX(node.name), 'getOrElse', orElse)
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

        @Override
        Expression getOrElse(MethodNode node, Expression orElse) {
            final getter = getter(node)
            return GeneralUtils.ternaryX(GeneralUtils.isNullX(getter), orElse, getter)
        }
    }

    Expression getter(MethodNode getterMethod)

    @Nullable
    Expression setter(MethodNode node, Expression args)

    @Nullable
    Expression getOrElse(MethodNode node, Expression orElse)
}