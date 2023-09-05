package net.coderbot.iris.compat.sodium.impl.options;

import java.io.IOException;

import me.jellysquid.mods.sodium.client.gui.options.OptionFlag;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.option.IrisVideoSettings;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class IrisSodiumOptions {
	public static OptionImpl<Options, Integer> createMaxShadowDistanceSlider(MinecraftOptionsStorage vanillaOpts) {
		OptionImpl<Options, Integer> maxShadowDistanceSlider = OptionImpl.createBuilder(int.class, vanillaOpts)
				.setName(new TranslatableComponent("options.iris.shadowDistance"))
				.setTooltip(new TranslatableComponent("options.iris.shadowDistance.sodium_tooltip"))
				.setControl(option -> new SliderControl(option, 0, 32, 1, ControlValueFormatter.quantity("options.chunks")))
				.setBinding((options, value) -> {
							IrisVideoSettings.shadowDistance = value;
							try {
								Iris.getIrisConfig().save();
							} catch (IOException e) {
								e.printStackTrace();
							}
						},
						options -> IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance))
				.setImpact(OptionImpact.HIGH)
				.setEnabled(true)
				.build();

		((OptionImplExtended) maxShadowDistanceSlider).iris$dynamicallyEnable(IrisVideoSettings::isShadowDistanceSliderEnabled);

		return maxShadowDistanceSlider;
	}

	public static OptionImpl<Options, SupportedGraphicsMode> createLimitedVideoSettingsButton(MinecraftOptionsStorage vanillaOpts) {
		return OptionImpl.createBuilder(SupportedGraphicsMode.class, vanillaOpts)
				.setName(new TranslatableComponent("options.graphics"))
				.setTooltip(new TranslatableComponent("sodium.options.graphics_quality.tooltip"))
				.setControl(option -> new CyclingControl<>(option, SupportedGraphicsMode.class, new Component[] { new TranslatableComponent("options.graphics.fast"), new TranslatableComponent("options.graphics.fancy") }))
				.setBinding(
						(opts, value) -> opts.graphicsMode = value.toVanilla(),
						opts -> SupportedGraphicsMode.fromVanilla(opts.graphicsMode))
				.setImpact(OptionImpact.HIGH)
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
				.build();
	}
}