package com.example.examplemod.mixin;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.level.block.RedstoneLampBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedstoneLampBlock.class)
public class TestMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        ExampleMod.LOGGER.info("Hello from RedstoneLampBlock constructor!");
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"))
    private void onNeighborChanged(CallbackInfo ci) {
        ExampleMod.LOGGER.info("Hello from RedstoneLampBlock neighbor changed!");
        System.err.println("Hello from RedstoneLampBlock neighbor changed!");
    }
}
