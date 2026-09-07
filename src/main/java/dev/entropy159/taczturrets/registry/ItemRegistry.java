package dev.entropy159.taczturrets.registry;

import com.tacz.guns.init.ModCreativeTabs;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.entropy159.taczturrets.turret.TurretItem;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import static dev.entropy159.taczturrets.TACZTurrets.REGISTRATE;

public class ItemRegistry {
    public static final ItemEntry<TurretItem> TURRET = REGISTRATE.item("turret", TurretItem::new).recipe((context, provider) -> {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, context.get()).pattern("I I").pattern(" B ").pattern("E E").define('I', Tags.Items.INGOTS_IRON).define('B', Items.IRON_BARS).define('E', Tags.Items.STORAGE_BLOCKS_IRON).unlockedBy("has_" + provider.safeName(context.getId()), RegistrateRecipeProvider.has(Items.IRON_BARS)).save(provider, provider.safeId(context.get()));
    }).model((ctx, provider) -> {}).tab(ModCreativeTabs.OTHER_TAB.getKey()).register();

    public static void init() {}
}
