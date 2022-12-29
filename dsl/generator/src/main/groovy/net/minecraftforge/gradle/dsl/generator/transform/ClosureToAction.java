package net.minecraftforge.gradle.dsl.generator.transform;

import groovy.lang.Closure;
import org.gradle.api.Action;

public class ClosureToAction {
    @SuppressWarnings("rawtypes")
    public static <T> Action<T> delegateAndCall(Closure closure) {
        return t -> {
            closure.setDelegate(t);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call(t);
        };
    }
}
