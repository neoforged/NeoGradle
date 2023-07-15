package net.neoforged.gradle.mcp

import net.minecraftforge.trainingwheels.gradle.functional.SimpleTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends SimpleTestSpecification {

    static {
        DEBUG = false
        DEBUG_PROJECT_DIR = false
    }

    protected File codeFile

    @Override
    def setup() {
        codeFile = new File(testProjectDir, 'src/main/java/net/minecraftforge/gradle/mcp/FunctionalTests.java')
        codeFile.getParentFile().mkdirs()
    }

    def "a mod with mcp as dependency can run the patch task for that dependency"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            mcp {
                mcpConfigVersion = '1.19-20220627.091056'
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client'
            }
        """

        when:
        def result = gradleRunner()
                .withArguments('--stacktrace', ':dependencyMcpClient1.19-20220627.091056Patch')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "a mod with mcp as dependency and official mappings can compile through gradle"() {

        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            mcp {
                mcpConfigVersion = '1.19-20220627.091056'
            }
            
            minecraft {
                accessTransformers {
                    entry "public net.minecraft.client.Minecraft LOGGER # searchRegistry"
                }
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client:1.19-20220627.091056'
            }
        """
        codeFile << """
            package net.neoforged.gradle.mcp;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.LOGGER.getClass().toString());
                }
            }
        """

        when:
        def result = runTask('build')

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "the mcp runtime by default supports the build cache"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            mcp {
                mcpConfigVersion = '1.19-20220627.091056'
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client'
            }
        """
        codeFile << """
            package net.neoforged.gradle.mcp;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.getInstance().getClass().toString());
                }
            }
        """

        when:
        def result = runTask('--build-cache', ':dependencyMcpClient1.19-20220627.091056SelectRawArtifact')

        then:
        result.task(":dependencyMcpClient1.19-20220627.091056Recompile").outcome == TaskOutcome.SUCCESS

        when:
        new File(testProjectDir, 'build').deleteDir()
        result = runTask('--build-cache', ':dependencyMcpClient1.19-20220627.091056SelectRawArtifact')

        then:
        result.task(":dependencyMcpClient1.19-20220627.091056Recompile").outcome == TaskOutcome.FROM_CACHE
    }
}
