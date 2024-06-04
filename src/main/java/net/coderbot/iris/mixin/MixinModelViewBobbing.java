package net.coderbot.iris.mixin;

import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//import com.mojang.blaze3d.vertex.PoseStack;
import net.coderbot.iris.vendored.joml.Matrix4f;

//import net.minecraft.client.renderer.GameRenderer;

/**
 * This mixin makes the effects of view bobbing and nausea apply to the model view matrix, not the projection matrix.
 *
 * Applying these effects to the projection matrix causes severe issues with most shaderpacks. As it turns out, OptiFine
 * applies these effects to the modelview matrix. As such, we must do the same to properly run shaderpacks.
 *
 * This mixin makes use of the matrix stack in order to make these changes without more invasive changes.
 */
@Mixin(EntityRenderer.class)
public class MixinModelViewBobbing {
	@Unique
	private Matrix4f bobbingEffectsModel;

	@Unique
	private boolean areShadersOn;

	@Inject(method = "renderWorld", at = @At("HEAD"))
	private void iris$saveShadersOn(float partialTicks, long finishTimeNano, CallbackInfo ci) {
		areShadersOn = IrisApi.getInstance().isShaderPackInUse();
	}

	@ModifyArg(method = "renderLevel", index = 0,
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private PoseStack iris$separateViewBobbing(PoseStack stack) {
		if (!areShadersOn) return stack;

		stack.pushPose();
		stack.last().pose().setIdentity();

		return stack;
	}

	@Redirect(method = "renderLevel",
			at = @At(value = "INVOKE",
					target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"),
			slice = @Slice(from = @At(value = "INVOKE",
					       target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")))
	private PoseStack.Pose iris$saveBobbing(PoseStack stack) {
		if (!areShadersOn) return stack.last();

		bobbingEffectsModel = stack.last().pose().copy();

		stack.popPose();

		return stack.last();
	}

	@Inject(method = "renderLevel",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lcom/mojang/math/Matrix4f;)V"))
	private void iris$applyBobbingToModelView(float tickDelta, long limitTime, PoseStack matrix, CallbackInfo ci) {
		if (!areShadersOn) return;

		matrix.last().pose().multiply(bobbingEffectsModel);

		bobbingEffectsModel = null;
	}
}
