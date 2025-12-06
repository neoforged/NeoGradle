package net.neoforged.gradle.userdev.extensions

import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.Builder
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.RunResult

class TestExtensions {

    /**
     * Registers a test mod to the project run builder.
     *
     * @param self The run builder
     * @return The run builder, with the test mod added.
     */
    static Builder withMod(
            Builder self
    ) {
        return withMod(self, "Simple")
    }

    /**
     * Registers a test mod to the project run builder.
     *
     * @param self The run builder
     * @param name The name of the mod
     * @return The run builder, with the test mod added.
     */
    static Builder withMod(
            Builder self,
            String name
    ) {
        self.file("src/main/resources/META-INF/neoforge.mods.toml",

            """
            license="MIT"
            [[mods]]
            modId="${name.toLowerCase()}"
            version="1.0"
            displayName="NeoGradle Test - ${name}"
            description='''Test Mod for NeoGradle - ${name}'''
            """)

        self.javaClassFile("net.neoforged.gradle.userdev.tests.${name.toLowerCase()}.EntryPoint", """
            package net.neoforged.gradle.userdev.tests.${name.toLowerCase()};
            
            import net.minecraft.client.Minecraft;
            import net.neoforged.fml.common.Mod;
            import org.slf4j.Logger;
            import com.mojang.logging.LogUtils;
            
            @Mod("${name.toLowerCase()}")
            public class EntryPoint {
            
                // Directly reference a slf4j logger
                public static final Logger LOGGER = LogUtils.getLogger();
            
                public EntryPoint() {
                    LOGGER.info("NeoGradle Test Mod has been loaded successfully!");
                }
            }
            """)

        return self
    }

    static boolean checkModLoading(RunResult self) {
        return self.output.contains("NeoGradle Test Mod has been loaded successfully!")
    }
}
