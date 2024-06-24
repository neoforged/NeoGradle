package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class MultiProjectTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev"
        injectIntoRootProject = true
    }

    def "multiple projects with neoforge dependencies should be able to run the game"() {
        given:
        def rootProject = create("multi_neoforge_root", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        def apiProject = create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
        })

        def mainProject = create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSource project(':api').sourceSets.main
                }
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runData')
            //We are expecting this test to fail, since there is a mod without any files included so it is fine.
            it.shouldFail()
        }

        then:
        run.task(':main:writeMinecraftClasspathData').outcome == TaskOutcome.SUCCESS

        def resourcesMainBuildDir = run.file("main/build/resources/main")
        run.output.contains("Error during pre-loading phase: ERROR: File null is not a valid mod file") ||
                run.output.contains("Caused by: net.neoforged.fml.ModLoadingException: Loading errors encountered:\n" +
                        "\t- File ${resourcesMainBuildDir.absolutePath} is not a valid mod file")//Validate that we are failing because of the missing mod file, and not something else.
    }

    def "multiple projects with neoforge dependencies should run using the central cache"() {
        given:
        def rootProject = create("multi_neoforge_root_cached", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withGlobalCacheDirectory(tempDir)
            it.withToolchains()
        })

        def apiProject = create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
        })

        def mainProject = create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSource project(':api').sourceSets.main
                }
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:build')
        }

        then:
        run.task(':main:build').outcome == TaskOutcome.SUCCESS
        run.task(':api:neoFormDecompile').outcome == TaskOutcome.SUCCESS
        run.task(':main:neoFormDecompile').outcome == TaskOutcome.SUCCESS
    }

    def "multiple projects where one is not neogradle with neoforge dependencies should pull the mod-classes entry from the project name"() {
        given:
        def rootProject = create("multi_neoforge_root_none_ng", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.disableConventions()
        })

        create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin("java-library")
        })

        create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:20.6.104-beta'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSource project.sourceSets.main
                    modSource project(':api').sourceSets.main
                }
            }
            """)
            it.withManifest([
                "FMLModType": "GAMELIBRARY",
                "Automatic-Module-Name": "main"
            ])
            it.file("src/main/resources/sometime.properties", """
                some=thing
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
            it.withManifest()
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runs')
            it.stacktrace()
        }

        then:
        run.task(':main:runs').outcome == TaskOutcome.SUCCESS

        def modSourcesSection = run.output.split(System.lineSeparator()).toList().subList(
                run.output.split(System.lineSeparator()).toList().indexOf("Mod Sources:"),
                run.output.split(System.lineSeparator()).toList().indexOf("Unit Test Sources:")
        )

        modSourcesSection.size() == 5
        modSourcesSection.get(0) == "Mod Sources:"
        modSourcesSection.get(1) == "  - main:"
        modSourcesSection.get(2) == "    - main"
        modSourcesSection.get(3) == "  - api:"
        modSourcesSection.get(4) == "    - main"
    }

    def "multiple projects where one is not neogradle with neoforge dependencies should pull the mod-classes entry from the project name, using the dsl"() {
        given:
        def rootProject = create("multi_neoforge_root_none_ng", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.disableConventions()
        })

        create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin("java-library")
        })

        create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:20.6.104-beta'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSources {
                       add project.sourceSets.main
                       add project(':api').sourceSets.main
                    }
                }
            }
            """)
            it.withManifest([
                    "FMLModType": "GAMELIBRARY",
                    "Automatic-Module-Name": "main"
            ])
            it.file("src/main/resources/sometime.properties", """
                some=thing
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
            it.withManifest()
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runs')
            it.stacktrace()
        }

        then:
        run.task(':main:runs').outcome == TaskOutcome.SUCCESS

        def modSourcesSection = run.output.split(System.lineSeparator()).toList().subList(
                run.output.split(System.lineSeparator()).toList().indexOf("Mod Sources:"),
                run.output.split(System.lineSeparator()).toList().indexOf("Unit Test Sources:")
        )
        modSourcesSection.size() == 5
        modSourcesSection.get(0) == "Mod Sources:"
        modSourcesSection.get(1) == "  - main:"
        modSourcesSection.get(2) == "    - main"
        modSourcesSection.get(3) == "  - api:"
        modSourcesSection.get(4) == "    - main"
    }


    def "multiple projects where one is not neogradle with neoforge dependencies should pull the mod-classes entry from the project name, using the legacy dsl"() {
        given:
        def rootProject = create("multi_neoforge_root_none_ng", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.disableConventions()
        })

        create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin("java-library")
        })

        create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:20.6.104-beta'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSource project.sourceSets.main
                    modSource project(':api').sourceSets.main
                }
            }
            """)
            it.withManifest([
                    "FMLModType": "GAMELIBRARY",
                    "Automatic-Module-Name": "main"
            ])
            it.file("src/main/resources/sometime.properties", """
                some=thing
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
            it.withManifest()
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runs')
            it.stacktrace()
        }

        then:
        run.task(':main:runs').outcome == TaskOutcome.SUCCESS

        def modSourcesSection = run.output.split(System.lineSeparator()).toList().subList(
                run.output.split(System.lineSeparator()).toList().indexOf("Mod Sources:"),
                run.output.split(System.lineSeparator()).toList().indexOf("Unit Test Sources:")
        )
        modSourcesSection.size() == 5
        modSourcesSection.get(0) == "Mod Sources:"
        modSourcesSection.get(1) == "  - main:"
        modSourcesSection.get(2) == "    - main"
        modSourcesSection.get(3) == "  - api:"
        modSourcesSection.get(4) == "    - main"
    }

    def "multiple projects where one is not neogradle with neoforge dependencies should combine the mod-classes entry from the project name, using the local-dsl"() {
        given:
        def rootProject = create("multi_neoforge_root_none_ng", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.disableConventions()
        })

        create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin("java-library")
        })

        create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:20.6.104-beta'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSources {
                       add project.sourceSets.main
                       local project(':api').sourceSets.main
                    }
                }
            }
            """)
            it.withManifest([
                    "FMLModType": "GAMELIBRARY",
                    "Automatic-Module-Name": "main"
            ])
            it.file("src/main/resources/sometime.properties", """
                some=thing
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
            it.withManifest()
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runs')
            it.stacktrace()
        }

        then:
        run.task(':main:runs').outcome == TaskOutcome.SUCCESS

        def modSourcesSection = run.output.split(System.lineSeparator()).toList().subList(
                run.output.split(System.lineSeparator()).toList().indexOf("Mod Sources:"),
                run.output.split(System.lineSeparator()).toList().indexOf("Unit Test Sources:")
        )
        modSourcesSection.size() == 4
        modSourcesSection.get(0) == "Mod Sources:"
        modSourcesSection.get(1) == "  - main:"
        modSourcesSection.get(2) == "    - main"
        modSourcesSection.get(3) == "    - main"
    }

    def "multiple projects where one is not neogradle with neoforge dependencies should combine the mod-classes entry from the project name, using custom group id"() {
        given:
        def rootProject = create("multi_neoforge_root_none_ng", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println("Hello World");
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin("java-library")
        })

        create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:20.6.104-beta'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSources {
                       add('something', project.sourceSets.main)
                       add('something', project(':api').sourceSets.main)
                    }
                }
            }
            """)
            it.withManifest([
                    "FMLModType": "GAMELIBRARY",
                    "Automatic-Module-Name": "main"
            ])
            it.file("src/main/resources/sometime.properties", """
                some=thing
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
            it.withManifest()
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runs')
            it.stacktrace()
        }

        then:
        run.task(':main:runs').outcome == TaskOutcome.SUCCESS

        def modSourcesSection = run.output.split(System.lineSeparator()).toList().subList(
                run.output.split(System.lineSeparator()).toList().indexOf("Mod Sources:"),
                run.output.split(System.lineSeparator()).toList().indexOf("Unit Test Sources:")
        )
        modSourcesSection.size() == 4
        modSourcesSection.get(0) == "Mod Sources:"
        modSourcesSection.get(1) == "  - something:"
        modSourcesSection.get(2) == "    - main"
        modSourcesSection.get(3) == "    - main"
    }


    def "multiple projects with neoforge dependencies should run when parallel is enabled"() {
        given:
        def rootProject = create("multi_neoforge_root_cached", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withGlobalCacheDirectory(tempDir)
            it.withToolchains()
            it.enableGradleParallelRunning()
        })

        def apiProject = create(rootProject, "api", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
        })

        def mainProject = create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSource project(':api').sourceSets.main
                }
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.plugin(this.pluginUnderTest)
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:build')
            it.stacktrace()
        }

        then:
        run.task(':main:build').outcome == TaskOutcome.SUCCESS
        run.task(':api:neoFormDecompile').outcome == TaskOutcome.SUCCESS || run.task(':api:neoFormDecompile').outcome == TaskOutcome.UP_TO_DATE
        run.task(':main:neoFormDecompile').outcome == TaskOutcome.SUCCESS || run.task(':main:neoFormDecompile').outcome == TaskOutcome.UP_TO_DATE

        if (run.task(':api:neoFormDecompile').outcome == TaskOutcome.SUCCESS)
            run.task(':main:neoFormDecompile').outcome == TaskOutcome.UP_TO_DATE
        else if (run.task(':main:neoFormDecompile').outcome == TaskOutcome.SUCCESS)
            run.task(':api:neoFormDecompile').outcome == TaskOutcome.UP_TO_DATE
    }
}
