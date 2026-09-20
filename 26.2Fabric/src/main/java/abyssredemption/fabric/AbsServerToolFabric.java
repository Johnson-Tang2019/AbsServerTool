package abyssredemption.fabric;

import abyssredemption.AbsServerTool;
import abyssredemption.command.AbsServerCommands;
import abyssredemption.network.AbsServerNetworking;
import abyssredemption.snapshot.SnapshotScheduler;
import abyssredemption.snapshot.SnapshotStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class AbsServerToolFabric implements ModInitializer {
    @Override public void onInitialize() {
        AbsServerTool.initialize();
        AbsServerNetworking.initialize();
        ServerTickEvents.END_SERVER_TICK.register(SnapshotScheduler::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(SnapshotStore::recordCleanShutdown);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AbsServerCommands.register(dispatcher));
        AbsServerTool.LOGGER.info("Registered Fabric server platform for protocol v2");
    }
}
