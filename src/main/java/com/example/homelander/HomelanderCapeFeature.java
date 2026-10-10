package com.example.homelander;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Red in-game cape attached to a player wearing the Homelander chestplate. */
public final class HomelanderCapeFeature
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier CAPE = HomelanderMod.id("textures/entity/homelander_cape.png");

    public HomelanderCapeFeature(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!player.getEquippedStack(EquipmentSlot.CHEST).isOf(HomelanderMod.CHESTPLATE)
                || player.isInvisible()) return;
        matrices.push();
        matrices.translate(0, 0, 0.125);
        // The vanilla cape model is a real 3D cloth panel attached to the torso.
        // A modest tilt keeps it visible when standing or flying.
        float angle = 9f + Math.min(20f, limbDistance * 8f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(CAPE));
        getContextModel().renderCape(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
