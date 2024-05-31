package net.coderbot.iris.gui.option;

import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.Option;
//import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentTranslation;
//import net.minecraft.client.gui.components.AbstractWidget;
//import net.minecraft.client.gui.components.OptionButton;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.TranslatableComponent;

public class ShaderPackSelectionButtonOption extends Option {
	private final GuiScreen parent;
	private final Minecraft client;

	public ShaderPackSelectionButtonOption(GuiScreen parent, Minecraft client) {
		super("options.iris.shaderPackSelection");
		this.parent = parent;
		this.client = client;
	}

	@Override
	public AbstractWidget createButton(Options options, int x, int y, int width) {
		return new OptionButton(
				x, y, width, 20,
				this,
				new TextComponentTranslation("options.iris.shaderPackSelection"),
				button -> client.setScreen(new ShaderPackScreen(parent))
		);
	}
}
