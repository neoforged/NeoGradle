package net.neoforged.gradle.userdev


import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

import java.util.zip.ZipFile

class AccessTransformerTests  extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "the userdev runtime supports loading ats from a file"() {
        given:
        def project = create("userdev_supports_ats_from_file", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft fixerUpper # fixerUpper""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().fixerUpper.getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.parallel()
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
            it.stacktrace()
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading ats from a file with the decompiler disabled"() {
        given:
        def project = create("userdev_supports_ats_from_file_decompiler_disabled", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft fixerUpper # fixerUpper""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().fixerUpper.getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.parallel()
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
            it.stacktrace()
        }

        then:
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
        initialRun.task(":neoFormDecompile") == null
    }

    def "the userdev runtime supports loading ats from a file after the dependencies block"() {
        given:
        def project = create("userdev_supports_ats_from_file", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')
            """)
            it.file("src/main/resources/META-INF/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft fixerUpper # fixerUpper""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().fixerUpper.getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading ats from multiple files"() {
        given:
        def project = create("userdev_supports_ats_in_multiple_distinctly_named_files", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer2.cfg')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft fixerUpper # fixerUpper""")
            it.file("src/main/resources/META-INF/accesstransformer2.cfg", """public-f net.minecraft.client.Minecraft LOGGER # LOGGER""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().fixerUpper.getClass().toString());
                        System.out.println(Minecraft.LOGGER.getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading ats from multiple files named the same in different directories"() {
        given:
        def project = create("userdev_supports_ats_in_files_named_the_same", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')
            minecraft.accessTransformers.file rootProject.file('src/main/resources/accesstransformer.cfg')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft fixerUpper # fixerUpper""")
            it.file("src/main/resources/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft LOGGER # LOGGER""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().fixerUpper.getClass().toString());
                        System.out.println(Minecraft.LOGGER.getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }
}
