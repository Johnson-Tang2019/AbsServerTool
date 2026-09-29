package abyssredemption.player;

import abyssredemption.AbsServerTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Persists observed Carpet fake identities and permits explicit legacy exclusions. */
public final class ExcludedPlayers {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "absservertool-excluded-players.json");
    private static final ExcludedPlayers INSTANCE = new ExcludedPlayers();
    private final Set<UUID> uuids = new HashSet<>();
    private final Set<String> names = new HashSet<>();
    private final PlayerNameResolver resolver = new PlayerNameResolver();
    private boolean loaded;

    private ExcludedPlayers() {}

    public static ExcludedPlayers get() { return INSTANCE; }

    private synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                save();
                return;
            }
            Data data = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Data.class);
            if (data == null) return;
            if (data.uuids != null) for (String value : data.uuids) {
                try { uuids.add(UUID.fromString(value)); }
                catch (IllegalArgumentException exception) { AbsServerTool.LOGGER.warn("Invalid excluded UUID {}", value); }
            }
            if (data.names != null) for (String name : data.names) if (name != null && !name.isBlank()) names.add(name.toLowerCase(java.util.Locale.ROOT));
        } catch (Exception exception) {
            AbsServerTool.LOGGER.warn("Unable to load excluded player identities", exception);
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(new Data(uuids.stream().map(UUID::toString).sorted().toList(), names.stream().sorted().toList())), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            AbsServerTool.LOGGER.warn("Unable to save excluded player identities", exception);
        }
    }

    public synchronized void observe(MinecraftServer server) {
        load();
        boolean changed = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CarpetFakePlayerDetector.isFake(player) && !CarpetFakePlayerDetector.isShadow(player)) changed |= uuids.add(player.getUUID());
        }
        if (changed) save();
    }

    public synchronized boolean contains(MinecraftServer server, UUID uuid) {
        load();
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null && CarpetFakePlayerDetector.isFake(online)) return true;
        if (online != null && !CarpetFakePlayerDetector.isFake(online)) return false;
        if (uuids.contains(uuid)) return true;
        String name = resolver.resolve(server, uuid);
        return names.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    private record Data(List<String> uuids, List<String> names) {}
}
