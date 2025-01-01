package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic

@CompileStatic
class Constants {
    public static final String DEFAULT_PARCHMENT_GROUP = "org.parchmentmc.data"
    public static final String DEFAULT_PARCHMENT_ARTIFACT_PREFIX = "parchment-"
    public static final String DEFAULT_PARCHMENT_MAVEN_URL = "https://maven.parchmentmc.org/"
    public static final String JST_TOOL_ARTIFACT = "net.neoforged.jst:jst-cli-bundle:1.0.67"
    public static final String DEVLOGIN_TOOL_ARTIFACT = "net.covers1624:DevLogin:0.1.0.4"
    public static final String RENDERNURSE_TOOL_ARTIFACT = "net.neoforged:render-nurse:0.0.12";
    public static final String DEVLOGIN_MAIN_CLASS = "net.covers1624.devlogin.DevLogin"
    public static final String BINPARCHER_TOOL_ARTIFACT = "net.neoforged.installertools:binarypatcher:2.1.7:fatjar"
    public static final String ACCESSTRANSFORMER_TOOL_ARTIFACT = "net.neoforged.accesstransformers:at-cli:11.0.2:fatjar"
    public static final String FART_TOOL_ARTIFACT = "net.neoforged:AutoRenamingTool:2.0.4:all"
    public static final String INSTALLERTOOLS_TOOL_ARTIFACT = "net.neoforged.installertools:installertools:2.1.7"
    public static final String JARSPLITTER_TOOL_ARTIFACT = "net.neoforged.installertools:jarsplitter:2.1.7"
    public static final String DECOMPILER_TOOL_ARTIFACT = "org.vineflower:vineflower:1.10.1"

    public static final String DEFAULT_RECOMPILER_MAX_MEMORY = "1g"

    public static final String SUBSYSTEM_PROPERTY_PREFIX = "neogradle.subsystems."
}
