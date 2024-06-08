package net.neoforged.gradle.platform.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class CreateJsonTask extends DefaultRuntime implements WithOutput, WithWorkspace {

    protected void collect(Property<ResolvedComponentResult> libraries, ListProperty<String> coords) {
        coords.addAll(
                libraries.map(new Transformer<Iterable<? extends ResolvedComponentResult>, ResolvedComponentResult>() {
                            @Override
                            public Iterable<? extends ResolvedComponentResult> transform(@NotNull ResolvedComponentResult resolvedComponentResult) {
                                final Set<ResolvedComponentResult> seen = new HashSet<>();
                                collect(resolvedComponentResult, seen);
                                return seen;
                            }

                            private void collect(ResolvedComponentResult result, Set<ResolvedComponentResult> seen) {
                                if (seen.add(result)) {
                                    for (DependencyResult dependency : result.getDependencies()) {
                                        if (dependency instanceof ResolvedDependencyResult resolvedDependency) {
                                            collect(resolvedDependency.getSelected(), seen);
                                        } else {
                                            throw new IllegalStateException("Unresolved dependency type: " + dependency.getRequested().getDisplayName());
                                        }
                                    }
                                }
                            }
                        })
                        .map((Transformer<Iterable<? extends String>, Iterable<? extends ResolvedComponentResult>>) resolvedComponentResults -> {
                            final Set<String> ids = new HashSet<>();
                            for (ResolvedComponentResult result : resolvedComponentResults) {
                                ids.add(result.getId().getDisplayName());
                            }
                            return ids;
                        })
        );
    }
}
