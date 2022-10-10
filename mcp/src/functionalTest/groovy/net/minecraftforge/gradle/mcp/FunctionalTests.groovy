package net.minecraftforge.gradle.mcp

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import net.minecraftforge.gradle.base.ForgeGradleTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends ForgeGradleTestSpecification {


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
                id 'net.minecraftforge.gradle.mcp'
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
                .withArguments('--stacktrace', 'dependencyClientPatch')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "a mod with mcp as dependency and official mappings can compile through gradle"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
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
            package net.minecraftforge.gradle.mcp;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.getInstance().getClass().toString());
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
                id 'net.minecraftforge.gradle.mcp'
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
            package net.minecraftforge.gradle.mcp;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.getInstance().getClass().toString());
                }
            }
        """

        when:
        def result = runTask('--build-cache', 'build')

        then:
        result.task(":dependencyClient1.19-20220627.091056Recompile").outcome == TaskOutcome.SUCCESS

        when:
        new File(testProjectDir, 'build').deleteDir()
        result = runTask('--build-cache', 'build')

        then:
        result.task(":dependencyClient1.19-20220627.091056Recompile").outcome == TaskOutcome.FROM_CACHE
    }
}
