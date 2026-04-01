package com.noxgg.elementalpower.world;

import com.noxgg.elementalpower.sound.ModSounds;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages combat music (Megalovania) per player.
 * Starts when a player hits a non-player mob, stops when the mob dies.
 */
public class CombatMusicManager {
    // Track which player is currently fighting (has music playing)
    private static final Map<UUID, Boolean> playingMusic = new HashMap<>();

    /**
     * Called when a player damages a non-player living entity.
     * Starts Megalovania if not already playing for this player.
     */
    public static void onPlayerHitMob(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (playingMusic.getOrDefault(playerId, false)) {
            return; // Already playing
        }

        // Start Megalovania
        playingMusic.put(playerId, true);

        player.connection.send(new ClientboundSoundPacket(
                ModSounds.MEGALOVANIA.getHolder().get(),
                SoundSource.MUSIC,
                player.getX(), player.getY(), player.getZ(),
                1.0f, // volume
                1.0f, // pitch
                player.level().random.nextLong()
        ));
    }

    /**
     * Called when any mob dies. If the killer is a player with music playing, stop it.
     */
    public static void onMobKilled(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (!playingMusic.getOrDefault(playerId, false)) {
            return; // No music playing
        }

        // Stop Megalovania
        playingMusic.put(playerId, false);

        player.connection.send(new ClientboundStopSoundPacket(
                new ResourceLocation("elementalpower", "megalovania"),
                SoundSource.MUSIC
        ));
    }

    /**
     * Called when a player disconnects - cleanup.
     */
    public static void onPlayerLogout(UUID playerId) {
        playingMusic.remove(playerId);
    }
}
