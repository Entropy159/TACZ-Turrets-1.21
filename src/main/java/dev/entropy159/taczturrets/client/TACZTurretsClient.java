package dev.entropy159.taczturrets.client;

import dev.entropy159.taczturrets.TACZTurrets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TACZTurrets.MODID, dist = Dist.CLIENT)
public class TACZTurretsClient {
    public TACZTurretsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
