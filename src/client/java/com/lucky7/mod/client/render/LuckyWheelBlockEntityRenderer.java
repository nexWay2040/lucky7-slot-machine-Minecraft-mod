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
    private static final Identifier GOLD = Lucky7Mod.id("textures/entity/gold.png");
    private static final Identifier CABINET = Lucky7Mod.id("textures/entity/cabinet.png");
    private static final Identifier BUTTON = Lucky7Mod.id("textures/entity/button_red.png");
    private static final Identifier KNOB = Lucky7Mod.id("textures/entity/knob.png");

    // 12 Rich Casino Sector Colors (Ruby, Gold, Emerald, Royal Purple, Diamond Cyan, Dark Velvet)
    private static final int[] SECTOR_COLORS = {
        0xFF20D8F0, // 0: Diamond Cyan
        0xFFE8B828, // 1: Gold
        0xFF9830E8, // 2: Royal Purple
        0xFFE02838, // 3: Ruby Red
        0xFF28D058, // 4: Emerald Green
        0xFF242432, // 5: Dark Obsidian
        0xFFE8B828, // 6: Gold
        0xFF28D058, // 7: Emerald Green
        0xFFE02838, // 8: Ruby Red
        0xFF20D8F0, // 9: Cyan
        0xFF9830E8, // 10: Purple
        0xFFFFD700  // 11: Royal Gold
    };

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

        // 1. Luxury Casino Pedestal Stand
        solidBox(matrices, vc, GOLD, 0.14f, 0.00f, 0.18f, 0.86f, 0.06f, 0.82f, light);
        solidBox(matrices, vc, CABINET, 0.18f, 0.06f, 0.22f, 0.82f, 0.22f, 0.78f, light);
        // Twin support pillars
        solidBox(matrices, vc, CABINET, 0.36f, 0.22f, 0.34f, 0.64f, 1.32f, 0.50f, light);
        solidBox(matrices, vc, GOLD, 0.34f, 0.22f, 0.32f, 0.66f, 0.28f, 0.52f, light);
        // Top arch holding the ticker
        solidBox(matrices, vc, GOLD, 0.42f, 1.32f, 0.36f, 0.58f, 1.92f, 0.52f, light);
        solidBox(matrices, vc, GOLD, 0.42f, 1.84f, 0.36f, 0.58f, 1.92f, 0.66f, light);

        // 2. Rotating 12-Sector Wheel Disc (Center at X: 0.5, Y: 1.25, Z: 0.56)
        matrices.pushPose();
        matrices.translate(0.5, 1.25, 0.56);
        matrices.rotate(Axis.ZP.rotationDegrees(-state.angle));

        renderWheelDisc(matrices, vc, light);
        matrices.popPose();

        // 3. Golden Ticker Clapper at Top (Y: 1.84, Z: 0.62)
        matrices.pushPose();
        matrices.translate(0.5, 1.84, 0.62);
        if (state.spinning) {
            float flutter = Mth.sin((System.currentTimeMillis() % 360) * 0.08f) * 14.0f;
            matrices.rotate(Axis.ZP.rotationDegrees(flutter));
        }
        solidBox(matrices, vc, BUTTON, -0.035f, -0.06f, -0.025f, 0.035f, 0.03f, 0.025f, 0x00F000F0);
        solidBox(matrices, vc, GOLD, -0.016f, -0.14f, -0.016f, 0.016f, -0.05f, 0.016f, 0x00F000F0);
        matrices.popPose();

        matrices.popPose();
    }

    private void renderWheelDisc(PoseStack matrices, SubmitNodeCollector vc, int light) {
        float radius = 0.56f;

        // Back plate of wheel
        solidBox(matrices, vc, CABINET, -0.38f, -0.38f, -0.04f, 0.38f, 0.38f, -0.005f, light);

        // 12 Colored Sectors + Rim Pegs
        vc.submitCustomGeometry(matrices, RenderTypes.entityCutout(GOLD), (pose, b) -> {
            for (int i = 0; i < 12; i++) {
                float a1 = (float) Math.toRadians(i * 30.0);
                float a2 = (float) Math.toRadians((i + 1) * 30.0);

                float x1 = Mth.sin(a1) * radius;
                float y1 = Mth.cos(a1) * radius;
                float x2 = Mth.sin(a2) * radius;
                float y2 = Mth.cos(a2) * radius;

                int color = SECTOR_COLORS[i % SECTOR_COLORS.length];
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                b.addVertex(pose, 0.0f, 0.0f, 0.01f).setColor(red, green, blue, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
                b.addVertex(pose, x1, y1, 0.01f).setColor(red, green, blue, 255).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
                b.addVertex(pose, x2, y2, 0.01f).setColor(red, green, blue, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
                b.addVertex(pose, x2, y2, 0.01f).setColor(red, green, blue, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(pose, 0, 0, 1);
            }
        });

        // 12 Golden Rim Pegs around the circumference
        for (int i = 0; i < 12; i++) {
            matrices.pushPose();
            matrices.rotate(Axis.ZP.rotationDegrees(i * 30.0f));
            solidBox(matrices, vc, GOLD, -0.025f, radius - 0.03f, -0.02f, 0.025f, radius + 0.02f, 0.035f, 0x00F000F0);
            matrices.popPose();
        }

        // Center Golden Hub Cap & Ruby Jewel
        solidBox(matrices, vc, GOLD, -0.09f, -0.09f, -0.02f, 0.09f, 0.09f, 0.035f, light);
        solidBox(matrices, vc, KNOB, -0.05f, -0.05f, 0.02f, 0.05f, 0.05f, 0.055f, 0x00F000F0);
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
