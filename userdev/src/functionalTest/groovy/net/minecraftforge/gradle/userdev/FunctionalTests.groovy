package net.minecraftforge.gradle.userdev

import net.minecraftforge.gradle.base.ForgeGradleTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends ForgeGradleTestSpecification {

    protected File codeFile

    @Override
    def setup() {
        codeFile = new File(testProjectDir, 'src/main/java/net/minecraftforge/gradle/userdev/FunctionalTests.java')
        codeFile.getParentFile().mkdirs()
    }

    def "a mod with userdev as dependency can run the patch task for that dependency"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraftforge:forge:1.19.2-43.1.34'
            }
        """

        when:
        def result = gradleRunner()
                .withArguments('--stacktrace', 'dependencyForge1.19.2-43.1.34Recompile')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "a mod with usedev as dependency and official mappings can compile through gradle"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraftforge:forge:1.19.2-43.1.34'
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

    def "the userdev runtime by default supports the build cache"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraftforge:forge:1.19.2-43.1.34'
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
        result.task(":dependencyForge1.19.2-43.1.34Recompile").outcome == TaskOutcome.SUCCESS

        when:
        new File(testProjectDir, 'build').deleteDir()
        result = runTask('--build-cache', 'build')

        then:
        result.task(":dependencyForge1.19.2-43.1.34Recompile").outcome == TaskOutcome.FROM_CACHE
    }

    def "the userdev runtime by default can compile a mod"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraftforge:forge:1.19.2-43.1.34'
            }
        """
        codeFile << """
            package net.minecraftforge.gradle.mcp;
            
            import com.mojang.logging.LogUtils;
            import net.minecraft.client.Minecraft;
            import net.minecraft.world.item.BlockItem;
            import net.minecraft.world.item.CreativeModeTab;
            import net.minecraft.world.item.Item;
            import net.minecraft.world.level.block.Block;
            import net.minecraft.world.level.block.Blocks;
            import net.minecraft.world.level.block.state.BlockBehaviour;
            import net.minecraft.world.level.material.Material;
            import net.minecraftforge.common.MinecraftForge;
            import net.minecraftforge.eventbus.api.IEventBus;
            import net.minecraftforge.eventbus.api.SubscribeEvent;
            import net.minecraftforge.fml.InterModComms;
            import net.minecraftforge.fml.common.Mod;
            import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
            import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
            import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
            import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
            import net.minecraftforge.event.server.ServerStartingEvent;
            import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
            import net.minecraftforge.registries.DeferredRegister;
            import net.minecraftforge.registries.ForgeRegistries;
            import net.minecraftforge.registries.RegistryObject;
            import org.slf4j.Logger;
            
            // The value here should match an entry in the META-INF/mods.toml file
            @Mod(FunctionalTests.MODID)
            public class FunctionalTests
            {
                // Define mod id in a common place for everything to reference
                public static final String MODID = "FunctionalTests";
                // Directly reference a slf4j logger
                private static final Logger LOGGER = LogUtils.getLogger();
                // Create a Deferred Register to hold Blocks which will all be registered under the "FunctionalTests" namespace
                public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
                // Create a Deferred Register to hold Items which will all be registered under the "FunctionalTests" namespace
                public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
            
                // Creates a new Block with the id "FunctionalTests:example_block", combining the namespace and path
                public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));
                // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
                public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
            
                public FunctionalTests()
                {
                    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
            
                    // Register the commonSetup method for modloading
                    modEventBus.addListener(this::commonSetup);
            
                    // Register the Deferred Register to the mod event bus so blocks get registered
                    BLOCKS.register(modEventBus);
                    // Register the Deferred Register to the mod event bus so items get registered
                    ITEMS.register(modEventBus);
            
                    // Register ourselves for server and other game events we are interested in
                    MinecraftForge.EVENT_BUS.register(this);
                }
            
                private void commonSetup(final FMLCommonSetupEvent event)
                {
                    // Some common setup code
                    LOGGER.info("HELLO FROM COMMON SETUP");
                    LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
                }
            
                // You can use SubscribeEvent and let the Event Bus discover methods to call
                @SubscribeEvent
                public void onServerStarting(ServerStartingEvent event)
                {
                    // Do something when the server starts
                    LOGGER.info("HELLO from server starting");
                }
            
                // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
                @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
                public static class ClientModEvents
                {
                    @SubscribeEvent
                    public static void onClientSetup(FMLClientSetupEvent event)
                    {
                        // Some client setup code
                        LOGGER.info("HELLO FROM CLIENT SETUP");
                        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
                    }
                }
            }
        """

        when:
        def result = runTask(':build')

        then:
        result.task(":build").outcome == TaskOutcome.SUCCESS

    }
}
