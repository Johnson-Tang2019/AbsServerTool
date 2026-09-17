package abyssredemption;

import abyssredemption.command.AbsServerCommands;
import abyssredemption.config.ConfigManager;
import abyssredemption.stats.BlockPlacementService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AbsServerTool implements ModInitializer {
    public static final String MOD_ID = "absservertool";
    public static final Logger LOGGER = LoggerFactory.getLogger("AbsServerTool");
    public static final BlockPlacementService PLACEMENTS = new BlockPlacementService();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing AbsServerTool 0.1.0");
        ConfigManager.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AbsServerCommands.register(dispatcher));
        LOGGER.info("Registered server commands");
    }
}
