package net.neoforged.gradle.common.extensions;

import com.google.common.collect.Sets;
import org.gradle.api.artifacts.Dependency;

import java.util.Set;

public class ForcedDependencyDeobfuscationExtension {

    private final Set<Dependency> toDeobfuscate = Sets.newHashSet();

    public void forceDeobfuscation(Dependency dependency) {
        toDeobfuscate.add(dependency);
    }

    public boolean shouldDeobfuscate(Dependency dependency) {
        return toDeobfuscate.contains(dependency);
    }
}
