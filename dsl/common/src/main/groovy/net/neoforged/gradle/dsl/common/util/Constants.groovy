package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic

@CompileStatic
class Constants {

    public static final String BINPATCHER_VERSION =  "1.+";
    public static final String BINPATCHER_VERSION_INTERPOLATION = "net.minecraftforge:binarypatcher:%s:fatjar";
    public static final String BINPATCHER = String.format(BINPATCHER_VERSION_INTERPOLATION, BINPATCHER_VERSION);
    public static final String ACCESSTRANSFORMER_VERSION = "8.0.+";
    public static final String ACCESSTRANSFORMER_VERSION_INTERPOLATION = "net.minecraftforge:accesstransformers:%s:fatjar";
    public static final String ACCESSTRANSFORMER = String.format(ACCESSTRANSFORMER_VERSION_INTERPOLATION, ACCESSTRANSFORMER_VERSION);
    public static final String SPECIALSOURCE = "net.md-5:SpecialSource:1.11.0:shaded";
    public static final String FART_VERSION = "1.0.2";
    public static final String FART_ARTIFACT_INTERPOLATION = "net.minecraftforge:ForgeAutoRenamingTool:%s:all";
    public static final String FART = String.format(FART_ARTIFACT_INTERPOLATION, FART_VERSION);
    public static final String SRG2SOURCE =  "net.minecraftforge:Srg2Source:8.+:fatjar";
    public static final String SIDESTRIPPER = "net.minecraftforge:mergetool:1.1.5:fatjar";
    public static final String INSTALLERTOOLS = "net.minecraftforge:installertools:1.3.2:fatjar";
    public static final String JARCOMPATIBILITYCHECKER = "net.minecraftforge:JarCompatibilityChecker:0.1.+:all";
    public static final String VINEFLOWER_VERSION = "1.9.3";
    public static final String VINEFLOWER_ARTIFACT_INTERPOLATION = "org.vineflower:vineflower:%s";
    public static final String VINEFLOWER = String.format(VINEFLOWER_ARTIFACT_INTERPOLATION, VINEFLOWER_VERSION);
}
