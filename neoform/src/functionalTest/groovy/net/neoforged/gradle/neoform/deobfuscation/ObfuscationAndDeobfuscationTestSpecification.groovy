package net.neoforged.gradle.neoform.deobfuscation

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification

class ObfuscationAndDeobfuscationTestSpecification extends BuilderBasedTestSpecification {

    def "supportObfuscationAndDeobfuscation"() {
        given:
        def obfuscatedProject = create("obfuscated", builder -> {
            builder.build("""
                
            """)
        })
    }

}
