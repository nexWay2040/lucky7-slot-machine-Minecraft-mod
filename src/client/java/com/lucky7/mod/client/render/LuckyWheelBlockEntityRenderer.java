package com.lucky7.mod.client.render;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.LuckyWheelBlock;
import com.lucky7.mod.blockentity.LuckyWheelBlockEntity;
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

public class LuckyWheelBlockEntityRenderer implements BlockEntityRenderer<LuckyWheelBlockEntity, LuckyWheelBlockEntityRenderer.WheelRenderState> {
    private static final Identifier MAHOGANY = Lucky7Mod.id("textures/entity/mahogany_wood.png");
    private static final Identifier GOLD = Lucky7Mod.id("textures/entity/cup_gold_ornate.png");
    private static final Identifier WHEEL_FACE = Lucky7Mod.id("textures/entity/wheel_face.png");
    private static final Identifier WHEEL_ARROW = Lucky7Mod.id("textures/entity/wheel_arrow.png");

    public static final class WheelRenderState extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float angle = 0.0f;
        public boolean spinning = false;
    }

    public LuckyWheelBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public WheelRenderState createRenderState() {
        return new WheelRenderState();
    }

    @Override
    public void extractRenderState(LuckyWheelBlockEntity wheel, WheelRenderState state, float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(wheel, state, partialTicks, cameraPos, breakProgress);
        try {
            state.facing = wheel.getBlockState().getValue(LuckyWheelBlock.FACING);
        } catch (Exception ignored) {
            state.facing = Direction.NORTH;
        }
        state.angle = wheel.getAngle();
        state.spinning = wheel.isSpinning();
    }

    @Override
    public void submit(WheelRenderState state, PoseStack matrices, SubmitNodeCollector vc, CameraRenderState camera) {
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

        // 1. Luxury Mahogany & Gold Casino Stand
        solidBox(matrices, vc, GOLD, 0.14f, 0.00f, 0.16f, 0.86f, 0.05f, 0.84f, light);
        solidBox(matrices, vc, MAHOGANY, 0.18f, 0.05f, 0.20f, 0.82f, 0.20f, 0.80f, light);
        // Sturdy twin wooden column & golden collar
        solidBox(matrices, vc, MAHOGANY, 0.37f, 0.20f, 0.34f, 0.63f, 1.32f, 0.50f, light);
        solidBox(matrices, vc, GOLD, 0.34f, 0.20f, 0.32f, 0.66f, 0.26f, 0.52f, light);
        // Top arched bracket for the pointer
        solidBox(matrices, vc, MAHOGANY, 0.42f, 1.32f, 0.36f, 0.58f, 1.92f, 0.52f, light);
        solidBox(matrices, vc, GOLD, 0.41f, 1.84f, 0.35f, 0.59f, 1.93f, 0.68f, light);

        // 2. Rotating 12-Sector UV-Mapped Fortune Wheel Disc (Center X: 0.5, Y: 1.25, Z: 0.56)
        matrices.pushPose();
        matrices.translate(0.5, 1.25, 0.56);
        // Positive Z rotation aligns sector i (+i*30 deg clockwise on wheel_face) directly under top pointer!
        matrices.rotate(Axis.ZP.rotationDegrees(state.angle));

        renderTexturedWheelDisc(matrices, vc, light);
        matrices.popPose();

        // 3. Top Red & Gold Ticker Clapper Pointer at 12 o'clock (Y: 1.84, Z: 0.64)
        matrices.pushPose();
        matrices.translate(0.5, 1.84, 0.64);
        if (state.spinning) {
            float flutter = Mth.sin((System.currentTimeMillis() % 360) * 0.09f) * 12.0f;
            matrices.rotate(Axis.ZP.rotationDegrees(flutter));
        }
        solidBox(matrices, vc, GOLD, -0.045f, -0.04f, -0.025f, 0.045f, 0.03f, 0.025f, 0x00F000F0);
        solidBox(matrices, vc, WHEEL_ARROW, -0.025f, -0.15f, -0.018f, 0.025f, -0.03f, 0.018f, 0x00F000F0);
        matrices.popPose();

        matrices.popPose();
    }

    private void renderTexturedWheelDisc(PoseStack matrices, SubmitNodeCollector vc, int light) {
        final float radius = 0.58f;
        final float zFront = 0.032f;
        final float zBack = -0.032f;
        final int segments = 36;

        // Front textured face using high-res 512x512 wheel_face.png!
        vc.submitCustomGeometry(matrices, RenderTypes.entityCutout(WHEEL_FACE), (pose, b) -> {
            for (int i = 0; i < segments; i++) {
                double a1 = (i * 2.0 * Math.PI) / segments;
                double a2 = ((i + 1) * 2.0 * Math.PI) / segments;

                float x1 = (float) Math.sin(a1) * radius;
                float y1 = (float) Math.cos(a1) * radius;
                float x2 = (float) Math.sin(a2) * radius;
                float y2 = (float) Math.cos(a2) * radius;

                // Map circular XY cleanly onto wheel_face.png UV [0..1] (Y down in texture: v = 0.5 - y/(2*R))
                float u1 = 0.5f + (x1 / (2.0f * radius));
                float v1 = 0.5f - (y1 / (2.0f * radius));
                float u2 = 0.5f + (x2 / (2.0f * radius));
                float v2 = 0.5f - (y2 / (2.0f * radius));

                // Outward-facing CCW quad degenerate fan (0 -> 2 -> 1 -> 1 so front +Z faces camera cleanly)
                b.addVertex(pose, 0.0f, 0.0f, zFront).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
                b.addVertex(pose, x2, y2, zFront).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
                b.addVertex(pose, x1, y1, zFront).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
                b.addVertex(pose, x1, y1, zFront).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
            }
        });

        // Mahogany wood back face (-Z)
        vc.submitCustomGeometry(matrices, RenderTypes.entityCutout(MAHOGANY), (pose, b) -> {
            for (int i = 0; i < segments; i++) {
                double a1 = (i * 2.0 * Math.PI) / segments;
                double a2 = ((i + 1) * 2.0 * Math.PI) / segments;

                float x1 = (float) Math.sin(a1) * radius;
                float y1 = (float) Math.cos(a1) * radius;
                float x2 = (float) Math.sin(a2) * radius;
                float y2 = (float) Math.cos(a2) * radius;

                float u1 = 0.5f + (x1 / (2.0f * radius));
                float v1 = 0.5f - (y1 / (2.0f * radius));
                float u2 = 0.5f + (x2 / (2.0f * radius));
                float v2 = 0.5f - (y2 / (2.0f * radius));

                b.addVertex(pose, 0.0f, 0.0f, zBack).setColor(255, 255, 255, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
                b.addVertex(pose, x1, y1, zBack).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
                b.addVertex(pose, x2, y2, zBack).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
                b.addVertex(pose, x2, y2, zBack).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
            }
        });

        // Gold outer rim thickness connecting front and back
        vc.submitCustomGeometry(matrices, RenderTypes.entityCutout(GOLD), (pose, b) -> {
            for (int i = 0; i < segments; i++) {
                double a1 = (i * 2.0 * Math.PI) / segments;
                double a2 = ((i + 1) * 2.0 * Math.PI) / segments;

                float x1 = (float) Math.sin(a1) * radius;
                float y1 = (float) Math.cos(a1) * radius;
                float x2 = (float) Math.sin(a2) * radius;
                float y2 = (float) Math.cos(a2) * radius;

                float nx = (float) Math.sin((a1 + a2) * 0.5);
                float ny = (float) Math.cos((a1 + a2) * 0.5);

                b.addVertex(pose, x1, y1, zBack).setColor(255, 255, 255, 255).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, 0);
                b.addVertex(pose, x1, y1, zFront).setColor(255, 255, 255, 255).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, 0);
                b.addVertex(pose, x2, y2, zFront).setColor(255, 255, 255, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, 0);
                b.addVertex(pose, x2, y2, zBack).setColor(255, 255, 255, 255).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, 0);
            }
        });

        // 12 brass divider pegs right between every sector (at +15 deg offset!)
        for (int i = 0; i < 12; i++) {
            matrices.pushPose();
            matrices.rotate(Axis.ZP.rotationDegrees(i * 30.0f + 15.0f));
            solidBox(matrices, vc, GOLD, -0.018f, radius - 0.03f, zFront - 0.01f, 0.018f, radius + 0.025f, zFront + 0.022f, 0x00F000F0);
            matrices.popPose();
        }

        // Center Golden Hub Cap
        solidBox(matrices, vc, GOLD, -0.08f, -0.08f, zFront - 0.01f, 0.08f, 0.08f, zFront + 0.028f, light);
        solidBox(matrices, vc, WHEEL_ARROW, -0.045f, -0.045f, zFront + 0.022f, 0.045f, 0.045f, zFront + 0.045f, 0x00F000F0);
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
