package net.neoforged.gradle.common.deobfuscation;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class DeobfuscatingTaskConfiguration {

    private final Configuration configuration;

    private final DependencyReplacementResult dependencyReplacementResult;

    private final ResolvedDependency resolvedDependency;

    private final File input;

    private final Map<ResolvedDependency, Optional<DependencyReplacementResult>> childResults;

    public DeobfuscatingTaskConfiguration(Configuration configuration, DependencyReplacementResult dependencyReplacementResult, ResolvedDependency resolvedDependency, File input, Map<ResolvedDependency, Optional<DependencyReplacementResult>> childResults) {
        this.configuration = configuration;
        this.dependencyReplacementResult = dependencyReplacementResult;
        this.resolvedDependency = resolvedDependency;
        this.input = input;
        this.childResults = childResults;
    }

    public DependencyReplacementResult getDependencyReplacementResult() {
        return dependencyReplacementResult;
    }

    public ResolvedDependency getResolvedDependency() {
        return resolvedDependency;
    }

    public File getInput() {
        return input;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Map<ResolvedDependency, Optional<DependencyReplacementResult>> getChildResults() {
        return childResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeobfuscatingTaskConfiguration)) return false;

        DeobfuscatingTaskConfiguration that = (DeobfuscatingTaskConfiguration) o;

        if (getConfiguration() != null ? !getConfiguration().equals(that.getConfiguration()) : that.getConfiguration() != null)
            return false;
        if (getDependencyReplacementResult() != null ? !getDependencyReplacementResult().equals(that.getDependencyReplacementResult()) : that.getDependencyReplacementResult() != null)
            return false;
        if (getResolvedDependency() != null ? !getResolvedDependency().equals(that.getResolvedDependency()) : that.getResolvedDependency() != null)
            return false;
        if (getInput() != null ? !getInput().equals(that.getInput()) : that.getInput() != null) return false;
        return getChildResults() != null ? getChildResults().equals(that.getChildResults()) : that.getChildResults() == null;
    }

    @Override
    public int hashCode() {
        int result = getConfiguration() != null ? getConfiguration().hashCode() : 0;
        result = 31 * result + (getDependencyReplacementResult() != null ? getDependencyReplacementResult().hashCode() : 0);
        result = 31 * result + (getResolvedDependency() != null ? getResolvedDependency().hashCode() : 0);
        result = 31 * result + (getInput() != null ? getInput().hashCode() : 0);
        result = 31 * result + (getChildResults() != null ? getChildResults().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeobfuscatingTaskConfiguration{" +
                "configuration=" + configuration +
                ", dependencyReplacementResult=" + dependencyReplacementResult +
                ", resolvedDependency=" + resolvedDependency +
                ", input=" + input +
                ", childResults=" + childResults +
                '}';
    }
}
