package abyssredemption.regression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class SocialRegression implements ModInitializer {
    public void onInitialize() {
        var scenario=new SocialRegressionScenario();
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->scenario.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(scenario::started);
        ServerTickEvents.END_SERVER_TICK.register(scenario::tick);
    }
}
