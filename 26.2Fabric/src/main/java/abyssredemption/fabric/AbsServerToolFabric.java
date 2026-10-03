package abyssredemption.fabric;

import abyssredemption.AbsServerTool;
import abyssredemption.command.AbsServerCommands;
import abyssredemption.network.AbsServerNetworking;
import abyssredemption.performance.PerformanceProfiler;
import abyssredemption.snapshot.SnapshotScheduler;
import abyssredemption.snapshot.SnapshotStore;
import abyssredemption.qq.QQCommands;
import abyssredemption.qq.QQConfigStore;
import abyssredemption.qq.QQScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

public final class AbsServerToolFabric implements ModInitializer {
    @Override public void onInitialize() {
        AbsServerTool.initialize();
        QQConfigStore.load();
        AbsServerNetworking.initialize();
        ServerTickEvents.START_SERVER_TICK.register(PerformanceProfiler::onServerTickStart);
        ServerTickEvents.END_SERVER_TICK.register(PerformanceProfiler::onServerTickEnd);
        ServerTickEvents.START_LEVEL_TICK.register(PerformanceProfiler::onLevelTickStart);
        ServerTickEvents.END_LEVEL_TICK.register(PerformanceProfiler::onLevelTickEnd);
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (PerformanceProfiler.isRunning()) PerformanceProfiler.onChunkLoad(level, chunk.getPos(), generated);
        });
        ServerTickEvents.END_SERVER_TICK.register(SnapshotScheduler::tick);
        ServerTickEvents.END_SERVER_TICK.register(QQScheduler::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(SnapshotStore::recordCleanShutdown);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PerformanceProfiler.onServerStopping());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AbsServerCommands.register(dispatcher);
            QQCommands.register(dispatcher);
        });
        AbsServerTool.LOGGER.info("Registered Fabric server platform for protocol v2");
    }
}
