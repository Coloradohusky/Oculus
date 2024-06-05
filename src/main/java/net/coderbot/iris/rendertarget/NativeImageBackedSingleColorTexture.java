package net.coderbot.iris.rendertarget;

import nanolive.compat.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import static nanolive.compat.NativeImage.toBufferedImage;

public class NativeImageBackedSingleColorTexture extends DynamicTexture {

	public NativeImageBackedSingleColorTexture(int red, int green, int blue, int alpha) {
		super(toBufferedImage(create(NativeImage.combine(alpha, blue, green, red))));
	}

	public NativeImageBackedSingleColorTexture(int rgba) {
		this(rgba >> 24 & 0xFF, rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF);
	}

	private static NativeImage create(int color) {
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
		image.setPixelRGBA(0, 0, color);
		return image;
	}
}
