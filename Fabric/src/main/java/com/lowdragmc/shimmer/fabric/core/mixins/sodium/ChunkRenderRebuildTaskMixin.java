package com.lowdragmc.shimmer.fabric.core.mixins.sodium;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.client.postprocessing.PostProcessing;
import com.lowdragmc.shimmer.core.IRenderChunk;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote ChunkRenderRebuildTaskMixin
 */
@Mixin(ChunkRenderRebuildTask.class)
public abstract class ChunkRenderRebuildTaskMixin {
    @Shadow(remap = false) @Final private RenderSection render;
    ImmutableList.Builder<ColorPointLight> lights;

    /* The above redirect inject causes a crash outside the dev env. This does not */
    @Inject(method = "performBuild", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectCompile(ChunkBuildContext buildContext, CancellationSource cancellationSource, CallbackInfoReturnable<ChunkBuildResult> cir, ChunkRenderData.Builder renderData, VisGraph occluder, ChunkRenderBounds.Builder bounds, ChunkBuildBuffers buffers, BlockRenderCache cache, WorldSlice slice, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockPos.MutableBlockPos blockPos, BlockPos.MutableBlockPos modelOffset, BlockRenderContext context, int y, int z, int x, BlockState blockState) {
        var blockstate = context.state();
        FluidState fluidstate = blockstate.getFluidState();
        if (LightManager.INSTANCE.isBlockHasLight(blockstate.getBlock(), fluidstate)) {
            ColorPointLight light = LightManager.INSTANCE.getBlockStateLight(slice, new BlockPos(x, y, z), blockstate, fluidstate);
            if (light != null) {
                lights.add(light);
            }
        }
        PostProcessing.setupBloom(blockstate, fluidstate);
    }


    @Inject(method = "performBuild", at = @At(value = "HEAD"), remap = false)
    private void injectCompilePre(ChunkBuildContext buildContext,
                                  CancellationSource cancellationSource,
                                  CallbackInfoReturnable<ChunkBuildResult> cir) {
        lights = ImmutableList.builder();
    }

    @Inject(method = "performBuild", at = @At(value = "RETURN"), remap = false)
    private void injectCompilePost(ChunkBuildContext buildContext,
                                   CancellationSource cancellationSource,
                                   CallbackInfoReturnable<ChunkBuildResult> cir) {
        if (this.render instanceof IRenderChunk renderChunk) {
            renderChunk.setShimmerLights(lights.build());
        }
        PostProcessing.cleanBloom();
    }
}
