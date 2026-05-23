package net.neoforged.gradle.userdev

import groovy.json.JsonSlurper
import net.neoforged.gradle.userdev.constants.TestConstants
import net.neoforged.gradle.userdev.extensions.TestExtensions
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
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json",
                """\
                {
                    "net/minecraft/client/Minecraft": [
                        "com/example/examplemod/MyInjectedInterface"
                    ]
                }
                """.stripIndent())
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
            it.tasks('compileJava')
            it.stacktrace()
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from a file with the decompiler disabled"() {
        given:
        def project = create("userdev_supports_iis_from_file_no_decompile", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json",
                    """\
                {
                    "net/minecraft/client/Minecraft": [
                        "com/example/examplemod/MyInjectedInterface"
                    ]
                }
                """.stripIndent())
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
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
            it.stacktrace()
            it.debug()
        }

        then:
        initialRun.task(":neoFormRecompile") == null
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from a file after the dependencies block"() {
        given:
        def project = create("userdev_supports_iis_from_file_decompiler_disabled", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
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
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from multiple files"() {
        given:
        def project = create("userdev_supports_iis_in_multiple_distinctly_named_files", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
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
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from multiple files named the same in different directories"() {
        given:
        def project = create("userdev_supports_iis_in_files_named_the_same", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
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
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime includes all recompile dependencies when using JST"() {
        given:
        def project = create("userdev_includes_all_recompile_dependencies", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
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
            it.tasks('compileJava')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
        !initialRun.output.contains("Failed to create binary representation for type")
    }

    def "the userdev runtime supports loading iis from inner interfaces from a file with the decompiler disabled"() {
        given:
        def project = create("userdev_supports_iis_inner_from_file_no_decompile", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json",
                    """\
                {
                    "net/minecraft/client/Minecraft": [
                        "com/example/examplemod/MyInjectedInterface\$Inner"
                    ]
                }
                """.stripIndent())
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
                
                    public interface Inner {
                        default void doSomething() { };
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
            it.stacktrace()
        }

        then:
        initialRun.task(":neoFormRecompile") == null
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from generic from a file with the decompiler disabled"() {
        given:
        def project = create("userdev_supports_iis_inner_from_file_no_decompile", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/iis.json",
                    """\
                {
                    "net/minecraft/client/Minecraft": [
                        "com/example/examplemod/MyInjectedInterface<net.minecraft.client.Minecraft>"
                    ]
                }
                """.stripIndent())
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
                
                public interface MyInjectedInterface<T> {
                    default T doSomething() {
                        return null;
                    };
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def initialRun = project.run {
            it.tasks('compileJava')
            it.stacktrace()
        }

        then:
        initialRun.task(":neoFormRecompile") == null
        initialRun.task(":compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports loading iis from dependencies and exposing them"() {
        given:
        def consumedProject = create("u_e_iis_publisher", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})

            group = "n.n.n.u.t.p"
            version = "1.0.0"
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
           
            minecraft.interfaceInjections.file rootProject.file('src/main/resources/META-INF/iis.json')
            
            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
            """)
            it.withMod("Publisher")
            it.file("src/main/java/com/example/examplemod/MyInjectedInterface.java", """
                package com.example.examplemod;
                
                public interface MyInjectedInterface {
                    default void doSomething() { };
                }
            """)
            it.file("src/main/resources/META-INF/iis.json",
                    """\
                {
                    "net/minecraft/client/Minecraft": [
                        "com/example/examplemod/MyInjectedInterface"
                    ]
                }
                """.stripIndent())
            it.plugin("maven-publish")
        })

        when:
        def publishRun = consumedProject.run {
            it.tasks("publishToMavenLocal")
            it.stacktrace()
        }

        then:
        publishRun.task(":publishToMavenLocal").outcome == TaskOutcome.SUCCESS

        and:
        def consumingProject = create("u_e_iis_consuming", {
            it.withRun("""
            group = "n.n.n.u.t.g"
            version = "1.0.0"
            
            repositories {
                mavenLocal()
            }
            
            dependencies {
                implementation 'n.n.n.u.t.p:u_e_iis_publisher:1.0.0'
            }
            
            interfaceInjections {
                consumeApi 'n.n.n.u.t.p:u_e_iis_publisher:1.0.0'
            }
            
            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
            """)
            it.plugin("maven-publish")
        })

        when:
        def consumerRun = consumingProject.run {
            it.run()
            it.stacktrace()
        }

        then:
        consumerRun.checkModLoading()

        when:
        def consumerPublish = consumingProject.run {
            it.tasks("publishToMavenLocal")
        }

        then:
        consumerPublish.task(":publishToMavenLocal").outcome == TaskOutcome.SUCCESS
        def moduleJson = consumerPublish.file("build/publications/maven/module.json")
        def slurper = new JsonSlurper()
        def module = slurper.parse(moduleJson)
        module.variants.find(it -> it.name == "interfaceInjectionElements").dependencies.size() > 0
        module.variants.find(it -> it.name == "interfaceInjectionElements").files.size() > 0
    }
}
