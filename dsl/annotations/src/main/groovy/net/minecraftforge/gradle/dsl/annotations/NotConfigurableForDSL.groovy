package net.minecraftforge.gradle.dsl.annotations

@interface NotConfigurableForDSL {
    String reason() default 'This type is not configurable for the DSL.'
}