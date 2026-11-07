package com.lucky7.mod.client.render;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.Lucky7Block;
import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.game.reels.ReelLayout;
import com.lucky7.mod.game.reels.ReelSymbol;
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

public final class Lucky7BlockEntityRenderer implements BlockEntityRenderer<Lucky7BlockEntity, Lucky7BlockEntityRenderer.SlotMachineRenderState> {
    private static final Identifier CABINET = Lucky7Mod.id("textures/entity/cabinet.png");
    private static final Identifier GOLD = Lucky7Mod.id("textures/entity/gold.png");
    private static final Identifier INTERIOR = Lucky7Mod.id("textures/entity/interior.png");
    private static final Identifier GLASS = Lucky7Mod.id("textures/entity/glass.png");
    private static final Identifier MARQUEE = Lucky7Mod.id("textures/entity/marquee.png");
    private static final Identifier PAYTABLE = Lucky7Mod.id("textures/entity/paytable.png");
    private static final Identifier DECK = Lucky7Mod.id("textures/entity/deck.png");
    private static final Identifier BUTTON = Lucky7Mod.id("textures/entity/button_red.png");
    private static final Identifier KNOB = Lucky7Mod.id("textures/entity/knob.png");
    private static final Identifier REEL_TAPE = Lucky7Mod.id("textures/entity/reel_tape.png");

    private static final Identifier[] SYMBOLS = new Identifier[18];
    static {
        ReelSymbol[] values = ReelSymbol.values();
        for (int i = 0; i < 18; i++) {
            String name = (i < values.length) ? values[i].id() : "seven";
            SYMBOLS[i] = Lucky7Mod.id("textures/reels/" + name + ".png");
        }
    }

