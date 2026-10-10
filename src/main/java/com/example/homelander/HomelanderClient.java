package com.example.homelander;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HomelanderClient implements ClientModInitializer {
    private static int tickCounter;
    private static boolean laserWasHeld;
    private static boolean speedWasHeld;
    private static boolean hudVisible = true;
    private static KeyBinding hudToggle;
    private static final Map<String, KeyBinding> ABILITY_KEYS = new LinkedHashMap<>();

    @Override
    public void onInitializeClient() {
        final KeyBinding laser = bind("laser", GLFW.GLFW_KEY_H);
        final KeyBinding flight = bind("flight", GLFW.GLFW_KEY_V);
        final KeyBinding wave = bind("shockwave", GLFW.GLFW_KEY_G);
        final KeyBinding speed = bind("speed", GLFW.GLFW_KEY_R);
        final KeyBinding punch = bind("punch", GLFW.GLFW_KEY_B);
        final KeyBinding land = bind("landing", GLFW.GLFW_KEY_N);
        final KeyBinding sonic = bind("sonic", GLFW.GLFW_KEY_F);
        final KeyBinding pull = bind("pull", GLFW.GLFW_KEY_C);
        final KeyBinding vision = bind("vision", GLFW.GLFW_KEY_X);
        final KeyBinding shield = bind("shield", GLFW.GLFW_KEY_Z);
        final KeyBinding hud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.homelander.hud", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J, "category.homelander.powers"));

        hudToggle = hud;
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> renderAbilityPanel(drawContext));
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new HomelanderCapeFeature(playerRenderer));
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) {
                laserWasHeld = false;
                speedWasHeld = false;
                return;
            }
            tickCounter++;
            while (hud.wasPressed()) hudVisible = !hudVisible;
            // isPressed() means the key is physically held; wasPressed() fires only once.
            boolean laserHeld = client.currentScreen == null && laser.isPressed();
            boolean speedHeld = client.currentScreen == null && speed.isPressed();
            if (laserHeld && (!laserWasHeld || tickCounter % 3 == 0)) send((byte) 0);
            if (!laserHeld && laserWasHeld) send((byte) 6);
            if (speedHeld && (!speedWasHeld || tickCounter % 3 == 0)) send((byte) 3);
            if (!speedHeld && speedWasHeld) send((byte) 7);
            laserWasHeld = laserHeld;
            speedWasHeld = speedHeld;
            while (flight.wasPressed()) send((byte) 1);
            while (wave.wasPressed()) send((byte) 2);
            while (punch.wasPressed()) send((byte) 4);
            while (land.wasPressed()) send((byte) 5);
            while (sonic.wasPressed()) send((byte) 8);
            while (pull.wasPressed()) send((byte) 9);
            while (vision.wasPressed()) send((byte) 10);
            while (shield.wasPressed()) send((byte) 11);
        });
    }

    private static KeyBinding bind(String action, int key) {
        KeyBinding binding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.homelander." + action, InputUtil.Type.KEYSYM, key,
                "category.homelander.powers"));
        ABILITY_KEYS.put(action, binding);
        return binding;
    }

    /** A compact, in-world ability guide at the bottom left, with live held-key indicators. */
    private static void renderAbilityPanel(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!hudVisible || client.player == null || client.world == null ||
                client.currentScreen != null || client.options.hudHidden) return;

        TextRenderer font = client.textRenderer;
        final int rowHeight = 11;
        final int panelPadding = 6;
        final int panelX = 7;
        final int panelBottom = context.getScaledWindowHeight() - 7;
        final int panelHeight = 30 + ABILITY_KEYS.size() * rowHeight;
        final int panelY = panelBottom - panelHeight;
        int panelWidth = 175;

        // Size to the localized labels, including user-rebound keys.
        for (Map.Entry<String, KeyBinding> entry : ABILITY_KEYS.entrySet()) {
            String line = keyName(entry.getValue()) + "  " +
                    Text.translatable("hud.homelander." + entry.getKey()).getString();
            panelWidth = Math.max(panelWidth, font.getWidth(line) + 2 * panelPadding + 3);
        }
        // Keep the guide on-screen with small window / GUI scale settings.
        panelWidth = Math.min(panelWidth, context.getScaledWindowWidth() - 14);
        if (panelWidth <= 60 || panelY < 6) return;

        int panelRight = panelX + panelWidth;
        context.fill(panelX, panelY, panelRight, panelBottom, 0xC9081120);
        context.fill(panelX, panelY, panelX + 3, panelBottom, 0xFFFFC84B);
        context.fill(panelX + 3, panelY + 18, panelRight, panelY + 19, 0x80FFC84B);
        context.drawTextWithShadow(font, "HOMELANDER  |  v2.1", panelX + 9, panelY + 5, 0xFFFFCC54);

        int y = panelY + 24;
        for (Map.Entry<String, KeyBinding> entry : ABILITY_KEYS.entrySet()) {
            String action = entry.getKey();
            KeyBinding binding = entry.getValue();
            boolean active = (action.equals("laser") || action.equals("speed")) && binding.isPressed();
            String text = keyName(binding) + "  " + Text.translatable("hud.homelander." + action).getString();
            if (active) context.fill(panelX + 5, y - 1, panelRight - 4, y + rowHeight - 1, 0x604EDB83);
            context.drawTextWithShadow(font, text, panelX + 9, y, active ? 0xFF79FFAE : 0xFFE9F0F8);
            y += rowHeight;
        }
        String footer = keyName(hudToggle) + "  " + Text.translatable("hud.homelander.footer").getString();
        context.drawTextWithShadow(font, footer, panelX + 9, panelBottom - 12, 0xFFACC0D3);
    }

    private static String keyName(KeyBinding binding) {
        return binding.getBoundKeyLocalizedText().getString();
    }

    private static void send(byte action) {
        ClientPlayNetworking.send(new AbilityPayload(action));
    }
}
