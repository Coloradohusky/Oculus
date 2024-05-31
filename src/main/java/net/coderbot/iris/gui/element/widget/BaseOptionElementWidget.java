package net.coderbot.iris.gui.element.widget;

import java.util.Optional;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
//import org.jetbrains.annotations.Nullable;
import javax.annotation.Nullable;
//import org.lwjgl.glfw.GLFW;

//import com.mojang.blaze3d.vertex.PoseStack;

import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.NavigationController;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.coderbot.iris.shaderpack.option.menu.OptionMenuElement;
//import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.Font;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.I18n;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.MutableComponent;
//import net.minecraft.network.chat.TextColor;
//import net.minecraft.network.chat.TextComponent;
//import net.minecraft.network.chat.TranslatableComponent;

public abstract class BaseOptionElementWidget<T extends OptionMenuElement> extends CommentedElementWidget<T> {
	protected static final ITextComponent SET_TO_DEFAULT = new TranslatableComponent("options.iris.setToDefault").withStyle(ChatFormatting.GREEN);
	protected static final ITextComponent DIVIDER = new TextComponent(": ");

	protected ITextComponent unmodifiedLabel;
	protected ShaderPackScreen screen;
	protected NavigationController navigation;
	private ITextComponent label;

	protected ITextComponent trimmedLabel;
	protected ITextComponent valueLabel;

	private boolean isLabelTrimmed;
	private int maxLabelWidth;
	private int valueSectionWidth;

	public BaseOptionElementWidget(T element) {
		super(element);
	}

	@Override
	public void init(ShaderPackScreen screen, NavigationController navigation) {
		this.screen = screen;
		this.navigation = navigation;
		this.valueLabel = null;
		this.trimmedLabel = null;
	}

	protected final void setLabel(MutableComponent label) {
		this.label = label.copy().append(DIVIDER);
		this.unmodifiedLabel = label;
	}

	protected final void updateRenderParams(int width, int minValueSectionWidth) {
		// Lazy init of value label
		if (this.valueLabel == null) {
			this.valueLabel = createValueLabel();
		}

		// Determine the width of the value box
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		this.valueSectionWidth = Math.max(minValueSectionWidth, font.width(this.valueLabel) + 8);

		// Determine maximum width of trimmed label
		this.maxLabelWidth = (width - 8) - this.valueSectionWidth;

		// Lazy init of trimmed label, and make sure it is only trimmed when necessary
		if (this.trimmedLabel == null || font.width(this.label) > this.maxLabelWidth != isLabelTrimmed) {
			updateLabels();
		}

		// Set whether the label has been trimmed (used when updating label and determining whether to render tooltips)
		this.isLabelTrimmed = font.width(this.label) > this.maxLabelWidth;
	}

	protected final void renderOptionWithValue(int x, int y, int width, int height, boolean hovered, float sliderPosition, int sliderWidth) {
		GuiUtil.bindIrisWidgetsTexture();

		// Draw button background
		GuiUtil.drawButton(x, y, width, height, hovered, false);

		// Draw the value box
		GuiUtil.drawButton((x + width) - (this.valueSectionWidth + 2), y + 2, this.valueSectionWidth, height - 4, false, true);

		// Draw the preview slider
		if (sliderPosition >= 0) {
			// Range of x values the slider can occupy
			int sliderSpace = (this.valueSectionWidth - 4) - sliderWidth;

			// Position of slider
			int sliderPos = ((x + width) - this.valueSectionWidth) + (int)(sliderPosition * sliderSpace);

			GuiUtil.drawButton(sliderPos, y + 4, sliderWidth, height - 8, false, false);
		}

		FontRenderer font = Minecraft.getMinecraft().fontRenderer;

		// Draw the label
		font.drawShadow(this.trimmedLabel, x + 6, y + 7, 0xFFFFFF);
		// Draw the value label
		font.drawShadow(this.valueLabel, (x + (width - 2)) - (int)(this.valueSectionWidth * 0.5) - (int)(font.width(this.valueLabel) * 0.5), y + 7, 0xFFFFFF);
	}

	protected final void renderOptionWithValue(int x, int y, int width, int height, boolean hovered) {
		this.renderOptionWithValue(x, y, width, height, hovered, -1, 0);
	}

	protected final void tryRenderTooltip(int mouseX, int mouseY, boolean hovered) {
		if (GuiScreen.isShiftKeyDown()) {
			renderTooltip(SET_TO_DEFAULT, mouseX, mouseY, hovered);
		} else if (this.isLabelTrimmed && !this.screen.isDisplayingComment()) {
			renderTooltip(this.unmodifiedLabel, mouseX, mouseY, hovered);
		}
	}

	protected final void renderTooltip(Component text, int mouseX, int mouseY, boolean hovered) {
		if (hovered) {
			ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(Minecraft.getMinecraft().fontRenderer, text, mouseX + 2, mouseY - 16));
		}
	}

	protected final void updateLabels() {
		this.trimmedLabel = createTrimmedLabel();
		this.valueLabel = createValueLabel();
	}

	protected final ITextComponent createTrimmedLabel() {
		MutableComponent label = GuiUtil.shortenText(
				Minecraft.getMinecraft().fontRenderer,
				this.label.copy(),
				this.maxLabelWidth);

		if (this.isValueModified()) {
			label = label.withStyle(style -> style.withColor(TextColor.fromRgb(0xffc94a)));
		}

		return label;
	}

	protected abstract ITextComponent createValueLabel();

	public abstract boolean applyNextValue();

	public abstract boolean applyPreviousValue();

	public abstract boolean applyOriginalValue();

	public abstract boolean isValueModified();

	public abstract @Nullable String getCommentKey();

	@Override
	public Optional<ITextComponent> getCommentTitle() {
		return Optional.of(this.unmodifiedLabel);
	}

	@Override
	public Optional<ITextComponent> getCommentBody() {
		return Optional.ofNullable(getCommentKey()).map(key -> I18n.exists(key) ? new TranslatableComponent(key) : null);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
//		if (button == GLFW.GLFW_MOUSE_BUTTON_1 || button == GLFW.GLFW_MOUSE_BUTTON_2) {
		if (button == 0 || button == 1) {
			boolean refresh = false;

			if (GuiScreen.isShiftKeyDown()) {
				refresh = applyOriginalValue();
			}
			if (!refresh) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
					refresh = applyNextValue();
				} else {
					refresh = applyPreviousValue();
				}
			}

			if (refresh) {
				this.navigation.refresh();
			}

			GuiUtil.playButtonClickSound();

			return true;
		}
		return super.mouseClicked(mx, my, button);
	}
}
