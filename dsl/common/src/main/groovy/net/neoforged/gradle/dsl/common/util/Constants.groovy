package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic

@CompileStatic
class Constants {

    public static final String BINPATCHER_VERSION =  "1.1.1";
    public static final String BINPATCHER_VERSION_INTERPOLATION = "net.minecraftforge:binarypatcher:%s:fatjar";
    public static final String BINPATCHER = String.format(BINPATCHER_VERSION_INTERPOLATION, BINPATCHER_VERSION);
    public static final String ACCESSTRANSFORMER_VERSION = "10.0.+";
    public static final String ACCESSTRANSFORMER_VERSION_INTERPOLATION = "net.neoforged.accesstransformers:at-cli:%s:fatjar";
    public static final String ACCESSTRANSFORMER = String.format(ACCESSTRANSFORMER_VERSION_INTERPOLATION, ACCESSTRANSFORMER_VERSION);
    public static final String SPECIALSOURCE = "net.md-5:SpecialSource:1.11.0:shaded";
    public static final String FART_VERSION = "2.0.3";
    public static final String FART_ARTIFACT_INTERPOLATION = "net.neoforged:AutoRenamingTool:%s:all";
    public static final String FART = String.format(FART_ARTIFACT_INTERPOLATION, FART_VERSION);
    public static final String INSTALLERTOOLS_VERSION = '2.1.2'
    public static final String INSTALLERTOOLS = "net.neoforged.installertools:installertools:${INSTALLERTOOLS_VERSION}"
    public static final String JARSPLITTER = "net.neoforged.installertools:jarsplitter:${INSTALLERTOOLS_VERSION}"
    public static final String BINARYPATCHER = "net.neoforged.installertools:binarypatcher:${INSTALLERTOOLS_VERSION}"

    public static final String VINEFLOWER_VERSION = "1.9.3";
    public static final String VINEFLOWER_ARTIFACT_INTERPOLATION = "org.vineflower:vineflower:%s";
    public static final String VINEFLOWER = String.format(VINEFLOWER_ARTIFACT_INTERPOLATION, VINEFLOWER_VERSION);

    public static final String DEFAULT_PARCHMENT_GROUP = "org.parchmentmc.data"
    public static final String DEFAULT_PARCHMENT_ARTIFACT_PREFIX = "parchment-"
    public static final String DEFAULT_PARCHMENT_MAVEN_URL = "https://maven.parchmentmc.org/"
    public static final String JST_TOOL_ARTIFACT = "net.neoforged.jst:jst-cli-bundle:1.0.43"
    public static final String DEVLOGIN_TOOL_ARTIFACT = "net.covers1624:DevLogin:0.1.0.4"
    public static final String DEVLOGIN_MAIN_CLASS = "net.covers1624.devlogin.DevLogin"

    public static final String DEFAULT_RECOMPILER_MAX_MEMORY = "1g"

    public static final String SUBSYSTEM_PROPERTY_PREFIX = "neogradle.subsystems."
}
