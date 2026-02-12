package com.maximde.hologramlib.hook;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;

public class HeadDatabaseHook {

    private static HeadDatabaseAPI api;
    private static boolean available = false;

    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("HeadDatabase") != null) {
            try {
                api = new HeadDatabaseAPI();
                available = true;
                Bukkit.getLogger().info("HeadDatabase API initialized successfully");
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to initialize HeadDatabase API", e);
                available = false;
            }
        } else {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available && api != null;
    }

    public static Optional<ItemStack> getHead(String headId) {
        if (!isAvailable() || headId == null || headId.isEmpty()) {
            return Optional.empty();
        }

        try {
            ItemStack head = api.getItemHead(headId);
            return Optional.ofNullable(head);
        } catch (NullPointerException e) {
            Bukkit.getLogger().warning("Could not find HeadDatabase head with ID: " + headId);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error getting HeadDatabase head: " + headId, e);
        }

        return Optional.empty();
    }

    public static Optional<String> getBase64Texture(String headId) {
        return getHead(headId).flatMap(HeadDatabaseHook::extractBase64Texture);
    }

    public static Optional<String> extractBase64Texture(ItemStack head) {
        if (head == null) {
            return Optional.empty();
        }

        try {
            if (head.getItemMeta() instanceof SkullMeta skullMeta) {
                PlayerProfile profile = skullMeta.getOwnerProfile();
                if (profile != null) {
                    PlayerTextures textures = profile.getTextures();
                    URL skinUrl = textures.getSkin();
                    if (skinUrl != null) {
                        String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl.toString() + "\"}}}";
                        return Optional.of(java.util.Base64.getEncoder().encodeToString(textureJson.getBytes()));
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error extracting texture from head", e);
        }

        return Optional.empty();
    }

    public static Optional<String> getHeadId(ItemStack item) {
        if (!isAvailable() || item == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(api.getItemID(item));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
