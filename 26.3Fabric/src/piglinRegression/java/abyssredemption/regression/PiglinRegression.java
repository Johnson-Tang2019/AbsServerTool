package abyssredemption.regression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Test-only Fabric adapter for the shared real-server scenario. */
public final class PiglinRegression implements ModInitializer {
    public void onInitialize() {
        var scenario = new PiglinRegressionScenario();
        ServerLifecycleEvents.SERVER_STARTED.register(scenario::started);
        ServerTickEvents.END_SERVER_TICK.register(scenario::tick);
    }
}
