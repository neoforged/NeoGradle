
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            