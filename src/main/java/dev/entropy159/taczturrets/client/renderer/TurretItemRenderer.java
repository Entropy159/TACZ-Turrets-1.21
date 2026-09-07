package dev.entropy159.taczturrets.client.renderer;

import dev.entropy159.taczturrets.client.model.TurretItemModel;
import dev.entropy159.taczturrets.turret.TurretItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TurretItemRenderer extends GeoItemRenderer<TurretItem> {
    public TurretItemRenderer() {
        super(new TurretItemModel());
    }
}
