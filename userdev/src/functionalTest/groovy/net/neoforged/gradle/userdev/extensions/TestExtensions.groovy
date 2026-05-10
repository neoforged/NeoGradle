package net.neoforged.gradle.userdev.extensions

import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.Builder
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.RunBuilder
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime.RunResult
import org.gradle.testkit.runner.TaskOutcome
import net.neoforged.gradle.userdev.constants.TestConstants

import java.util.function.Consumer;

class TestExtensions {

    static Builder withRoot(
            Builder self
    ) {
        self.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            """)
        self.withToolchains()
        self.enableLocalBuildCache()
        self.withTemporaryGlobalCacheDirectory()
    }

    static Builder withSimpleLibrary(
            Builder self,
            String additionalBuildContent = "") {
        self.build(
    """
           plugins {
               id 'java-library'
           }
           java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
           
           sourceSets {
              main {
                 runs {
                   modIdentifier = "main"
                 }
              }
           }
           
           dependencies {
               implementation 'net.neoforged:neoforge:${TestConstants.Latest.NeoForgeVersion}'
           }
                       
           ${additionalBuildContent.stripIndent(true)}
           """)
        self.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
        self.file("runs/server/eula.txt", """eula=true""")
        self.withToolchains()
        self.enableLocalBuildCache()
        self.withTemporaryGlobalCacheDirectory()
    }

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
                    environmentVariables.put('NEOFORGE_DEDICATED_SERVER_SELFTEST', "server_self_test.json")
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
            RunBuilder self,
            String projectPrefix = ""
    ) {
        self.tasks(projectPrefix + ':runServer')
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

    static boolean checkModLoading(RunResult self,
                                   String projectPrefix = "") {
        return self.task(projectPrefix + ':runServer').outcome == TaskOutcome.SUCCESS && self.output.contains("NeoGradle Test Mod has been loaded successfully!")
    }

    static void updatePropertiesFile(net.neoforged.trainingwheels.gradle.functional.builder.Runtime self,
            Consumer<Properties> updater
    ) {
        final File propertiesFile = self.file("gradle.properties")
        if (!propertiesFile.exists()) {
            propertiesFile.parentFile.mkdirs()
            propertiesFile.createNewFile()
        }

        final Properties result = new Properties()
        try(InputStream stream = new FileInputStream(propertiesFile)) {
            result.load(stream)
        }

        updater.accept(result);

        if (propertiesFile.exists()) {
            propertiesFile.delete()
            propertiesFile.createNewFile()
        }
        try(OutputStream stream = new FileOutputStream(propertiesFile)) {
            result.store(stream, "Gradle properties updated by test run")
        }
    }

    static void resetSelfTestFile(net.neoforged.trainingwheels.gradle.functional.builder.Runtime self) {
        var selfTestFile = self.file("runs/server/server_self_test.json")
        if (selfTestFile.exists())
            selfTestFile.delete()
    }
}
