package net.neoforged.gradle.userdev


import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class InterfaceInjectionTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "the userdev runtime supports loading iis from a file"() {
        given:
        def project = create("userdev_supports_iis_from_file", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json", """{"net/minecraft/client/Minecraft": ["com/example/examplemod/MyInjectedInterface"]}""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        Minecraft.getInstance().doSomething();
                    }
                }
            """)
            it.file("src/main/java/com/example/examplemod/MyInjectedInterface.java", """
                package com.example.examplemod;
                
                public interface MyInjectedInterface {
                    default void doSomething() { };
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('build')
            it.debug()
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from a file after the dependencies block"() {
        given:
        def project = create("userdev_supports_iis_from_file", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            """)
            it.file("src/main/resources/META-INF/iis.json", """{"net/minecraft/client/Minecraft": ["com/example/examplemod/MyInjectedInterface"]}""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        Minecraft.getInstance().doSomething();
                    }
                }
            """)
            it.file("src/main/java/com/example/examplemod/MyInjectedInterface.java", """
                package com.example.examplemod;
                
                public interface MyInjectedInterface {
                    default void doSomething() { };
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('build')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from multiple files"() {
        given:
        def project = create("userdev_supports_iis_in_multiple_distinctly_named_files", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis2.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json", """{"net/minecraft/client/Minecraft": ["com/example/examplemod/MyInjectedInterface"]}""")
            it.file("src/main/resources/META-INF/iis2.json", """{"net/minecraft/client/Minecraft": ["com/example/examplemod/MySecondaryInterface"]}""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        Minecraft.getInstance().doSomething();
                        Minecraft.getInstance().doSecondSomething();
                    }
                }
            """)
            it.file("src/main/java/com/example/examplemod/MyInjectedInterface.java", """
                package com.example.examplemod;
                
                public interface MyInjectedInterface {
                    default void doSomething() { };
                }
            """)
            it.file("src/main/java/com/example/examplemod/MySecondaryInterface.java", """
                package com.example.examplemod;
                
                public interface MySecondaryInterface {
                    default void doSecondSomething() { };
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('build')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from multiple files named the same in different directories"() {
        given:
        def project = create("userdev_supports_iis_in_files_named_the_same", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/iis.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json", """{"net/minecraft/client/Minecraft": ["com/example/examplemod/MyInjectedInterface"]}""")
            it.file("src/main/resources/iis.json", """{"net/minecraft/client/Minecraft": ["com/example/examplemod/MySecondaryInterface"]}""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        Minecraft.getInstance().doSomething();
                        Minecraft.getInstance().doSecondSomething();
                    }
                }
            """)
            it.file("src/main/java/com/example/examplemod/MyInjectedInterface.java", """
                package com.example.examplemod;
                
                public interface MyInjectedInterface {
                    default void doSomething() { };
                }
            """)
            it.file("src/main/java/com/example/examplemod/MySecondaryInterface.java", """
                package com.example.examplemod;
                
                public interface MySecondaryInterface {
                    default void doSecondSomething() { };
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('build')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }
}
