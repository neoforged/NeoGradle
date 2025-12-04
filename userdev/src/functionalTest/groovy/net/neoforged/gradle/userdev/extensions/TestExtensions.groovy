package net.neoforged.gradle.userdev.extensions

import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.Builder
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.RunResult;

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
        self.file("src/main/resources/META-INF/neoforge.mods.toml", """
            license="MIT"
            [[mods]]
            modId="neogradle_test"
            version="1.0"
            displayName="NeoGradle Test"
            description='''Test Mod for NeoGradle'''
            """.stripMargin())

        self.file("src/main/java/net/neoforged/gradle/userdev/EntryPoint.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.fml.common.Mod;
                import org.slf4j.Logger;
                import com.mojang.logging.LogUtils;
                
                @Mod("neogradle_test")
                public class EntryPoint {
                
                    // Directly reference a slf4j logger
                    public static final Logger LOGGER = LogUtils.getLogger();
                
                    public EntryPoint() {
                        LOGGER.info("NeoGradle Test Mod has been loaded successfully!");
                    }
                }
            """.stripMargin())

        return self
    }

    static boolean checkModLoading(RunResult self) {
        return self.output.contains("NeoGradle Test Mod has been loaded successfully!")
    }
}
