package net.neoforged.gradle.dsl.platform.model

import com.google.gson.annotations.SerializedName
import groovy.transform.CompileStatic

@CompileStatic
enum RuleAction {

    @SerializedName("allow")
    ALLOWED,
    @SerializedName("disallow")
    DISALLOWED;

    boolean isAllowed() {
        return ALLOWED == this;
    }

    boolean isDisallowed() {
        return DISALLOWED == this;
    }
}
