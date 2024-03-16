package net.neoforged.gradle.platform.tasks;

import org.gradle.api.Task;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;

public interface TokenizedTask extends Task {
    
    @Input
    MapProperty<String, String> getTokens();
    
    default void token(String key, Object value) {
        getTokens().put(key, value.toString());
    }
    
    default void token(String key, Provider<?> value) {
        getTokens().put(key, value.map(Object::toString));
    }
}
