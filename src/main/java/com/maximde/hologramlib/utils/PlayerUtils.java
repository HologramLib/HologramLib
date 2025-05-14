package com.maximde.hologramlib.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class PlayerUtils {

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
        try {
            var url = new URI("https://api.mojang.com/users/profiles/minecraft/" + playerName).toURL();

            try (InputStream stream = url.openStream()) {
                var response = new String(stream.readAllBytes());
                return Optional.of(UUID.fromString(JsonParser.parseString(response)
                        .getAsJsonObject()
                        .get("id")
                        .getAsString()
                        .replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
            }

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Nullable
    public static String getPlayerSkinUrl(UUID uuid) {
        if(uuid == null) return "https://textures.minecraft.net/texture/60a5bd016b3c9a1b9272e4929e30827a67be4ebb219017adbbc4a4d22ebd5b1";
        try {
            InputStreamReader reader = new InputStreamReader(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" +
                    uuid.toString().replace("-", "")).openConnection().getInputStream());

                JsonObject profile = JsonParser.parseReader(reader).getAsJsonObject();
                if (!profile.has("properties")) return null;

                String encodedTextures = profile.getAsJsonArray("properties")
                        .get(0).getAsJsonObject()
                        .get("value").getAsString();

                JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(encodedTextures)))
                        .getAsJsonObject()
                        .getAsJsonObject("textures");

                return textures.has("SKIN") ? textures.getAsJsonObject("SKIN").get("url").getAsString() : null;
        } catch (Exception exception) {
            return "https://textures.minecraft.net/texture/60a5bd016b3c9a1b9272e4929e30827a67be4ebb219017adbbc4a4d22ebd5b1";
        }
    }
}
