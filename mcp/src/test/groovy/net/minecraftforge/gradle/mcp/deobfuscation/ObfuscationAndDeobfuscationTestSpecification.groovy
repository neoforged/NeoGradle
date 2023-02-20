package net.minecraftforge.gradle.mcp.deobfuscation

import net.minecraftforge.gradle.base.BuilderBasedTestSpecification

class ObfuscationAndDeobfuscationTestSpecification extends BuilderBasedTestSpecification {

    def "supportObfuscationAndDeobfuscation"() {
        given:
        def obfuscatedProject = create("obfuscated", builder -> {
            builder.build("""
                
            """)
        })
    }

}
