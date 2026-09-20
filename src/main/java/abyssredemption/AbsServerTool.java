package abyssredemption;

import abyssredemption.config.ConfigManager;
import abyssredemption.snapshot.SnapshotScheduler;
import abyssredemption.snapshot.SnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AbsServerTool {
    public static final String MOD_ID = "absservertool";
    public static final Logger LOGGER = LoggerFactory.getLogger("AbsServerTool");

    public static void initialize() {
        LOGGER.info("Initializing AbsServerTool 0.2.4");
        ConfigManager.load();
        SnapshotStore.initialize();
    }
}
