package abyssredemption.neoforge;

import abyssredemption.AbsServerTool;
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
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> QQScheduler.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> SnapshotStore.recordCleanShutdown(event.getServer()));
        AbsServerTool.LOGGER.info("Registered NeoForge server platform for protocol v2");
    }
}
