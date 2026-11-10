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
    private static final Identifier MAHOGANY = Lucky7Mod.id("textures/entity/mahogany_wood.png");
    private static final Identifier TABLE_FELT = Lucky7Mod.id("textures/entity/table_felt.png");
    private static final Identifier CUP_GOLD = Lucky7Mod.id("textures/entity/cup_gold_ornate.png");
    private static final Identifier WHEEL_ARROW = Lucky7Mod.id("textures/entity/wheel_arrow.png");
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

        // 1. Luxury Mahogany Casino Table Legs + Apron + Emerald Felt Mat
        solidBox(matrices, vc, MAHOGANY, 0.09f, 0.00f, 0.09f, 0.21f, 0.68f, 0.21f, light);
        solidBox(matrices, vc, MAHOGANY, 0.79f, 0.00f, 0.09f, 0.91f, 0.68f, 0.21f, light);
        solidBox(matrices, vc, MAHOGANY, 0.09f, 0.00f, 0.79f, 0.21f, 0.68f, 0.91f, light);
        solidBox(matrices, vc, MAHOGANY, 0.79f, 0.00f, 0.79f, 0.91f, 0.68f, 0.91f, light);

        // Mahogany side frame & gold trim bevel
        solidBox(matrices, vc, MAHOGANY, 0.06f, 0.62f, 0.06f, 0.94f, 0.75f, 0.94f, light);
        solidBox(matrices, vc, CUP_GOLD, 0.04f, 0.72f, 0.04f, 0.96f, 0.76f, 0.96f, light);
        // Emerald felt mat flush on top!
        solidBox(matrices, vc, TABLE_FELT, 0.08f, 0.755f, 0.08f, 0.92f, 0.772f, 0.92f, light);

        // 2. Three Ornate Golden Thimble Cups (X: 0.23 = Cup 1, 0.50 = Cup 2, 0.77 = Cup 3)
        float[] basePositions = {0.23f, 0.50f, 0.77f};
        ShellGameBlockEntity.State gs = state.gameState;
        float progress = state.shuffleProgress;

        for (int i = 0; i < 3; i++) {
            float cupX = basePositions[i];
            float cupY = 0.772f;
            float cupZ = 0.50f;

            // Lift cup smoothly during SHOWING, WON, or LOST
            if ((gs == ShellGameBlockEntity.State.SHOWING && i == state.prizeCup) ||
                (gs == ShellGameBlockEntity.State.WON && i == state.chosenCup) ||
                (gs == ShellGameBlockEntity.State.LOST && (i == state.chosenCup || i == state.prizeCup))) {
                cupY += 0.24f;
            }

            // Smooth 3D weaving shuffle orbit
            if (gs == ShellGameBlockEntity.State.SHUFFLING) {
                float phase = progress * (float) Math.PI * 6.0f + i * 2.094f;
                cupX = 0.50f + Mth.cos(phase) * 0.26f;
                cupZ = 0.50f + Mth.sin(phase) * 0.16f;
            }

            // Sculpted Stepped Goblet Thimble (wide flared rim, tapered fluted body, gold/ruby finial)
            matrices.pushPose();
            matrices.translate(cupX, cupY, cupZ);
            solidBox(matrices, vc, CUP_GOLD, -0.095f, 0.000f, -0.095f, 0.095f, 0.045f, 0.095f, light);
            solidBox(matrices, vc, CUP_GOLD, -0.080f, 0.045f, -0.080f, 0.080f, 0.140f, 0.080f, light);
            solidBox(matrices, vc, CUP_GOLD, -0.064f, 0.140f, -0.064f, 0.064f, 0.205f, 0.064f, light);
            solidBox(matrices, vc, CUP_GOLD, -0.042f, 0.205f, -0.042f, 0.042f, 0.232f, 0.042f, light);
            solidBox(matrices, vc, WHEEL_ARROW, -0.024f, 0.232f, -0.024f, 0.024f, 0.262f, 0.024f, 0x00F000F0);
            matrices.popPose();

            // Render Sparkling 3D Crossed-Sprite Diamond Crystal under prize cup when lifted
            if (i == state.prizeCup && (cupY > 0.85f)) {
                matrices.pushPose();
                matrices.translate(cupX, 0.80f, cupZ);
                float spinGem = (System.currentTimeMillis() % 3600) * 0.12f;
                matrices.rotate(Axis.YP.rotationDegrees(spinGem));
                renderDiamondSprite(matrices, vc);
                matrices.popPose();
            }
        }

        matrices.popPose();
    }

    private void renderDiamondSprite(PoseStack m, SubmitNodeCollector vc) {
        vc.submitCustomGeometry(m, RenderTypes.entityCutout(DIAMOND_TEX), (pose, b) -> {
            float r = 0.075f;
            float h = 0.145f;
            // Crossed double-sided diamond crystal quads (crisp cutout!)
            quadFace(pose, b, 0x00F000F0, 0, 0, 1, -r, 0, 0, r, 0, 0, r, h, 0, -r, h, 0);
            quadFace(pose, b, 0x00F000F0, 0, 0, -1, r, 0, 0, -r, 0, 0, -r, h, 0, r, h, 0);
            quadFace(pose, b, 0x00F000F0, 1, 0, 0, 0, 0, r, 0, 0, -r, 0, h, -r, 0, h, r);
            quadFace(pose, b, 0x00F000F0, -1, 0, 0, 0, 0, -r, 0, 0, r, 0, h, r, 0, h, -r);
        });
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
