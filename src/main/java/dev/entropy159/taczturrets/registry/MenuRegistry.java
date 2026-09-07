package dev.entropy159.taczturrets.registry;

import com.tterrag.registrate.util.entry.MenuEntry;
import dev.entropy159.taczturrets.client.screen.TurretScreen;
import dev.entropy159.taczturrets.menu.TurretMenu;

import static dev.entropy159.taczturrets.TACZTurrets.REGISTRATE;

public class MenuRegistry {
    public static final MenuEntry<TurretMenu> TURRET = REGISTRATE.menu("turret", (type, id, inv, buf) -> new TurretMenu(id, inv, buf), () -> TurretScreen::new).register();

    public static void init() {}
}
