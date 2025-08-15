package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.definition.Definition
import net.neoforged.gradle.dsl.common.runtime.spec.Specification
import org.gradle.jvm.toolchain.JavaLanguageVersion

import java.util.function.BooleanSupplier
import java.util.function.Predicate

@CompileStatic
enum RecompilerType {
    GRADLE(
            () -> true,
            1000,
            (d) -> true
    ),
    NATIVE(
            () -> false,
            //() -> Runtime.getRuntime().totalMemory() / 1024 * 1024 * 1024 > 2,
            0,
            (Definition<? extends Specification> d) -> false
            //(Definition<? extends Specification> d) -> d.getRequiredJavaVersion().get().asInt() == JavaLanguageVersion.current().asInt()
    );

    private final BooleanSupplier canWork
    private final int priority
    private final Predicate<Definition<? extends Specification>> canRun

    RecompilerType(final BooleanSupplier canWork, final int priority, Predicate<Definition<? extends Specification>> canRun) {
        this.canWork = canWork
        this.priority = priority
        this.canRun = canRun
    }

    static RecompilerType getDefaultCompilerType() {
        final valuesList = new ArrayList<RecompilerType>(values().toList());
        valuesList.sort { t -> t.priority }
        for (final def type in valuesList ) {
            if (type.canWork.getAsBoolean())
                return type;
        }

        return GRADLE;
    }

    static RecompilerType getCompatibleRecompilerType(RecompilerType requested, Definition<? extends Specification> spec) {
        if (requested.canRun.test(spec))
            return requested;

        final valuesList = new ArrayList<RecompilerType>(values().toList());
        valuesList.sort { t -> t.priority }
        for (final def type in valuesList ) {
            if (type.canRun.test(spec))
                return type;
        }

        return GRADLE;
    }
}
