package net.minecraftforge.gradle.common.deobfuscation;

import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementContext;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementResult;
import org.gradle.api.artifacts.ResolvedDependency;

import java.io.File;
import java.util.Objects;

public class DeobfuscatingTaskConfiguration {

    private final DependencyReplacementContext context;

    private final DependencyReplacementResult dependencyReplacementResult;

    private final ResolvedDependency resolvedDependency;

    private final File input;

    public DeobfuscatingTaskConfiguration(DependencyReplacementContext context, DependencyReplacementResult dependencyReplacementResult, ResolvedDependency resolvedDependency, File input) {
        this.context = context;
        this.dependencyReplacementResult = dependencyReplacementResult;
        this.resolvedDependency = resolvedDependency;
        this.input = input;
    }

    public DependencyReplacementContext context() {
        return context;
    }

    public DependencyReplacementResult dependencyReplacementResult() {
        return dependencyReplacementResult;
    }

    public ResolvedDependency resolvedDependency() {
        return resolvedDependency;
    }

    public File input() {
        return input;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeobfuscatingTaskConfiguration)) return false;

        DeobfuscatingTaskConfiguration that = (DeobfuscatingTaskConfiguration) o;

        if (!Objects.equals(context, that.context)) return false;
        if (!Objects.equals(dependencyReplacementResult, that.dependencyReplacementResult))
            return false;
        if (!Objects.equals(resolvedDependency, that.resolvedDependency))
            return false;
        return Objects.equals(input, that.input);
    }

    @Override
    public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + (dependencyReplacementResult != null ? dependencyReplacementResult.hashCode() : 0);
        result = 31 * result + (resolvedDependency != null ? resolvedDependency.hashCode() : 0);
        result = 31 * result + (input != null ? input.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeobfuscatingTaskConfiguration{" +
                "context=" + context +
                ", dependencyReplacementResult=" + dependencyReplacementResult +
                ", resolvedDependency=" + resolvedDependency +
                ", input=" + input +
                '}';
    }
}
