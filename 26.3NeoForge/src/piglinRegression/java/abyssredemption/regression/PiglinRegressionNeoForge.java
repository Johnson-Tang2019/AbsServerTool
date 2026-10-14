package abyssredemption.regression;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Test-only NeoForge adapter; never included in release artifacts. */
@Mod("absservertool_piglin_regression")
public final class PiglinRegressionNeoForge {
    public PiglinRegressionNeoForge() {
        var scenario = new PiglinRegressionScenario();
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> scenario.started(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> scenario.tick(event.getServer()));
    }
}
