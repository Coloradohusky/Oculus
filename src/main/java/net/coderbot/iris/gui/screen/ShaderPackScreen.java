package net.coderbot.iris.gui.screen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.coderbot.iris.gui.option.ImageButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Util;
//import org.jetbrains.annotations.Nullable;
//import org.lwjgl.glfw.GLFW;

//import com.mojang.blaze3d.vertex.PoseStack;

import net.coderbot.iris.Iris;
import net.coderbot.iris.gui.GuiUtil;
import net.coderbot.iris.gui.NavigationController;
import net.coderbot.iris.gui.element.ShaderPackOptionList;
import net.coderbot.iris.gui.element.ShaderPackSelectionList;
import net.coderbot.iris.gui.element.widget.AbstractElementWidget;
import net.coderbot.iris.gui.element.widget.CommentedElementWidget;
import net.coderbot.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.util.text.*;
//import net.minecraft.ChatFormatting;
//import net.minecraft.Util;
//import net.minecraft.client.gui.components.Button;
//import net.minecraft.client.gui.components.ImageButton;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.CommonComponents;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.MutableComponent;
//import net.minecraft.network.chat.TextComponent;
//import net.minecraft.network.chat.TranslatableComponent;
//import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;

public class ShaderPackScreen extends GuiScreen implements HudHideable {
	/**
	 * Queue rendering to happen on top of all elements. Useful for tooltips or dialogs.
	 */
	public static final Set<Runnable> TOP_LAYER_RENDER_QUEUE = new HashSet<>();

	private static final ITextComponent SELECT_TITLE = new TextComponentTranslation("pack.iris.select.title").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
	private static final ITextComponent CONFIGURE_TITLE = new TextComponentTranslation("pack.iris.configure.title").setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
	private static final ITextComponent title = new TextComponentTranslation("options.iris.shaderPackSelection.title");
	private static final int COMMENT_PANEL_WIDTH = 314;

	private final GuiScreen parent;
	private final ITextComponent irisTextComponent;

	private ShaderPackSelectionList shaderPackList;

	private @Nullable ShaderPackOptionList shaderOptionList = null;
	private @Nullable NavigationController navigation = null;
	private GuiButton screenSwitchButton;

	private ITextComponent notificationDialog = null;
	private int notificationDialogTimer = 0;

	private @Nullable AbstractElementWidget<?> hoveredElement = null;
	private Optional<ITextComponent> hoveredElementCommentTitle = Optional.empty();
	private List<String> hoveredElementCommentBody = new ArrayList<>();
	private int hoveredElementCommentTimer = 0;

	private boolean optionMenuOpen = false;

	private boolean dropChanges = false;
	private static final String development = "Development Environment";
	private ITextComponent developmentComponent;
	private ITextComponent updateComponent;

	private boolean guiHidden = false;
	private float guiButtonHoverTimer = 0.0f;

	private static final int DONE_BUTTON_ID = 0;
	private static final int APPLY_BUTTON_ID = 1;
	private static final int DROP_BUTTON_ID = 2;
	private static final int OPEN_BUTTON_ID = 3;
	private static final int LIST_BUTTON_ID = 4;

	public ShaderPackScreen(GuiScreen parent) {
		// todo super(new TextComponentTranslation("options.iris.shaderPackSelection.title"));

		this.parent = parent;

		String irisName = Iris.MODNAME + " " + Iris.getVersion();

		if (irisName.contains("-development-environment")) {
			this.developmentComponent = new TextComponentString(TextFormatting.GOLD + development);
			irisName = irisName.replace("-development-environment", "");
		}

		this.irisTextComponent = new TextComponentString(irisName).setStyle(new Style().setColor(TextFormatting.GRAY));

		refreshForChangedPack();
	}

