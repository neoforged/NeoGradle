package net.minecraftforge.gradle.dsl.common.util

class Constants {
    public static final String FORGE_MAVEN = "https://maven.minecraftforge.net/";
    public static final String MOJANG_MAVEN = "https://libraries.minecraft.net/";
    public static final String BINPATCHER =  "net.minecraftforge:binarypatcher:1.+:fatjar";
    public static final String ACCESSTRANSFORMER_VERSION = "8.0.+";
    public static final String ACCESSTRANSFORMER_VERSION_INTERPOLATION = "net.minecraftforge:accesstransformers:%s:fatjar";
    public static final String ACCESSTRANSFORMER = String.format(ACCESSTRANSFORMER_VERSION_INTERPOLATION, ACCESSTRANSFORMER_VERSION);
    public static final String SPECIALSOURCE = "net.md-5:SpecialSource:1.11.0:shaded";
    public static final String FART_VERSION = "0.1.+";
    public static final String FART_ARTIFACT_INTERPOLATION = "net.minecraftforge:ForgeAutoRenamingTool:%s:all";
    public static final String FART = String.format(FART_ARTIFACT_INTERPOLATION, FART_VERSION);
    public static final String SRG2SOURCE =  "net.minecraftforge:Srg2Source:8.+:fatjar";
    public static final String SIDESTRIPPER = "net.minecraftforge:mergetool:1.1.5:fatjar";
    public static final String INSTALLERTOOLS = "net.minecraftforge:installertools:1.3.2:fatjar";
    public static final String JARCOMPATIBILITYCHECKER = "net.minecraftforge:JarCompatibilityChecker:0.1.+:all";
    public static final String FORGEFLOWER_VERSION = "2.0.605.1";
    public static final String FORGEFLOWER_ARTIFACT_INTERPOLATION = "net.minecraftforge:forgeflower:%s";
    public static final String FORGEFLOWER = String.format(FORGEFLOWER_ARTIFACT_INTERPOLATION, FORGEFLOWER_VERSION);
    public static final long ZIPTIME = 628041600000L;
    public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
}
