package dev.entropy159.taczturrets.config;

import dev.entropy159.taczturrets.turret.state.HealthBarStyle;
import dev.entropy159.taczturrets.turret.state.RecoilType;
import dev.entropy159.taczturrets.util.ItemFilter;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<HealthBarStyle> HEALTH_BAR_STYLE = BUILDER.comment("Turret health bar style. GREEN_TO_RED fades green to orange to red as health drops, COLOR uses healthBarColor.").defineEnum("healthBarStyle", HealthBarStyle.GREEN_TO_RED);

    public static final ModConfigSpec.ConfigValue<String> HEALTH_BAR_COLOR = BUILDER.comment("Turret health bar colour as hex.").define("healthBarColor", "#FF3030");

    public static final ModConfigSpec.EnumValue<RecoilType> RECOIL_TYPE = BUILDER.comment("Turret recoil type. BOUNCE kicks the barrel up, PUSH slides the gun backwards.").defineEnum("recoilType", RecoilType.PUSH);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int getHealthBarColor() {
        return parseColor(HEALTH_BAR_COLOR.get());
    }

    public static int parseColor(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return (int) (Long.parseLong(value, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return 0xFF3030;
        }
    }
}