	//@Override
	public void render(int mouseX, int mouseY, float delta) {
		if (this.mc.world == null) {
			this.drawDefaultBackground();
		} else if (!this.guiHidden) {
			this.drawGradientRect(0, 0, width, height, 0x4F232323, 0x4F232323);
		}

		if (!this.guiHidden) {
			if (optionMenuOpen && this.shaderOptionList != null) {
				this.shaderOptionList.drawScreen(mouseX, mouseY, delta);
			} else {
				this.shaderPackList.drawScreen(mouseX, mouseY, delta);
			}
		}

		float previousHoverTimer = this.guiButtonHoverTimer;
		super.drawScreen(mouseX, mouseY, delta);
		if (previousHoverTimer == this.guiButtonHoverTimer) {
			this.guiButtonHoverTimer = 0.0f;
		}

		if (!this.guiHidden) {
			drawCenteredString(this.fontRenderer, title.getFormattedText(), (int) (this.width * 0.5), 8, 0xFFFFFF);

			if (notificationDialog != null && notificationDialogTimer > 0) {
				drawCenteredString(this.fontRenderer, notificationDialog.getFormattedText(), (int) (this.width * 0.5), 21, 0xFFFFFF);
			} else {
				if (optionMenuOpen) {
					drawCenteredString(this.fontRenderer, CONFIGURE_TITLE.getFormattedText(), (int) (this.width * 0.5), 21, 0xFFFFFF);
				} else {
					drawCenteredString(this.fontRenderer, SELECT_TITLE.getFormattedText(), (int) (this.width * 0.5), 21, 0xFFFFFF);
				}
			}

			// Draw the comment panel
			if (this.isDisplayingComment()) {
				// Determine panel height and position
				int panelHeight = Math.max(50, 18 + (this.hoveredElementCommentBody.size() * 10));
				int x = (int) (0.5 * this.width) - 157;
				int y = this.height - (panelHeight + 4);
				// Draw panel
				GuiUtil.drawPanel(x, y, COMMENT_PANEL_WIDTH, panelHeight);
				// Draw text
				this.fontRenderer.drawStringWithShadow(this.hoveredElementCommentTitle.toString(), x + 4, y + 4, 0xFFFFFF);
				for (int i = 0; i < this.hoveredElementCommentBody.size(); i++) {
					this.fontRenderer.drawStringWithShadow(String.valueOf(this.hoveredElementCommentBody.toString().charAt(i)), x + 4, (y + 16) + (i * 10), 0xFFFFFF);
				}
			}
		}

		// Render everything queued to render last
		for (Runnable render : TOP_LAYER_RENDER_QUEUE) {
			render.run();
		}
		TOP_LAYER_RENDER_QUEUE.clear();

		if (this.developmentComponent != null) {
			this.fontRenderer.drawStringWithShadow(developmentComponent.getFormattedText(), 2, this.height - 10, 0xFFFFFF);
			this.fontRenderer.drawStringWithShadow(irisTextComponent.getFormattedText(), 2, this.height - 20, 0xFFFFFF);
		} else if (this.updateComponent != null) {
			this.fontRenderer.drawStringWithShadow(updateComponent.getFormattedText(), 2, this.height - 10, 0xFFFFFF);
			this.fontRenderer.drawStringWithShadow(irisTextComponent.getFormattedText(), 2, this.height - 20, 0xFFFFFF);
		} else {
			this.fontRenderer.drawStringWithShadow(irisTextComponent.getFormattedText(), 2, this.height - 10, 0xFFFFFF);
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		int bottomCenter = this.width / 2 - 50;
		int topCenter = this.width / 2 - 76;
		boolean inWorld = this.mc.world != null;

		// todo this.children.remove(this.shaderPackList);
		// todo this.children.remove(this.shaderOptionList);

		this.shaderPackList = new ShaderPackSelectionList(this, this.mc, this.width, this.height, 32, this.height - 58, 0, this.width);

		if (Iris.getCurrentPack().isPresent() && this.navigation != null) {
			ShaderPack currentPack = Iris.getCurrentPack().get();

			this.shaderOptionList = new ShaderPackOptionList(this, this.navigation, currentPack, this.mc, this.width, this.height, 32, this.height - 58, 0, this.width);
			this.navigation.setActiveOptionList(this.shaderOptionList);

			this.shaderOptionList.rebuild();
		} else {
			optionMenuOpen = false;
			this.shaderOptionList = null;
		}

		if (inWorld) {
			// todo this.shaderPackList.setRenderBackground(false);
			if (shaderOptionList != null) {
				this.shaderOptionList.setRenderBackground(false);
			}
		}

		// todo this.buttons.clear();
		// todo this.children.clear();

		if (!this.guiHidden) {
			if (optionMenuOpen && shaderOptionList != null) {
				// todo this.children.add(shaderOptionList);
			} else {
				// todo this.children.add(shaderPackList);
			}

			this.buttonList.add(new GuiButton(DONE_BUTTON_ID, bottomCenter + 104, this.height - 27, 100, 20,
					new TextComponentTranslation("options.iris.done").getFormattedText()));

			this.buttonList.add(new GuiButton(APPLY_BUTTON_ID, bottomCenter, this.height - 27, 100, 20,
					new TextComponentTranslation("options.iris.apply").getFormattedText()));

			this.buttonList.add(new GuiButton(DROP_BUTTON_ID, bottomCenter - 104, this.height - 27, 100, 20,
					new TextComponentTranslation("options.iris.cancel").getFormattedText()));

			this.buttonList.add(new GuiButton(OPEN_BUTTON_ID, topCenter - 78, this.height - 51, 152, 20,
					new TextComponentTranslation("options.iris.openShaderPackFolder").getFormattedText()));

			this.buttonList.add(new GuiButton(LIST_BUTTON_ID, topCenter + 78, this.height - 51, 152, 20,
					new TextComponentTranslation("options.iris.shaderPackList").getFormattedText()));
			this.screenSwitchButton = this.buttonList.get(LIST_BUTTON_ID);

			refreshScreenSwitchButton();
		}

		if (inWorld) {
			ITextComponent showOrHide = this.guiHidden
				? new TextComponentTranslation("options.iris.gui.show")
				: new TextComponentTranslation("options.iris.gui.hide");

			float endOfLastButton = this.width / 2.0f + 154.0f;
			float freeSpace = this.width - endOfLastButton;
			int x;
			if (freeSpace > 100.0f) {
				x = this.width - 50;
			} else if (freeSpace < 20.0f) {
				x = this.width - 20;
			} else {
				x = (int) (endOfLastButton + (freeSpace / 2.0f)) - 10;
			}

			this.addButton(new ImageButton(
					0, x, this.height - 39, 20, 20,
					this.guiHidden ? 20 : 0, 146,
					0, 146,
					GuiUtil.IRIS_WIDGETS_TEX
			));
		}

		// NB: Don't let comment remain when exiting options screen
		// https://github.com/IrisShaders/Iris/issues/1494
		this.hoveredElement = null;
		this.hoveredElementCommentTimer = 0;
	}

	public void refreshForChangedPack() {
		if (Iris.getCurrentPack().isPresent()) {
			ShaderPack currentPack = Iris.getCurrentPack().get();

			this.navigation = new NavigationController(currentPack.getMenuContainer());

			if (this.shaderOptionList != null) {
				this.shaderOptionList.applyShaderPack(currentPack);
				this.shaderOptionList.rebuild();
			}
		} else {
			this.navigation = null;
		}

		refreshScreenSwitchButton();
	}

	public void refreshScreenSwitchButton() {
		if (this.screenSwitchButton != null) {
			this.screenSwitchButton.displayString = optionMenuOpen
					? I18n.format("options.iris.shaderPackList")
					: I18n.format("options.iris.shaderPackSettings");
			this.screenSwitchButton.enabled = optionMenuOpen || this.shaderPackList.getTopButtonRow().shadersEnabled;
		}
	}


	//@Override
	public void tick() {
		super.updateScreen();

		if (this.notificationDialogTimer > 0) {
			this.notificationDialogTimer--;
		}

		if (this.hoveredElement != null) {
			this.hoveredElementCommentTimer++;
		} else {
			this.hoveredElementCommentTimer = 0;
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);

        if (typedChar != 'a') {
            if (keyCode == 1) {
                if (this.guiHidden) {
                    this.guiHidden = false;
                    this.initGui();
                } else if (this.navigation != null && this.navigation.hasHistory()) {
                    this.navigation.back();
                } else if (this.optionMenuOpen) {
                    this.optionMenuOpen = false;
                    this.initGui();
                }
            }
        }
    }


	//@Override
	public void onFilesDrop(List<Path> paths) {
		if (this.optionMenuOpen) {
			onOptionMenuFilesDrop(paths);
		} else {
			onPackListFilesDrop(paths);
		}
	}

	public void onPackListFilesDrop(List<Path> paths) {
		List<Path> packs = paths.stream().filter(Iris::isValidShaderpack).collect(Collectors.toList());

		for (Path pack : packs) {
			String fileName = pack.getFileName().toString();

			try {
				Iris.getShaderpacksDirectoryManager().copyPackIntoDirectory(fileName, pack);
			} catch (FileAlreadyExistsException e) {
				this.notificationDialog = new TextComponentTranslation(
						"options.iris.shaderPackSelection.copyErrorAlreadyExists",
						fileName
				).setStyle(new Style().setColor(TextFormatting.RED).setItalic(true));

				this.notificationDialogTimer = 100;
				this.shaderPackList.refresh();

				return;
			} catch (IOException e) {
				Iris.logger.warn("Error copying dragged shader pack", e);

				this.notificationDialog = new TextComponentTranslation(
						"options.iris.shaderPackSelection.copyError",
						fileName
				).setStyle(new Style().setColor(TextFormatting.RED).setItalic(true));

				this.notificationDialogTimer = 100;
				this.shaderPackList.refresh();

				return;
			}
		}

		// After copying the relevant files over to the folder, make sure to refresh the shader pack list.
		this.shaderPackList.refresh();

		if (packs.isEmpty()) {
			// If zero packs were added, then notify the user that the files that they added weren't actually shader
			// packs.

			if (paths.size() == 1) {
				// If a single pack could not be added, provide a message with that pack in the file name
				String fileName = paths.get(0).getFileName().toString();

				this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackSelection.failedAddSingle",
					fileName
				).setStyle(new Style().setColor(TextFormatting.RED).setItalic(true));
			} else {
				// Otherwise, show a generic message.

				this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackSelection.failedAdd"
				).setStyle(new Style().setColor(TextFormatting.RED).setItalic(true));
			}

		} else if (packs.size() == 1) {
			// In most cases, users will drag a single pack into the selection menu. So, let's special case it.
			String packName = packs.get(0).getFileName().toString();

			this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackSelection.addedPack",
					packName
			).setStyle(new Style().setColor(TextFormatting.YELLOW).setItalic(true));