    public static final class SlotMachineRenderState extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public final int[] reels = new int[]{0, 1, 2};
        public boolean spinning = false;
        public float progress = 1.0f;
        public boolean isFever = false;
    }

    public Lucky7BlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public SlotMachineRenderState createRenderState() {
        return new SlotMachineRenderState();
    }

    @Override
    public void extractRenderState(Lucky7BlockEntity be, SlotMachineRenderState state, float partialTicks, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPos, breakProgress);
        try {
            state.facing = be.getBlockState().getValue(Lucky7Block.FACING);
        } catch (Exception ignored) {
            state.facing = Direction.NORTH;
        }
        int[] r = be.reels();
        if (r != null && r.length == 3) {
            state.reels[0] = r[0];
            state.reels[1] = r[1];
            state.reels[2] = r[2];
        }
        state.spinning = be.isSpinning();
        state.progress = be.clientProgress(partialTicks);
        state.isFever = be.isFeverMode();
    }

    @Override
    public void submit(SlotMachineRenderState state, PoseStack matrices, SubmitNodeCollector vc, CameraRenderState camera) {
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

        // 1. BASE & PLINTH (Y: 0.00 -> 0.28)
        solidBox(matrices, vc, CABINET, 0.04f, 0.00f, 0.06f, 0.96f, 0.28f, 0.94f, light);
        solidBox(matrices, vc, GOLD, 0.02f, 0.00f, 0.04f, 0.98f, 0.05f, 0.96f, light);
        solidBox(matrices, vc, GOLD, 0.24f, 0.06f, 0.88f, 0.76f, 0.16f, 0.98f, light);
        solidBox(matrices, vc, INTERIOR, 0.26f, 0.08f, 0.89f, 0.74f, 0.14f, 0.96f, light);

        // 2. LOWER ART PANEL & DOOR (Y: 0.28 -> 0.92)
        solidBox(matrices, vc, CABINET, 0.06f, 0.28f, 0.08f, 0.94f, 0.92f, 0.92f, light);
        solidBox(matrices, vc, GOLD, 0.12f, 0.32f, 0.90f, 0.88f, 0.88f, 0.93f, light);
        solidBox(matrices, vc, PAYTABLE, 0.14f, 0.34f, 0.91f, 0.86f, 0.86f, 0.935f, light);

        // 3. SLANTED CONTROL DECK & BUTTONS (Y: 0.92 -> 1.10)
        solidBox(matrices, vc, DECK, 0.06f, 0.92f, 0.08f, 0.94f, 1.08f, 1.07f, light);
        solidBox(matrices, vc, GOLD, 0.05f, 1.04f, 1.05f, 0.95f, 1.09f, 1.08f, light);

        float btnHeight = 0.055f;
        if (state.spinning && state.progress < 0.20f) {
            float press = Mth.sin(state.progress / 0.20f * (float) Math.PI);
            btnHeight = 0.055f - press * 0.035f;
        }
        solidBox(matrices, vc, GOLD, 0.20f, 1.08f, 0.96f, 0.42f, 1.095f, 1.05f, light);
        solidBox(matrices, vc, CABINET, 0.215f, 1.08f, 0.965f, 0.405f, 1.08f + btnHeight - 0.002f, 1.045f, light);
        solidBox(matrices, vc, BUTTON, 0.22f, 1.08f, 0.97f, 0.40f, 1.08f + btnHeight, 1.04f, light);

        solidBox(matrices, vc, GOLD, 0.68f, 1.08f, 0.96f, 0.80f, 1.11f, 1.03f, light);
        solidBox(matrices, vc, INTERIOR, 0.72f, 1.09f, 0.97f, 0.76f, 1.11f, 1.02f, light);

        // 4. REEL CHAMBER & GLASS HOUSING (Y: 1.14 -> 1.76)
        solidBox(matrices, vc, CABINET, 0.06f, 1.14f, 0.08f, 0.94f, 1.76f, 0.18f, light);
        solidBox(matrices, vc, INTERIOR, 0.12f, 1.16f, 0.18f, 0.88f, 1.74f, 0.20f, light);
        solidBox(matrices, vc, CABINET, 0.06f, 1.14f, 0.18f, 0.14f, 1.76f, 0.92f, light);
        solidBox(matrices, vc, CABINET, 0.86f, 1.14f, 0.18f, 0.94f, 1.76f, 0.92f, light);
        solidBox(matrices, vc, CABINET, 0.14f, 1.72f, 0.18f, 0.86f, 1.76f, 0.92f, light);

        // 3D MECHANICAL REELS
        drawReels(state, matrices, vc, light);

        // Gold Bezel Frame around Reel Window
        solidBox(matrices, vc, GOLD, 0.10f, 1.12f, 0.90f, 0.90f, 1.18f, 0.94f, light);
        solidBox(matrices, vc, GOLD, 0.10f, 1.70f, 0.90f, 0.90f, 1.76f, 0.94f, light);
        solidBox(matrices, vc, GOLD, 0.10f, 1.18f, 0.90f, 0.16f, 1.70f, 0.94f, light);
        solidBox(matrices, vc, GOLD, 0.84f, 1.18f, 0.90f, 0.90f, 1.70f, 0.94f, light);

        solidBox(matrices, vc, GOLD, 0.38f, 1.18f, 0.88f, 0.40f, 1.70f, 0.92f, light);
        solidBox(matrices, vc, GOLD, 0.60f, 1.18f, 0.88f, 0.62f, 1.70f, 0.92f, light);

        // Center Payline
        solidBox(matrices, vc, BUTTON, 0.16f, 1.443f, 0.91f, 0.84f, 1.457f, 0.93f, 0x00F000F0);
        solidBox(matrices, vc, GOLD, 0.16f, 1.457f, 0.90f, 0.84f, 1.463f, 0.92f, light);
        solidBox(matrices, vc, GOLD, 0.16f, 1.437f, 0.90f, 0.84f, 1.443f, 0.92f, light);
        solidBox(matrices, vc, BUTTON, 0.11f, 1.435f, 0.93f, 0.16f, 1.465f, 0.95f, 0x00F000F0);
        solidBox(matrices, vc, BUTTON, 0.84f, 1.435f, 0.93f, 0.89f, 1.465f, 0.95f, 0x00F000F0);

        // Glass Pane
        renderGlass(matrices, vc, 0.16f, 1.18f, 0.84f, 1.70f, 0.94f, light);

        // 5. TOP MARQUEE & CROWN (Y: 1.76 -> 2.08)
        solidBox(matrices, vc, CABINET, 0.06f, 1.76f, 0.08f, 0.94f, 2.05f, 0.92f, light);
        solidBox(matrices, vc, GOLD, 0.04f, 2.03f, 0.06f, 0.96f, 2.08f, 0.94f, light);
        solidBox(matrices, vc, GOLD, 0.10f, 1.80f, 0.89f, 0.90f, 2.02f, 0.93f, light);
        solidBox(matrices, vc, MARQUEE, 0.12f, 1.81f, 0.91f, 0.88f, 2.01f, 0.935f, 0x00F000F0);

        long time = System.currentTimeMillis();
        for (int b = 0; b < 7; b++) {
            float bx = 0.16f + b * 0.105f;
            Identifier bulbTex;
            int bulbLight = 0x00F000F0;

            if (state.isFever) {
                int disco = (int) ((time / 80 + b) % 3);
                bulbTex = (disco == 0) ? BUTTON : (disco == 1 ? GOLD : KNOB);
            } else if (state.spinning) {
                int chase = (int) ((time / 90) % 7);
                bulbTex = (b == chase || b == (chase + 1) % 7) ? BUTTON : GOLD;
            } else {
                boolean alt = ((time / 500 + b) % 2 == 0);
                bulbTex = alt ? GOLD : BUTTON;
            }

            solidBox(matrices, vc, bulbTex, bx, 2.02f, 0.92f, bx + 0.045f, 2.045f, 0.945f, bulbLight);
            solidBox(matrices, vc, bulbTex, bx, 1.77f, 0.92f, bx + 0.045f, 1.795f, 0.945f, bulbLight);
        }

        if (state.isFever) {
            solidBox(matrices, vc, GOLD, 0.14f, 1.16f, 0.20f, 0.86f, 1.74f, 0.22f, 0x00F000F0);
        }

        // 6. FULL BACK & SIDE PANELS
        solidBox(matrices, vc, CABINET, 0.04f, 0.00f, 0.04f, 0.96f, 2.05f, 0.08f, light);
        solidBox(matrices, vc, CABINET, 0.04f, 0.00f, 0.06f, 0.08f, 2.05f, 0.92f, light);
        solidBox(matrices, vc, CABINET, 0.92f, 0.00f, 0.06f, 0.96f, 2.05f, 0.92f, light);

        // 7. MECHANICAL PULL LEVER
        drawLever(state, matrices, vc, light);

        matrices.popPose();
    }

    private void drawLever(SlotMachineRenderState state, PoseStack matrices, SubmitNodeCollector vc, int light) {
        solidBox(matrices, vc, GOLD, 0.955f, 1.28f, 0.46f, 1.06f, 1.38f, 0.58f, light);

        matrices.pushPose();
        matrices.translate(1.05, 1.33, 0.52);

        float pullAngle = 0.0f;
        if (state.spinning && state.progress < 0.25f) {
            pullAngle = Mth.sin(state.progress / 0.25f * (float) Math.PI) * 40.0f;
        }
        matrices.rotate(Axis.XP.rotationDegrees(-pullAngle));

        solidBox(matrices, vc, GOLD, -0.018f, 0.0f, -0.018f, 0.018f, 0.38f, 0.018f, light);
        solidBox(matrices, vc, GOLD, -0.028f, 0.34f, -0.028f, 0.028f, 0.365f, 0.028f, light);
        solidBox(matrices, vc, CABINET, -0.038f, 0.355f, -0.038f, 0.038f, 0.455f, 0.038f, light);
        solidBox(matrices, vc, KNOB, -0.045f, 0.355f, -0.045f, 0.045f, 0.455f, 0.045f, light);

        matrices.popPose();
    }

    private void drawReels(SlotMachineRenderState state, PoseStack matrices, SubmitNodeCollector vc, int light) {
        int[] finalReels = state.reels;
        boolean spinning = state.spinning;
        float progress = state.progress;

        float cy = 1.45f;
        float cz = 0.55f;
        float reelRadius = 0.22f;
        float reelWidth = 0.17f;
        float facetHalfHeight = 0.0382f;

        float[] reelX = {0.27f, 0.50f, 0.73f};

        for (int r = 0; r < 3; r++) {
            float cx = reelX[r];
            float stopThreshold = (r == 0) ? 0.50f : (r == 1) ? 0.75f : 1.00f;
            float reelProgress = Math.min(1.0f, progress / stopThreshold);

            float targetAngle = finalReels[r] * 20.0f;
            float angle;
            if (!spinning || progress >= stopThreshold) {
                float bounceT = (progress - stopThreshold) / 0.10f;
                if (spinning && bounceT >= 0.0f && bounceT < 1.0f) {
                    float bounce = Mth.sin(bounceT * (float) Math.PI) * 3.0f * (1.0f - bounceT);
                    angle = targetAngle + bounce;
                } else {
                    angle = targetAngle;
                }
            } else {
                float t = reelProgress;
                float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
                float totalRotations = (4 - r) * 720.0f;
                angle = (1.0f - eased) * totalRotations + targetAngle;
            }

            matrices.pushPose();
            matrices.translate(cx, cy, cz);
            matrices.rotate(Axis.XP.rotationDegrees(angle));

            solidBox(matrices, vc, CABINET, -reelWidth * 0.51f, -0.04f, -0.04f, reelWidth * 0.51f, 0.04f, 0.04f, light);
            renderDrumEndCap(matrices, vc, -reelWidth * 0.495f, reelRadius * 0.99f, light);
            renderDrumEndCap(matrices, vc, reelWidth * 0.495f, reelRadius * 0.99f, light);

            for (int k = 0; k < 18; k++) {
                ReelSymbol facetSymbol = ReelLayout.symbolAt(r, k);
                Identifier symbolTex = SYMBOLS[facetSymbol.ordinal()];

                matrices.pushPose();
                matrices.rotate(Axis.XP.rotationDegrees(-k * 20.0f));
                matrices.translate(0.0, 0.0, reelRadius);

                renderTapeBacking(matrices, vc, -reelWidth * 0.49f, -facetHalfHeight, reelWidth * 0.49f, facetHalfHeight, light);
                renderSymbolFace(matrices, vc, symbolTex, -reelWidth * 0.48f, -facetHalfHeight, reelWidth * 0.48f, facetHalfHeight, light);
                matrices.popPose();
            }

            matrices.popPose();
        }
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

    private void renderDrumEndCap(PoseStack m, SubmitNodeCollector vc, float x, float radius, int light) {
        vc.submitCustomGeometry(m, RenderTypes.entityCutout(GOLD), (pose, b) -> {
            float normX = x >= 0 ? 1.0f : -1.0f;
            for (int i = 0; i < 18; i++) {
                float a1 = (float) Math.toRadians(i * 20.0);
                float a2 = (float) Math.toRadians((i + 1) * 20.0);
                float y1 = Mth.sin(a1) * radius;
                float z1 = Mth.cos(a1) * radius;
                float y2 = Mth.sin(a2) * radius;
                float z2 = Mth.cos(a2) * radius;
                b.addVertex(pose, x, 0.0f, 0.0f).setColor(210, 165, 45, 255).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, normX, 0, 0);
                b.addVertex(pose, x, y1, z1).setColor(180, 135, 35, 255).setUv(0.5f + Mth.cos(a1) * 0.4f, 0.5f + Mth.sin(a1) * 0.4f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, normX, 0, 0);
                b.addVertex(pose, x, y2, z2).setColor(180, 135, 35, 255).setUv(0.5f + Mth.cos(a2) * 0.4f, 0.5f + Mth.sin(a2) * 0.4f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, normX, 0, 0);
                b.addVertex(pose, x, y2, z2).setColor(180, 135, 35, 255).setUv(0.5f + Mth.cos(a2) * 0.4f, 0.5f + Mth.sin(a2) * 0.4f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, normX, 0, 0);
            }
        });
    }

    private void renderTapeBacking(PoseStack m, SubmitNodeCollector vc, float x0, float y0, float x1, float y1, int light) {
        vc.submitCustomGeometry(m, RenderTypes.entityCutout(REEL_TAPE), (pose, b) -> {
            b.addVertex(pose, x0, y1, -0.002f).setColor(255, 255, 255, 255).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x1, y1, -0.002f).setColor(255, 255, 255, 255).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x1, y0, -0.002f).setColor(255, 255, 255, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x0, y0, -0.002f).setColor(255, 255, 255, 255).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        });
    }

    private void renderSymbolFace(PoseStack m, SubmitNodeCollector vc, Identifier tex, float x0, float y0, float x1, float y1, int light) {
        vc.submitCustomGeometry(m, RenderTypes.entityCutout(tex), (pose, b) -> {
            b.addVertex(pose, x0, y1, 0.0f).setColor(255, 255, 255, 255).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x1, y1, 0.0f).setColor(255, 255, 255, 255).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x1, y0, 0.0f).setColor(255, 255, 255, 255).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x0, y0, 0.0f).setColor(255, 255, 255, 255).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        });
    }

    private void renderGlass(PoseStack m, SubmitNodeCollector vc, float x0, float y0, float x1, float y1, float z, int light) {
        vc.submitCustomGeometry(m, RenderTypes.entityTranslucent(GLASS), (pose, b) -> {
            b.addVertex(pose, x0, y0, z).setColor(255, 255, 255, 160).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x1, y0, z).setColor(255, 255, 255, 160).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x1, y1, z).setColor(255, 255, 255, 160).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
            b.addVertex(pose, x0, y1, z).setColor(255, 255, 255, 160).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
        });
    }
}
