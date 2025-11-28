
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.fml.common.Mod;
                
                @Mod("neogradle_test")
                public class FunctionalTests {
                    public FunctionalTests() {
                        System.out.println(Minecraft.class.toString());
                    }
                }
            