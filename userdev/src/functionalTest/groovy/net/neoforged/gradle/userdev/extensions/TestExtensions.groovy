package net.neoforged.gradle.userdev.extensions

import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.Builder
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.RunBuilder
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.RunResult
import org.gradle.testkit.runner.TaskOutcome
import net.neoforged.gradle.userdev.constants.TestConstants;

class TestExtensions {

    static Builder withRun(
            Builder self,
            String additionalBuildContent = ""
    ) {
        self.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            dependencies {
                implementation 'net.neoforged:neoforge:${TestConstants.Latest.NeoForgeVersion}'
            }
            
            runs {
                server { 
                    environmentVariables.put('NEOFORGE_DEDICATED_SERVER_SELFTEST', "${(new Date()).format('ddMMyy_HHmm')}.json")
                    arguments.add('--nogui')
                }
            }
            
            ${additionalBuildContent.stripIndent(true)}
            """)
        self.file("runs/server/eula.txt", """eula=true""")
        self.withToolchains()
        self.enableLocalBuildCache()
        self.withTemporaryGlobalCacheDirectory()
        self.withMod()
    }

    static RunBuilder run(
            RunBuilder self
    ) {
        self.tasks('runServer')
    }

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
        return self.task(':runServer').outcome == TaskOutcome.SUCCESS && self.output.contains("NeoGradle Test Mod has been loaded successfully!")
    }
}
