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
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@SuppressWarnings("unused")
@Getter
public class PlayerUtils {
    private static final String DEFAULT_SKIN_URL = "https://textures.minecraft.net/texture/60a5bd016b3c9a1b9272e4929e30827a67be4ebb219017adbbc4a4d22ebd5b1";

    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int READ_TIMEOUT = 5000;


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
        try {
            getPlayerHead(UUID.fromString(PLACEHOLDER_PROFILE));
            Bukkit.getLogger().info("[HologramLib] Placeholder head loaded!");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[HologramLib] Failed to load placeholder head: " + e.getMessage());
        }
    }

    public static CompletableFuture<String> getPlayerHeadAsync(UUID uuid) {
        Optional<String> cached = playerHeadCache.get(uuid);
        if (cached != null && cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }
        return CompletableFuture.supplyAsync(() -> getPlayerHead(uuid))
                .exceptionally(ex -> {
                    Bukkit.getLogger().log(Level.WARNING, "Failed to load player head async for " + uuid + ": " + ex.getMessage());
                    return null;
                });
    }

    public static PlayerProfile getPlayerProfile(UUID uuid) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        try {
            String skinUrl = PlayerUtils.getPlayerSkinUrl(uuid);
            if (skinUrl == null || skinUrl.isEmpty()) {
                Bukkit.getLogger().log(Level.WARNING, "Could not find skin url for " + uuid + ", using default");
                skinUrl = DEFAULT_SKIN_URL;
            }
            URL url = new URI(skinUrl).toURL();
            textures.setSkin(url);
        } catch (IOException | URISyntaxException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to set skin texture: " + e.getMessage());
        }
        profile.setTextures(textures);
        return profile;
    }



    /**
     * Create a URL connection with proper timeouts
     */
    private static URLConnection createConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "HologramLib/1.8.2");
        return connection;
    }

    public static Optional<UUID> getUUID(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return Optional.empty();
        }

        String key = playerName.toLowerCase().trim();

        synchronized (uuidCache) {
            Optional<UUID> cached = uuidCache.get(key);
            if (cached != null) {
                return cached;
            }
        }

        try {
            URL url = new URI("https://api.mojang.com/users/profiles/minecraft/" + key).toURL();
            URLConnection connection = createConnection(url);

            try (InputStream stream = connection.getInputStream()) {
                String response = new String(stream.readAllBytes());

                if (response.isEmpty()) {
                    Optional<UUID> result = Optional.empty();
                    synchronized (uuidCache) {
                        uuidCache.put(key, result);
                    }
                    return result;
                }

                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                if (!json.has("id")) {
                    Optional<UUID> result = Optional.empty();
                    synchronized (uuidCache) {
                        uuidCache.put(key, result);
                    }
                    return result;
                }

                String id = json.get("id").getAsString();
                UUID uuid = UUID.fromString(id.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"));
                Optional<UUID> result = Optional.of(uuid);
                synchronized (uuidCache) {
                    uuidCache.put(key, result);
                }
                return result;
            }
        } catch (SocketTimeoutException e) {
            Bukkit.getLogger().log(Level.WARNING, "Timeout while fetching UUID for " + key);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Network error while fetching UUID for " + key + ": " + e.getMessage());
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error fetching UUID for " + key + ": " + e.getMessage());
        }

        Optional<UUID> result = Optional.empty();
        synchronized (uuidCache) {
            uuidCache.put(key, result);
        }
        return result;
    }

    /**
     * Get a combination of unicodes which represent pixels and offsets
     * to display a player head
     */
    public static String getPlayerHead(UUID uuid) {
        if (uuid == null) {
            Bukkit.getLogger().log(Level.WARNING, "Attempted to get player head with null UUID");
            return null;
        }

        synchronized (playerHeadCache) {
            Optional<String> cached = playerHeadCache.get(uuid);
            if (cached != null) {
                return cached.orElse(null);
            }
        }

        if (!loadingHeads.add(uuid)) {
            Bukkit.getLogger().log(Level.FINE, "Already loading head for " + uuid);
            return null;
        }

        try {
            String skinUrl = getPlayerSkinUrl(uuid);
            if (skinUrl == null) {
                Bukkit.getLogger().log(Level.WARNING, "Could not retrieve skin URL for " + uuid);
                synchronized (playerHeadCache) {
                    playerHeadCache.put(uuid, Optional.empty());
                }
                return null;
            }

            String[] hexColors = getPixelColorsFromSkin(skinUrl);
            if (hexColors == null || hexColors.length < 64) {
                Bukkit.getLogger().log(Level.WARNING, "Invalid pixel colors for " + uuid);
                synchronized (playerHeadCache) {
                    playerHeadCache.put(uuid, Optional.empty());
                }
                return null;
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
            Bukkit.getLogger().log(Level.WARNING, "Failed to generate player head for " + uuid + ": " + e.getMessage());
            synchronized (playerHeadCache) {
                playerHeadCache.put(uuid, Optional.empty());
            }
            return null;
        } finally {
            loadingHeads.remove(uuid);
        }
    }

    @Nullable
    private static String[] getPixelColorsFromSkin(String playerSkinUrl) {
        if (playerSkinUrl == null || playerSkinUrl.isEmpty()) {
            return null;
        }

        String[] colors = new String[64];
        BufferedImage skin = null;

        try {
            URL url = new URL(playerSkinUrl);
            URLConnection connection = createConnection(url);

            try (InputStream inputStream = connection.getInputStream()) {
                skin = ImageIO.read(inputStream);
            }

            if (skin == null) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to read skin image from URL: " + playerSkinUrl);
                return null;
            }

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
            return colors;
        } catch (SocketTimeoutException e) {
            Bukkit.getLogger().log(Level.WARNING, "Timeout while downloading skin from: " + playerSkinUrl);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "IO error while reading skin from " + playerSkinUrl + ": " + e.getMessage());
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error processing skin from " + playerSkinUrl + ": " + e.getMessage());
        } finally {
            if (skin != null) {
                skin.flush();
            }
        }
        return null;
    }

    @Nullable
    public static String getPlayerSkinUrl(UUID uuid) {
        if (uuid == null) {
            Bukkit.getLogger().log(Level.WARNING, "Attempted to get skin URL with null UUID");
            return DEFAULT_SKIN_URL;
        }

        synchronized (skinUrlCache) {
            Optional<String> cached = skinUrlCache.get(uuid);
            if (cached != null) {
                return cached.orElse(DEFAULT_SKIN_URL);
            }
        }


        InputStreamReader reader = null;
        try {
            String uuidString = uuid.toString().replace("-", "");
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidString);
            URLConnection connection = createConnection(url);

            reader = new InputStreamReader(connection.getInputStream());
            JsonObject profile = JsonParser.parseReader(reader).getAsJsonObject();

            if (!profile.has("properties")) {
                Bukkit.getLogger().log(Level.FINE, "No properties found for UUID " + uuid);
                synchronized (skinUrlCache) {
                    skinUrlCache.put(uuid, Optional.of(DEFAULT_SKIN_URL));
                }
                return DEFAULT_SKIN_URL;
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
                    DEFAULT_SKIN_URL;

            Optional<String> result = Optional.of(skinUrl != null ? skinUrl : DEFAULT_SKIN_URL);
            synchronized (skinUrlCache) {
                skinUrlCache.put(uuid, result);
            }
            return skinUrl != null ? skinUrl : DEFAULT_SKIN_URL;
        } catch (SocketTimeoutException e) {
            Bukkit.getLogger().log(Level.WARNING, "Timeout while fetching skin URL for " + uuid);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Network error while fetching skin URL for " + uuid + ": " + e.getMessage());
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error fetching skin URL for " + uuid + ": " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        synchronized (skinUrlCache) {
            skinUrlCache.put(uuid, Optional.of(DEFAULT_SKIN_URL));
        }
        return DEFAULT_SKIN_URL;
    }
}