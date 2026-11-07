package com.lucky7.mod.client.render;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.ShellGameBlock;
import com.lucky7.mod.blockentity.ShellGameBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShellGameBlockEntityRenderer implements BlockEntityRenderer<ShellGameBlockEntity, ShellGameBlockEntityRenderer.ShellGameRenderState> {
    private static final Identifier GOLD = Lucky7Mod.id("textures/entity/gold.png");
    private static final Identifier DECK = Lucky7Mod.id("textures/entity/deck.png");
    private static final Identifier CABINET = Lucky7Mod.id("textures/entity/cabinet.png");
    private static final Identifier BUTTON = Lucky7Mod.id("textures/entity/button_red.png");
    private static final Identifier DIAMOND_TEX = Lucky7Mod.id("textures/reels/diamond.png");

    public static final class ShellGameRenderState extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public ShellGameBlockEntity.State gameState = ShellGameBlockEntity.State.IDLE;
        public int prizeCup = 1;
        public int chosenCup = -1;
        public float shuffleProgress = 1.0f;
    }

    public ShellGameBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public ShellGameRenderState createRenderState() {
        return new ShellGameRenderState();
    }

    @Override
    public void extractRenderState(ShellGameBlockEntity table, ShellGameRenderState state, float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPos, breakProgress);
        try {
            state.facing = table.getBlockState().getValue(ShellGameBlock.FACING);
        } catch (Exception ignored) {
            state.facing = Direction.NORTH;
        }
        state.gameState = table.getGameState();
        state.prizeCup = table.getPrizeCup();
        state.chosenCup = table.getChosenCup();
        state.shuffleProgress = table.getShuffleProgress();
    }

    @Override
    public void submit(ShellGameRenderState state, PoseStack matrices, SubmitNodeCollector vc, CameraRenderState camera) {
        int light = state.lightCoords;

        matrices.pushPose();
        matrices.translate(0.5, 0.0, 0.5);

        float rot = switch (state.facing) {
            case SOUTH -> 0.0f;
            case EAST -> 90.0f;
            case NORTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
        matrices.rotate(Axis.YP.rotationDegrees(rot));
        matrices.translate(-0.5, 0.0, -0.5);

        // 1. Luxury Casino Table (4 legs + mahogany apron + green velvet top + gold corner trims)
        solidBox(matrices, vc, CABINET, 0.08f, 0.00f, 0.08f, 0.20f, 0.68f, 0.20f, light);
        solidBox(matrices, vc, CABINET, 0.80f, 0.00f, 0.08f, 0.92f, 0.68f, 0.20f, light);
        solidBox(matrices, vc, CABINET, 0.08f, 0.00f, 0.80f, 0.20f, 0.68f, 0.92f, light);
        solidBox(matrices, vc, CABINET, 0.80f, 0.00f, 0.80f, 0.92f, 0.68f, 0.92f, light);

        // Table top & Gold trim
        solidBox(matrices, vc, GOLD, 0.04f, 0.68f, 0.04f, 0.96f, 0.76f, 0.96f, light);
        solidBox(matrices, vc, DECK, 0.08f, 0.75f, 0.08f, 0.92f, 0.77f, 0.92f, light);

        // 2. Three Animated Golden Cups (X: 0.24 = Left, 0.50 = Center, 0.76 = Right)
        float[] basePositions = {0.24f, 0.50f, 0.76f};
        ShellGameBlockEntity.State gs = state.gameState;
        float progress = state.shuffleProgress;

        for (int i = 0; i < 3; i++) {
            float cupX = basePositions[i];
            float cupY = 0.77f;
            float cupZ = 0.50f;

            // Lift cup during SHOWING, WON, or LOST
            if ((gs == ShellGameBlockEntity.State.SHOWING && i == state.prizeCup) ||
                (gs == ShellGameBlockEntity.State.WON && i == state.chosenCup) ||
                (gs == ShellGameBlockEntity.State.LOST && (i == state.chosenCup || i == state.prizeCup))) {
                cupY += 0.24f;
            }

            // 3D Orbiting Shuffle Animation (cups weave around each other in X and Z)
            if (gs == ShellGameBlockEntity.State.SHUFFLING) {
                float phase = progress * (float) Math.PI * 6.0f + i * 2.094f;
                cupX = 0.50f + Mth.cos(phase) * 0.25f;
                cupZ = 0.50f + Mth.sin(phase) * 0.16f;
            }

            // Draw Stepped Golden Cup + Ruby Handle
            matrices.pushPose();
            matrices.translate(cupX, cupY, cupZ);
            solidBox(matrices, vc, GOLD, -0.085f, 0.00f, -0.085f, 0.085f, 0.04f, 0.085f, light);
            solidBox(matrices, vc, GOLD, -0.075f, 0.04f, -0.075f, 0.075f, 0.18f, 0.075f, light);
            solidBox(matrices, vc, GOLD, -0.055f, 0.18f, -0.055f, 0.055f, 0.22f, 0.055f, light);
            solidBox(matrices, vc, BUTTON, -0.030f, 0.22f, -0.030f, 0.030f, 0.26f, 0.030f, 0x00F000F0);
            matrices.popPose();

            // Render Sparkling Diamond underneath prize cup when lifted
            if (i == state.prizeCup && (cupY > 0.85f)) {
                matrices.pushPose();
                matrices.translate(cupX, 0.78f, cupZ);
                float spinGem = (System.currentTimeMillis() % 3600) * 0.1f;
                matrices.rotate(Axis.YP.rotationDegrees(spinGem));
                solidBox(matrices, vc, DIAMOND_TEX, -0.045f, 0.0f, -0.045f, 0.045f, 0.09f, 0.045f, 0x00F000F0);
                matrices.popPose();
            }
        }

        matrices.popPose();
    }

    private void solidBox(PoseStack m, SubmitNodeCollector vc, Identifier tex, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        vc.submitCustomGeometry(m, RenderTypes.entityCutout(tex), (pose, b) -> {
            quadFace(pose, b, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            quadFace(pose, b, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
            quadFace(pose, b, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            quadFace(pose, b, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
            quadFace(pose, b, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
            quadFace(pose, b, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        });
    }

    private static void quadFace(PoseStack.Pose pose, VertexConsumer b, int light, float nx, float ny, float nz, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        b.addVertex(pose, x0, y0, z0).setColor(255, 255, 255, 255).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        b.addVertex(pose, x1, y1, z1).setColor(255, 255, 255, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        b.addVertex(pose, x2, y2, z2).setColor(255, 255, 255, 255).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        b.addVertex(pose, x3, y3, z3).setColor(255, 255, 255, 255).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
