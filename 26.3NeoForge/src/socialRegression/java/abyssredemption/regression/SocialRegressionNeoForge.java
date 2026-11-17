package abyssredemption.regression;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("absservertool_social_regression")
public final class SocialRegressionNeoForge {
    public SocialRegressionNeoForge() {
        var scenario=new SocialRegressionScenario();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->scenario.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e)->scenario.started(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e)->scenario.tick(e.getServer()));
    }
}
