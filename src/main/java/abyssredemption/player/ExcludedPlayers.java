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
    private static final ExcludedPlayers INSTANCE = new ExcludedPlayers(Path.of("config", "absservertool-excluded-players.json"));
    private final Path file;
    private final Set<UUID> uuids = new HashSet<>();
    private final Set<String> names = new HashSet<>();
    private final PlayerNameResolver resolver = new PlayerNameResolver();
    private boolean loaded;

    private long revision;
    ExcludedPlayers(Path file) { this.file = file; }

    public static ExcludedPlayers get() { return INSTANCE; }

    private synchronized void load() {
        if (loaded) return;
        try {
            if (!Files.exists(file)) {
                loaded = true;
                return;
            }
            Data data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (data == null) throw new java.io.IOException("Empty exclusion document");
            if (data.uuids != null) for (String value : data.uuids) {
                try { uuids.add(UUID.fromString(value)); }
                catch (IllegalArgumentException exception) { AbsServerTool.LOGGER.warn("Invalid excluded UUID {}", value); }
            }
            if (data.names != null) for (String name : data.names) if (name != null && !name.isBlank()) names.add(name.toLowerCase(java.util.Locale.ROOT));
            loaded = true;
        } catch (Exception exception) {
            AbsServerTool.LOGGER.warn("Unable to load excluded player identities", exception);
        }
    }

    private void save() throws java.io.IOException {
        if (!loaded) throw new java.io.IOException("Exclusion file could not be loaded; refusing to overwrite it");
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "excluded-players-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(new Data(uuids.stream().map(UUID::toString).sorted().toList(), names.stream().sorted().toList())), StandardCharsets.UTF_8);
            try { Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException exception) { Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public synchronized void observe(MinecraftServer server) {
        load();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            observe(player);
        }
    }

    public synchronized void observe(ServerPlayer player) {
        if (!CarpetFakePlayerDetector.isFake(player) || CarpetFakePlayerDetector.isShadow(player)) return;
        try { edit(player.getUUID().toString(), true); }
        catch (java.io.IOException exception) { AbsServerTool.LOGGER.warn("Unable to persist fake player exclusion {}", player.getUUID(), exception); }
    }

    public synchronized long revision() { return revision; }

    public synchronized void edit(String identity, boolean exclude) throws java.io.IOException {
        load();
        Set<UUID> oldUuids = new HashSet<>(uuids);
        Set<String> oldNames = new HashSet<>(names);
        boolean changed;
        try {
            UUID uuid = UUID.fromString(identity);
            changed = exclude ? uuids.add(uuid) : uuids.remove(uuid);
        } catch (IllegalArgumentException exception) {
            String name = identity.toLowerCase(java.util.Locale.ROOT);
            changed = exclude ? names.add(name) : names.remove(name);
        }
        if (!changed) return;
        try { save(); revision++; }
        catch (java.io.IOException exception) {
            uuids.clear(); uuids.addAll(oldUuids); names.clear(); names.addAll(oldNames);
            throw exception;
        }
    }

    public synchronized String describe() { load(); return "UUID: " + uuids.stream().sorted().toList() + " · 名称: " + names.stream().sorted().toList(); }

    boolean containsIdentity(UUID uuid, String name, boolean fake) {
        load();
        return fake || uuids.contains(uuid) || names.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    public synchronized boolean contains(MinecraftServer server, UUID uuid) {
        load();
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        return containsIdentity(uuid, resolver.resolve(server, uuid), online != null && CarpetFakePlayerDetector.isFake(online));
    }

    private record Data(List<String> uuids, List<String> names) {}
}
