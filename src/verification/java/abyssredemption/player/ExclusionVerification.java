package abyssredemption.player;

import java.nio.file.Files;
import java.util.UUID;

public final class ExclusionVerification {
    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("player-exclusion-verification-");
        var file = directory.resolve("excluded.json");
        UUID bot = UUID.randomUUID(), human = UUID.randomUUID();
        try {
            var exclusions = new ExcludedPlayers(file);
            check(exclusions.containsIdentity(bot, "Bot", true), "Live fake is excluded without a stored identity");
            check(!exclusions.containsIdentity(human, "BotFan", false), "Names containing bot must not be guessed");
            exclusions.edit(bot.toString(), true);
            check(exclusions.revision() == 1, "Cache invalidation on edit");
            var restarted = new ExcludedPlayers(file);
            check(restarted.containsIdentity(bot, "Unknown", false), "Offline fake remains excluded after restart");
            check(!restarted.containsIdentity(human, "Human", false), "Offline real player remains included");
            restarted.edit("LegacyBot", true);
            check(restarted.containsIdentity(human, "LEGACYBOT", false), "Explicit names override online real-player path too");
            restarted.edit("legacybot", false);
            restarted.edit(bot.toString(), false);
            check(!restarted.containsIdentity(human, "LegacyBot", false), "Name removal restores statistics");
            check(!new ExcludedPlayers(file).containsIdentity(bot, "Bot", false), "UUID removal persists");
            Files.writeString(file, "{broken");
            boolean rejected = false;
            try { new ExcludedPlayers(file).edit(bot.toString(), true); }
            catch (java.io.IOException expected) { rejected = true; }
            check(rejected && Files.readString(file).equals("{broken"), "Never overwrite a corrupt exclusion file");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
        System.out.println("EXCLUSION_VERIFICATION_PASSED: live/offline filtering, restart, explicit names, removal, cache revision, corrupt-file preservation");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