			// Select the pack that the user just added, since if a user just dragged a pack in, they'll probably want
			// to actually use that pack afterwards.
			this.shaderPackList.select(packName);
		} else {
			// We also support multiple packs being dragged and dropped at a time. Just show a generic success message
			// in that case.
			this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackSelection.addedPacks",
					packs.size()
			).setStyle(new Style().setColor(TextFormatting.YELLOW).setItalic(true));
		}

		// Show the relevant message for 5 seconds (100 ticks)
		this.notificationDialogTimer = 100;
	}

	public void displayNotification(ITextComponent component) {
		this.notificationDialog = component;
		this.notificationDialogTimer = 100;
	}

	public void onOptionMenuFilesDrop(List<Path> paths) {
		// If more than one option file has been dragged, display an error
		// as only one option file should be imported at a time
		if (paths.size() != 1) {
			this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackOptions.tooManyFiles"
			).setStyle(new Style().setColor(TextFormatting.RED).setItalic(true));
			this.notificationDialogTimer = 100; // 5 seconds (100 ticks)

			return;
		}

		this.importPackOptions(paths.get(0));
	}

	public void importPackOptions(Path settingFile) {
		try (InputStream in = Files.newInputStream(settingFile)) {
			Properties properties = new Properties();
			properties.load(in);

			Iris.queueShaderPackOptionsFromProperties(properties);

			this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackOptions.importedSettings",
					settingFile.getFileName().toString()
			).setStyle(new Style().setColor(TextFormatting.YELLOW).setItalic(true));
			this.notificationDialogTimer = 100; // 5 seconds (100 ticks)

			if (this.navigation != null) {
				this.navigation.refresh();
			}
		} catch (Exception e) {
			// If the file could not be properly parsed or loaded,
			// log the error and display a message to the user
			Iris.logger.error("Error importing shader settings file \""+ settingFile.toString() +"\"", e);

			this.notificationDialog = new TextComponentTranslation(
					"options.iris.shaderPackOptions.failedImport",
					settingFile.getFileName().toString()
			).setStyle(new Style().setColor(TextFormatting.RED).setItalic(true));
			this.notificationDialogTimer = 100; // 5 seconds (100 ticks)
		}
	}

	@Override
	public void onGuiClosed() {
		if (!dropChanges) {
			applyChanges();
		} else {
			discardChanges();
		}

		this.mc.displayGuiScreen(parent);
	}

	private void dropChangesAndClose() {
		dropChanges = true;
		onGuiClosed();
	}

	public void applyChanges() {
		ShaderPackSelectionList.BaseEntry base = this.shaderPackList.getSelected();

		if (!(base instanceof ShaderPackSelectionList.ShaderPackEntry)) {
			return;
		}

		ShaderPackSelectionList.ShaderPackEntry entry = (ShaderPackSelectionList.ShaderPackEntry)base;
		this.shaderPackList.setApplied(entry);

		String name = entry.getPackName();

		// If the pack is being changed, clear pending options from the previous pack to
		// avoid possible undefined behavior from applying one pack's options to another pack
		if (!name.equals(Iris.getCurrentPackName())) {
			Iris.clearShaderPackOptionQueue();
		}

		boolean enabled = this.shaderPackList.getTopButtonRow().shadersEnabled;

		String previousPackName = Iris.getIrisConfig().getShaderPackName().orElse(null);
		boolean previousShadersEnabled = Iris.getIrisConfig().areShadersEnabled();

		// Only reload if the pack would be different from before, or shaders were toggled, or options were changed, or if we're about to reset options.
		if (!name.equals(previousPackName) || enabled != previousShadersEnabled || !Iris.getShaderPackOptionQueue().isEmpty() || Iris.shouldResetShaderPackOptionsOnNextReload()) {
			Iris.getIrisConfig().setShaderPackName(name);
			IrisApi.getInstance().getConfig().setShadersEnabledAndApply(enabled);
		}

		refreshForChangedPack();
	}

	private void discardChanges() {
		Iris.clearShaderPackOptionQueue();
	}

	private void openShaderPackFolder() {
		CompletableFuture.runAsync(() -> open(Iris.getShaderpacksDirectoryManager().getDirectoryUri().toString()));
	}

	private static String[] getURLOpenCommand(String url) {
		switch (Util.getOSType()) {
			case WINDOWS:
				return new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
			case OSX:
				return new String[]{"open", url};
			case UNKNOWN:
			case LINUX:
			case SOLARIS:
				return new String[]{"xdg-open", url};
			default:
				throw new IllegalArgumentException("Unexpected OS Type");
		}
	}

	public static void open(String url) {
		try {
			Runtime.getRuntime().exec(getURLOpenCommand(url));
		} catch (IOException exception) {
			Iris.logger.error("Couldn't open url '{}'", url, exception);
		}
	}

	// Let the screen know if an element is hovered or not, allowing for accurately updating which element is hovered
	public void setElementHoveredStatus(AbstractElementWidget<?> widget, boolean hovered) {
		if (hovered && widget != this.hoveredElement) {
			this.hoveredElement = widget;

			if (widget instanceof CommentedElementWidget) {
				this.hoveredElementCommentTitle = ((CommentedElementWidget<?>) widget).getCommentTitle();

				Optional<ITextComponent> commentBody = ((CommentedElementWidget<?>) widget).getCommentBody();
				if (!commentBody.isPresent()) {
					this.hoveredElementCommentBody.clear();
				} else {
					String rawCommentBody = commentBody.get().getFormattedText();

					// Strip any trailing "."s
					if (rawCommentBody.endsWith(".")) {
						rawCommentBody = rawCommentBody.substring(0, rawCommentBody.length() - 1);
					}
					// Split comment body into lines by separator ". "
					List<ITextComponent> splitByPeriods = Arrays.stream(rawCommentBody.split("\\. [ ]*")).map(TextComponentString::new).collect(Collectors.toList());
					// Line wrap
					this.hoveredElementCommentBody = new ArrayList<>();
					for (ITextComponent text : splitByPeriods) {
						this.hoveredElementCommentBody.addAll(this.fontRenderer.listFormattedStringToWidth(text.getFormattedText(), COMMENT_PANEL_WIDTH - 8));
					}
				}
			} else {
				this.hoveredElementCommentTitle = Optional.empty();
				this.hoveredElementCommentBody.clear();
			}

			this.hoveredElementCommentTimer = 0;
		} else if (!hovered && widget == this.hoveredElement) {
			this.hoveredElement = null;
			this.hoveredElementCommentTitle = Optional.empty();
			this.hoveredElementCommentBody.clear();
			this.hoveredElementCommentTimer = 0;
		}
	}

	public boolean isDisplayingComment() {
		return this.hoveredElementCommentTimer > 20 &&
				this.hoveredElementCommentTitle.isPresent() &&
				!this.hoveredElementCommentBody.isEmpty();
	}
}
