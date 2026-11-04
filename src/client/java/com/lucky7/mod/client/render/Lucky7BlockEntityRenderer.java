package com.lucky7.mod.client.render;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.Lucky7Block;
import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.game.reels.ReelLayout;
import com.lucky7.mod.game.reels.ReelSymbol;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class Lucky7BlockEntityRenderer implements BlockEntityRenderer<Lucky7BlockEntity> {
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

    public Lucky7BlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(Lucky7BlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vc, int light, int overlay) {
        Direction facing = Direction.NORTH;
        try {
            facing = be.getCachedState().get(Lucky7Block.FACING);
        } catch (Exception ignored) { }

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);

        // When facing SOUTH (default for placed block opposite to player looking NORTH), rotation is 0 deg.
        // Front (+Z) points directly towards SOUTH (towards the player).
        float rot = switch (facing) {
            case SOUTH -> 0.0f;
            case EAST -> 90.0f;
            case NORTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rot));
        matrices.translate(-0.5, 0.0, -0.5);

        // =====================================================================
        // 1. BASE & PLINTH (Y: 0.00 -> 0.28)
        // =====================================================================
        // Main base body
        solidBox(matrices, vc, CABINET, 0.04f, 0.00f, 0.06f, 0.96f, 0.28f, 0.94f, light);
        // Bottom gold trim base rim
        solidBox(matrices, vc, GOLD, 0.02f, 0.00f, 0.04f, 0.98f, 0.05f, 0.96f, light);
        // Coin tray front rim (Y: 0.06 -> 0.16, Z: 0.88 -> 0.98)
        solidBox(matrices, vc, GOLD, 0.24f, 0.06f, 0.88f, 0.76f, 0.16f, 0.98f, light);
        // Tray cavity interior
        solidBox(matrices, vc, INTERIOR, 0.26f, 0.08f, 0.89f, 0.74f, 0.14f, 0.96f, light);

        // =====================================================================
        // 2. LOWER ART PANEL & DOOR (Y: 0.28 -> 0.92)
        // =====================================================================
        // Lower cabinet body
        solidBox(matrices, vc, CABINET, 0.06f, 0.28f, 0.08f, 0.94f, 0.92f, 0.92f, light);
        // Raised decorative outer gold frame for artwork
        solidBox(matrices, vc, GOLD, 0.12f, 0.32f, 0.90f, 0.88f, 0.88f, 0.93f, light);
        // Recessed retro paytable chart glass
        solidBox(matrices, vc, PAYTABLE, 0.14f, 0.34f, 0.91f, 0.86f, 0.86f, 0.935f, light);

        // =====================================================================
        // 3. SLANTED CONTROL DECK & BUTTONS (Y: 0.92 -> 1.10)
        // =====================================================================
        // Slanted deck shelf jutting forward towards player (extends cleanly to Z = 1.07)
        solidBox(matrices, vc, DECK, 0.06f, 0.92f, 0.08f, 0.94f, 1.08f, 1.07f, light);
        // Sleek refined front lip edge trim
        solidBox(matrices, vc, GOLD, 0.05f, 1.04f, 1.05f, 0.95f, 1.09f, 1.08f, light);

        // Big Red SPIN Button on deck (quick spring press animation)
        float btnHeight = 0.055f;
        if (be.isSpinning()) {
            float p = be.clientProgress(tickDelta);
            if (p < 0.20f) {
                float press = MathHelper.sin(p / 0.20f * (float) Math.PI);
                btnHeight = 0.055f - press * 0.035f;
            }
        }
        // Bezel socket rim around red button - placed forward on deck (Z: 0.96 -> 1.05, in front of window frame at Z <= 0.94)
        solidBox(matrices, vc, GOLD, 0.20f, 1.08f, 0.96f, 0.42f, 1.095f, 1.05f, light);
        // Solid black inner core filling the socket completely (100% non-transparent)
        solidBox(matrices, vc, CABINET, 0.215f, 1.08f, 0.965f, 0.405f, 1.08f + btnHeight - 0.002f, 1.045f, light);
        // Red button on top
        solidBox(matrices, vc, BUTTON, 0.22f, 1.08f, 0.97f, 0.40f, 1.08f + btnHeight, 1.04f, light);

        // Coin slot housing on right side of deck (Z: 0.96 -> 1.03)
        solidBox(matrices, vc, GOLD, 0.68f, 1.08f, 0.96f, 0.80f, 1.11f, 1.03f, light);
        solidBox(matrices, vc, INTERIOR, 0.72f, 1.09f, 0.97f, 0.76f, 1.11f, 1.02f, light);

        // =====================================================================
        // 4. REEL CHAMBER & GLASS HOUSING (Y: 1.14 -> 1.76)
        // =====================================================================
        // Back wall behind reels
        solidBox(matrices, vc, CABINET, 0.06f, 1.14f, 0.08f, 0.94f, 1.76f, 0.18f, light);
        // Interior back cavity lining
        solidBox(matrices, vc, INTERIOR, 0.12f, 1.16f, 0.18f, 0.88f, 1.74f, 0.20f, light);
        // Left wall
        solidBox(matrices, vc, CABINET, 0.06f, 1.14f, 0.18f, 0.14f, 1.76f, 0.92f, light);
        // Right wall
        solidBox(matrices, vc, CABINET, 0.86f, 1.14f, 0.18f, 0.94f, 1.76f, 0.92f, light);
        // Ceiling over reels
        solidBox(matrices, vc, CABINET, 0.14f, 1.72f, 0.18f, 0.86f, 1.76f, 0.92f, light);

        // 3D MECHANICAL REELS
        drawReels(be, tickDelta, matrices, vc, light);

        // Gold Bezel Frame around Reel Window
        // Bottom frame strip
        solidBox(matrices, vc, GOLD, 0.10f, 1.12f, 0.90f, 0.90f, 1.18f, 0.94f, light);
        // Top frame strip
        solidBox(matrices, vc, GOLD, 0.10f, 1.70f, 0.90f, 0.90f, 1.76f, 0.94f, light);
        // Left frame strip
        solidBox(matrices, vc, GOLD, 0.10f, 1.18f, 0.90f, 0.16f, 1.70f, 0.94f, light);
        // Right frame strip
        solidBox(matrices, vc, GOLD, 0.84f, 1.18f, 0.90f, 0.90f, 1.70f, 0.94f, light);

        // Dividers between reels
        solidBox(matrices, vc, GOLD, 0.38f, 1.18f, 0.88f, 0.40f, 1.70f, 0.92f, light);
        solidBox(matrices, vc, GOLD, 0.60f, 1.18f, 0.88f, 0.62f, 1.70f, 0.92f, light);

        // Clear, Prominent Center Payline across the window (Winning Line)
        // Red glowing core line
        solidBox(matrices, vc, BUTTON, 0.16f, 1.443f, 0.91f, 0.84f, 1.457f, 0.93f, 0x00F000F0);
        // Golden guide rails above and below the line
        solidBox(matrices, vc, GOLD, 0.16f, 1.457f, 0.90f, 0.84f, 1.463f, 0.92f, light);
        solidBox(matrices, vc, GOLD, 0.16f, 1.437f, 0.90f, 0.84f, 1.443f, 0.92f, light);
        // Side payline pointer arrows on left and right bezels
        solidBox(matrices, vc, BUTTON, 0.11f, 1.435f, 0.93f, 0.16f, 1.465f, 0.95f, 0x00F000F0);
        solidBox(matrices, vc, BUTTON, 0.84f, 1.435f, 0.93f, 0.89f, 1.465f, 0.95f, 0x00F000F0);

        // Clear Glass Pane covering the reels
        renderGlass(matrices, vc, 0.16f, 1.18f, 0.84f, 1.70f, 0.94f, light);

        // =====================================================================
        // 5. TOP MARQUEE & CROWN (Y: 1.76 -> 2.08)
        // =====================================================================
        // Upper cabinet head
        solidBox(matrices, vc, CABINET, 0.06f, 1.76f, 0.08f, 0.94f, 2.05f, 0.92f, light);
        // Top gold roof crown
        solidBox(matrices, vc, GOLD, 0.04f, 2.03f, 0.06f, 0.96f, 2.08f, 0.94f, light);
        // Illuminated Header Marquee Sign ("★ LUCKY 7 ★")
        solidBox(matrices, vc, GOLD, 0.10f, 1.80f, 0.89f, 0.90f, 2.02f, 0.93f, light);
        solidBox(matrices, vc, MARQUEE, 0.12f, 1.81f, 0.91f, 0.88f, 2.01f, 0.935f, 0x00F000F0);

        // Animated Casino Cabochon Bulbs along top crown (Chaser lights & disco strobe)
        long time = System.currentTimeMillis();
        boolean spinning = be.isSpinning();
        boolean isFever = be.isFeverMode();

        for (int b = 0; b < 7; b++) {
            float bx = 0.16f + b * 0.105f;
            Identifier bulbTex;
            int bulbLight = 0x00F000F0;

            if (isFever) {
                int disco = (int) ((time / 80 + b) % 3);
                bulbTex = (disco == 0) ? BUTTON : (disco == 1 ? GOLD : KNOB);
            } else if (spinning) {
                int chase = (int) ((time / 90) % 7);
                bulbTex = (b == chase || b == (chase + 1) % 7) ? BUTTON : GOLD;
            } else {
                boolean alt = ((time / 500 + b) % 2 == 0);
                bulbTex = alt ? GOLD : BUTTON;
            }

            // Top bulbs along crown
            solidBox(matrices, vc, bulbTex, bx, 2.02f, 0.92f, bx + 0.045f, 2.045f, 0.945f, bulbLight);
            // Bottom bulbs along deck edge
            solidBox(matrices, vc, bulbTex, bx, 1.77f, 0.92f, bx + 0.045f, 1.795f, 0.945f, bulbLight);
        }

        if (isFever) {
            // Luminous golden aura filling the reel chamber
            solidBox(matrices, vc, GOLD, 0.14f, 1.16f, 0.20f, 0.86f, 1.74f, 0.22f, 0x00F000F0);
        }

        // =====================================================================
        // 6. FULL BACK & SIDE PANELS (Guarantees zero holes or inside-out faces)
        // =====================================================================
        // Solid back plate from Y=0 to Y=2.05
        solidBox(matrices, vc, CABINET, 0.04f, 0.00f, 0.04f, 0.96f, 2.05f, 0.08f, light);
        // Solid left outer side plate
        solidBox(matrices, vc, CABINET, 0.04f, 0.00f, 0.06f, 0.08f, 2.05f, 0.92f, light);
        // Solid right outer side plate
        solidBox(matrices, vc, CABINET, 0.92f, 0.00f, 0.06f, 0.96f, 2.05f, 0.92f, light);

        // =====================================================================
        // 7. MECHANICAL PULL LEVER (Mounted on Right Side Wall X = 0.96)
        // =====================================================================
        drawLever(be, tickDelta, matrices, vc, light);

        matrices.pop();
    }

    private void drawLever(Lucky7BlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vc, int light) {
        // Pivot housing base mounted flush against Right Side Wall (wall is at X = 0.96f)
        // Embedded slightly into the wall at X = 0.955f (no gap) and extends outward to X = 1.06f
        solidBox(matrices, vc, GOLD, 0.955f, 1.28f, 0.46f, 1.06f, 1.38f, 0.58f, light);

        matrices.push();
        // Pivot center placed at X = 1.05f so the entire knob (radius 0.045f) is at X >= 1.005f, well clear of the wall (X = 0.96f)
        matrices.translate(1.05, 1.33, 0.52);

        // Pull angle animation
        float pullAngle = 0.0f;
        if (be.isSpinning()) {
            float p = be.clientProgress(tickDelta);
            if (p < 0.25f) {
                // Pull down smoothly in first 25% of spin
                pullAngle = MathHelper.sin(p / 0.25f * (float) Math.PI) * 40.0f;
            }
        }
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-pullAngle));

        // Metal lever rod extending upwards
        solidBox(matrices, vc, GOLD, -0.018f, 0.0f, -0.018f, 0.018f, 0.38f, 0.018f, light);

        // Golden collar ring connecting rod to knob
        solidBox(matrices, vc, GOLD, -0.028f, 0.34f, -0.028f, 0.028f, 0.365f, 0.028f, light);

        // Solid black inner socket core filling the interior completely (no hollow void)
        solidBox(matrices, vc, CABINET, -0.038f, 0.355f, -0.038f, 0.038f, 0.455f, 0.038f, light);

        // Solid ruby chrome ball knob cube on top using KNOB texture (100% solid, zero wall clipping)
        solidBox(matrices, vc, KNOB, -0.045f, 0.355f, -0.045f, 0.045f, 0.455f, 0.045f, light);

        matrices.pop();
    }

    private void drawReels(Lucky7BlockEntity be, float tickDelta, MatrixStack matrices, VertexConsumerProvider vc, int light) {
        int[] finalReels = be.reels();
        boolean spinning = be.isSpinning();
        float progress = be.clientProgress(tickDelta);

        float cy = 1.45f; // center height of reels
        float cz = 0.55f; // center depth
        float reelRadius = 0.22f;
        float reelWidth = 0.17f;
        float facetHalfHeight = 0.0382f; // Exact chord half-length for 18 facets at R=0.22 (zero overlap!)

        float[] reelX = {0.27f, 0.50f, 0.73f};

        for (int r = 0; r < 3; r++) {
            float cx = reelX[r];

            // Independent reel stop timing:
            // Reel 0 stops at 0.50 of total spin
            // Reel 1 stops at 0.75 of total spin
            // Reel 2 stops at 1.00 of total spin
            float stopThreshold = (r == 0) ? 0.50f : (r == 1) ? 0.75f : 1.00f;
            float reelProgress = Math.min(1.0f, progress / stopThreshold);

            float targetAngle = finalReels[r] * 20.0f;
            float angle;
            if (!spinning || progress >= stopThreshold) {
                // Subtle mechanical click bounce when stopping
                float bounceT = (progress - stopThreshold) / 0.10f;
                if (spinning && bounceT >= 0.0f && bounceT < 1.0f) {
                    float bounce = MathHelper.sin(bounceT * (float) Math.PI) * 3.0f * (1.0f - bounceT);
                    angle = targetAngle + bounce;
                } else {
                    angle = targetAngle;
                }
            } else {
                // Spinning phase with smooth acceleration and deceleration
                float t = reelProgress;
                float eased = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
                float totalRotations = (4 - r) * 720.0f;
                angle = (1.0f - eased) * totalRotations + targetAngle;
            }

            matrices.push();
            matrices.translate(cx, cy, cz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));

            // Central slim mechanical axle (never protrudes through the 18 facets)
            solidBox(matrices, vc, CABINET, -reelWidth * 0.51f, -0.04f, -0.04f, reelWidth * 0.51f, 0.04f, 0.04f, light);

            // Circular 18-sided drum end plates on left and right sides
            renderDrumEndCap(matrices, vc, -reelWidth * 0.495f, reelRadius * 0.99f, light);
            renderDrumEndCap(matrices, vc, reelWidth * 0.495f, reelRadius * 0.99f, light);

            // Render 18 symbol facets around the cylinder (without overlapping!)
            for (int k = 0; k < 18; k++) {
                ReelSymbol facetSymbol = ReelLayout.symbolAt(r, k);
                Identifier symbolTex = SYMBOLS[facetSymbol.ordinal()];

                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-k * 20.0f));
                matrices.translate(0.0, 0.0, reelRadius);

                // Authentic mechanical white reel tape backing behind each symbol
                renderTapeBacking(matrices, vc, -reelWidth * 0.49f, -facetHalfHeight, reelWidth * 0.49f, facetHalfHeight, light);

                // Quad displaying the symbol right-side up, exactly matching the facet dimensions
                renderSymbolFace(matrices, vc, symbolTex, -reelWidth * 0.48f, -facetHalfHeight, reelWidth * 0.48f, facetHalfHeight, light);
                matrices.pop();
            }

            matrices.pop();
        }
    }

    // =========================================================================
    // GEOMETRIC PRIMITIVES WITH PROPER NORMALS AND OUTWARD-FACING CULLING
    // =========================================================================
    private void solidBox(MatrixStack m, VertexConsumerProvider vc, Identifier tex, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
        VertexConsumer b = vc.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        Matrix4f mat = m.peek().getPositionMatrix();

        // Front (+Z)
        quadFace(mat, b, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        // Back (-Z)
        quadFace(mat, b, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        // Left (-X)
        quadFace(mat, b, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        // Right (+X)
        quadFace(mat, b, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        // Top (+Y)
        quadFace(mat, b, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        // Bottom (-Y)
        quadFace(mat, b, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    }

    private void quadFace(Matrix4f mat, VertexConsumer b, int light, float nx, float ny, float nz, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        b.vertex(mat, x0, y0, z0).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
        b.vertex(mat, x1, y1, z1).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
        b.vertex(mat, x2, y2, z2).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
        b.vertex(mat, x3, y3, z3).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(nx, ny, nz);
    }

    private void renderDrumEndCap(MatrixStack m, VertexConsumerProvider vc, float x, float radius, int light) {
        VertexConsumer b = vc.getBuffer(RenderLayer.getEntityCutoutNoCull(GOLD));
        Matrix4f mat = m.peek().getPositionMatrix();
        float normX = x >= 0 ? 1.0f : -1.0f;
        for (int i = 0; i < 18; i++) {
            float a1 = (float) Math.toRadians(i * 20.0);
            float a2 = (float) Math.toRadians((i + 1) * 20.0);
            float y1 = MathHelper.sin(a1) * radius;
            float z1 = MathHelper.cos(a1) * radius;
            float y2 = MathHelper.sin(a2) * radius;
            float z2 = MathHelper.cos(a2) * radius;
            b.vertex(mat, x, 0.0f, 0.0f).color(210, 165, 45, 255).texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normX, 0, 0);
            b.vertex(mat, x, y1, z1).color(180, 135, 35, 255).texture(0.5f + MathHelper.cos(a1) * 0.4f, 0.5f + MathHelper.sin(a1) * 0.4f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normX, 0, 0);
            b.vertex(mat, x, y2, z2).color(180, 135, 35, 255).texture(0.5f + MathHelper.cos(a2) * 0.4f, 0.5f + MathHelper.sin(a2) * 0.4f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normX, 0, 0);
            b.vertex(mat, x, y2, z2).color(180, 135, 35, 255).texture(0.5f + MathHelper.cos(a2) * 0.4f, 0.5f + MathHelper.sin(a2) * 0.4f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normX, 0, 0);
        }
    }

    private void renderTapeBacking(MatrixStack m, VertexConsumerProvider vc, float x0, float y0, float x1, float y1, int light) {
        VertexConsumer b = vc.getBuffer(RenderLayer.getEntityCutoutNoCull(REEL_TAPE));
        Matrix4f mat = m.peek().getPositionMatrix();
        // Clean brilliant white polymer tape background behind symbol
        b.vertex(mat, x0, y1, -0.002f).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x1, y1, -0.002f).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x1, y0, -0.002f).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x0, y0, -0.002f).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
    }

    private void renderSymbolFace(MatrixStack m, VertexConsumerProvider vc, Identifier tex, float x0, float y0, float x1, float y1, int light) {
        VertexConsumer b = vc.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
        Matrix4f mat = m.peek().getPositionMatrix();
        // y1 is top -> v = 0.0f; y0 is bottom -> v = 1.0f (Symbol is right-side up!)
        b.vertex(mat, x0, y1, 0.0f).color(255, 255, 255, 255).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x1, y1, 0.0f).color(255, 255, 255, 255).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x1, y0, 0.0f).color(255, 255, 255, 255).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x0, y0, 0.0f).color(255, 255, 255, 255).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
    }

    private void renderGlass(MatrixStack m, VertexConsumerProvider vc, float x0, float y0, float x1, float y1, float z, int light) {
        VertexConsumer b = vc.getBuffer(RenderLayer.getEntityTranslucent(GLASS));
        Matrix4f mat = m.peek().getPositionMatrix();
        b.vertex(mat, x0, y0, z).color(255, 255, 255, 160).texture(0.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x1, y0, z).color(255, 255, 255, 160).texture(1.0f, 1.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x1, y1, z).color(255, 255, 255, 160).texture(1.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        b.vertex(mat, x0, y1, z).color(255, 255, 255, 160).texture(0.0f, 0.0f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
    }
}
