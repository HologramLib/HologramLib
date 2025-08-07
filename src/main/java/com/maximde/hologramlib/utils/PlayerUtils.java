package com.maximde.hologramlib.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressWarnings("unused")
@Getter
public class PlayerUtils {
    private static final String DEFAULT_SKIN_URL = "https://textures.minecraft.net/texture/60a5bd016b3c9a1b9272e4929e30827a67be4ebb219017adbbc4a4d22ebd5b1";

    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(maxSize, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }

    private static final LRUCache<String, Optional<UUID>> uuidCache = new LRUCache<>(1000);
    private static final LRUCache<UUID, Optional<String>> skinUrlCache = new LRUCache<>(1000);
    private static final LRUCache<UUID, Optional<String>> playerHeadCache = new LRUCache<>(1000);

    public static String PLACEHOLDER_PROFILE = "6d01fd6b-43ec-4294-b4f7-00dd3c330648";

    private static final Set<UUID> loadingHeads = ConcurrentHashMap.newKeySet();

    public static void loadPlaceholders() {
        getPlayerHead(UUID.fromString(PLACEHOLDER_PROFILE));
        Bukkit.getLogger().info("[HologramLib] Placeholder head loaded!");
    }

    public static CompletableFuture<String> getPlayerHeadAsync(UUID uuid) {
        Optional<String> cached = playerHeadCache.get(uuid);
        if (cached != null && cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }
        return CompletableFuture.supplyAsync(() -> getPlayerHead(uuid));
    }

    public static PlayerProfile getPlayerProfile(UUID uuid) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        try {
            String skinUrl = PlayerUtils.getPlayerSkinUrl(uuid);
            if (skinUrl == null) {
                Bukkit.getLogger().log(Level.WARNING, "Could not find skin url for " + uuid);
                return profile;
            }
            URL url = new URI(skinUrl).toURL();
            textures.setSkin(url);
        } catch (IOException | URISyntaxException e) {
            Bukkit.getLogger().log(Level.WARNING, e.getMessage());
        }
        profile.setTextures(textures);
        return profile;
    }

    public static Optional<UUID> getUUID(String playerName) {
        String key = playerName.toLowerCase();
        synchronized (uuidCache) {
            Optional<UUID> cached = uuidCache.get(key);
            if (cached != null) {
                return cached;
            }
        }
        try {
            var url = new URI("https://api.mojang.com/users/profiles/minecraft/" + key).toURL();
            try (InputStream stream = url.openStream()) {
                var response = new String(stream.readAllBytes());
                String id = JsonParser.parseString(response)
                        .getAsJsonObject()
                        .get("id")
                        .getAsString();
                UUID uuid = UUID.fromString(id.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"));
                Optional<UUID> result = Optional.of(uuid);
                synchronized (uuidCache) {
                    uuidCache.put(key, result);
                }
                return result;
            }
        } catch (Exception e) {
            Optional<UUID> result = Optional.empty();
            synchronized (uuidCache) {
                uuidCache.put(key, result);
            }
            return result;
        }
    }

    /**
     * Get a combination of unicodes which represent pixels and offsets
     * to display a player head
     * @return
     */
    public static String getPlayerHead(UUID uuid) {
        if (uuid == null) return null;

        synchronized (playerHeadCache) {
            Optional<String> cached = playerHeadCache.get(uuid);
            if (cached != null) {
                return cached.orElse(null);
            }
        }

        try {
            String[] hexColors = getPixelColorsFromSkin(getPlayerSkinUrl(uuid));
            if (hexColors.length < 64) {
                throw new IllegalArgumentException("Hex colors must have at least 64 elements.");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<font:minecraft:playerhead>");

            for (int i = 0; i < 64; i++) {
                char base = (char) ('\uF001' + (i % 8));
                String glyph = (i == 63
                        ? String.valueOf(base)
                        : (i % 8 == 7
                        ? base + "\uF101"
                        : base + "\uF102"));
                String hex = hexColors[i].substring(1);
                sb.append("<#").append(hex).append(">")
                        .append(glyph);
            }
            sb.append("<font:minecraft:default>");

            String result = sb.toString();

            synchronized (playerHeadCache) {
                playerHeadCache.put(uuid, Optional.of(result));
            }

            return result;
        } catch (Exception e) {
            synchronized (playerHeadCache) {
                playerHeadCache.put(uuid, Optional.empty());
            }
            return null;
        }
    }

    private static String[] getPixelColorsFromSkin(String playerSkinUrl) {
        String[] colors = new String[64];
        try {
            BufferedImage skin = ImageIO.read(new URL(playerSkinUrl));
            boolean overlay = skin.getHeight() >= 64;

            BufferedImage face = skin.getSubimage(8, 8, 8, 8);
            BufferedImage overlayImg = overlay ? skin.getSubimage(40, 8, 8, 8) : null;

            for (int i = 0, idx = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    int rgb = face.getRGB(i, j);
                    if (overlay && overlayImg != null) {
                        int overlayRgb = overlayImg.getRGB(i, j);
                        rgb = (overlayRgb >>> 24 != 0) ? overlayRgb : rgb;
                    }
                    colors[idx++] = String.format("#%06X", rgb & 0xFFFFFF);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return colors;
    }

    @Nullable
    public static String getPlayerSkinUrl(UUID uuid) {
        if (uuid == null) return DEFAULT_SKIN_URL;

        synchronized (skinUrlCache) {
            Optional<String> cached = skinUrlCache.get(uuid);
            if (cached != null) {
                return cached.orElse(null);
            }
        }


        try {
            InputStreamReader reader = new InputStreamReader(new URL(
                    "https://sessionserver.mojang.com/session/minecraft/profile/" +
                            uuid.toString().replace("-", "")).openConnection().getInputStream());

            JsonObject profile = JsonParser.parseReader(reader).getAsJsonObject();
            if (!profile.has("properties")) {
                synchronized (skinUrlCache) {
                    skinUrlCache.put(uuid, Optional.empty());
                }
                return null;
            }

            String encodedTextures = profile.getAsJsonArray("properties")
                    .get(0).getAsJsonObject()
                    .get("value").getAsString();

            JsonObject textures = JsonParser.parseString(
                            new String(Base64.getDecoder().decode(encodedTextures)))
                    .getAsJsonObject()
                    .getAsJsonObject("textures");

            String skinUrl = textures.has("SKIN") ?
                    textures.getAsJsonObject("SKIN").get("url").getAsString() :
                    null;

            Optional<String> result = Optional.ofNullable(skinUrl);
            synchronized (skinUrlCache) {
                skinUrlCache.put(uuid, result);
            }
            return skinUrl;
        } catch (Exception exception) {
            synchronized (skinUrlCache) {
                skinUrlCache.put(uuid, Optional.of(DEFAULT_SKIN_URL));
            }
            return DEFAULT_SKIN_URL;
        }
    }
}