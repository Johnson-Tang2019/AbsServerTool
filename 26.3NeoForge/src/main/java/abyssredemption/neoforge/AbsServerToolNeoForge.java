package abyssredemption.neoforge;

import abyssredemption.AbsServerTool;
import abyssredemption.performance.PerformanceProfiler;
import abyssredemption.command.AbsServerCommands;
import abyssredemption.snapshot.SnapshotScheduler;
import abyssredemption.snapshot.SnapshotStore;
import abyssredemption.qq.QQCommands;
import abyssredemption.qq.QQConfigStore;
import abyssredemption.qq.QQScheduler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.server.level.ServerLevel;

@Mod(AbsServerTool.MOD_ID)
public final class AbsServerToolNeoForge {
    public AbsServerToolNeoForge(IEventBus modEventBus) {
        AbsServerTool.initialize();
        QQConfigStore.load();
        modEventBus.addListener(AbsServerNetworkingNeoForge::register);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            AbsServerCommands.register(event.getDispatcher());
            QQCommands.register(event.getDispatcher());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> SnapshotScheduler.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre event) -> PerformanceProfiler.onServerTickStart(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PerformanceProfiler.onServerTickEnd(event.getServer()));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> {
            if (event.getLevel() instanceof ServerLevel level) PerformanceProfiler.onLevelTickStart(level);
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) PerformanceProfiler.onLevelTickEnd(level);
        });
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (PerformanceProfiler.isRunning() && event.getLevel() instanceof ServerLevel level)
                PerformanceProfiler.onChunkLoad(level, event.getChunk().getPos(), event.isNewChunk());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> QQScheduler.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> SnapshotStore.recordCleanShutdown(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> PerformanceProfiler.onServerStopping());
        AbsServerTool.LOGGER.info("Registered NeoForge server platform for protocol v2");
    }
}
