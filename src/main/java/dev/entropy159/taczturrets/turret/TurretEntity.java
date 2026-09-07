package dev.entropy159.taczturrets.turret;

import com.mojang.datafixers.util.Pair;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import dev.entropy159.taczturrets.config.ClientConfig;
import dev.entropy159.taczturrets.config.ServerConfig;
import dev.entropy159.taczturrets.menu.TurretLayout;
import dev.entropy159.taczturrets.menu.TurretMenu;
import dev.entropy159.taczturrets.registry.EntityTypeRegistry;
import dev.entropy159.taczturrets.registry.ItemRegistry;
import dev.entropy159.taczturrets.registry.SoundRegistry;
import dev.entropy159.taczturrets.registry.TagRegistry;
import dev.entropy159.taczturrets.turret.ai.TaczShootAttack;
import dev.entropy159.taczturrets.turret.state.*;
import dev.entropy159.taczturrets.util.HasTurretInventory;
import dev.entropy159.taczturrets.util.TargetFilter;
import dev.entropy159.taczturrets.util.TurretAllies;
import dev.entropy159.taczturrets.util.TurretEnergyStorage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.*;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;
import net.tslat.smartbrainlib.util.BrainUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class TurretEntity extends Mob implements SmartBrainOwner<TurretEntity>, HasTurretInventory, GeoEntity, MenuProvider {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final IGunOperator gunOperator = IGunOperator.fromLivingEntity(this); //LivingEntity is already a gun operator, implementing it here would just be redundant. However, the IDE does not recognize it because it's implemented through a mixin, so this is a small workaround.

    private static final UniformInt ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);
    private static final int TARGET_SPREAD_INTERVAL = 10;
    private static final double TARGET_SPREAD_REACH = 4.0D;
    private static final double TARGET_SPREAD_MIN_RADIUS = 16.0D;
    private static final double TARGET_SWITCH_MARGIN = 0.5D;
    private static final int CONSERVATIVE_SHOT_INTERVAL = 8;
    private static final int RECOIL_TICKS = 3;
    private static final float RECOIL_DEGREES = 9.0F;
    private static final float RECOIL_PUSH = 0.09F;
    private static final int REPAIR_INTERACT_GRACE = 10;
    private static final int RETALIATE_UNTIL_DEATH = -1;
    public static final EntityDataAccessor<String> STATE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> RECOIL = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private boolean gunDrawn = false;
    private TurretEnableType enableType = TurretEnableType.ALWAYS_ON;
    private TurretMode mode = TurretMode.AGGRESSIVE;
    private PlayerTargeting playerTargeting = PlayerTargeting.RETALIATE;
    private String ownerName = "";
    private int lastShotTick = 0;
    private boolean hadTarget = false;
    private int lastRepairTick = -100;
    private UUID retaliateTarget;
    private int retaliateTicks = 0;
    private final ItemStackHandler inventory = new ItemStackHandler(Math.max(1, ServerConfig.TURRET_SLOT_ROWS.getDefault() * ServerConfig.TURRET_SLOT_LENGTH.getDefault()));
    private final List<ItemStack> overflow = new ArrayList<>();
    private final TurretEnergyStorage energy = new TurretEnergyStorage(ServerConfig.ENERGY_CAPACITY.getDefault(), ServerConfig.ENERGY_TRANSFER_RATE.getDefault());
    public UUID owner;

    public TurretEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        gunOperator.initialData();
    }

    public static TurretEntity create(Level level, BlockPos pos, Player player) {
        var turret = EntityTypeRegistry.TURRET.create(level);
        if (turret == null) {
            return null;
        }
        turret.setPos(pos.getCenter());
        turret.owner = player.getUUID();
        turret.ownerName = player.getGameProfile().getName();
        return turret;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, TurretState.NO_GUN.name);
        builder.define(RECOIL, 0);
    }

    public static AttributeSupplier.Builder createLivingAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, Math.max(ServerConfig.TURRET_RANGE.getDefault(), ServerConfig.SNIPER_TURRET_RANGE.getDefault())).add(Attributes.ARMOR, ServerConfig.TURRET_ARMOR.getDefault()).add(Attributes.MAX_HEALTH, ServerConfig.TURRET_HEALTH.getDefault());
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Inventory", inventory.serializeNBT(level().registryAccess()));
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("EnableType", enableType.name());
        tag.putString("Mode", mode.name());
        tag.putString("PlayerTargeting", playerTargeting.name());
        tag.putString("OwnerName", ownerName);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        inventory.deserializeNBT(level().registryAccess(), tag.getCompound("Inventory"));
        resizeInventory();
        if (tag.contains("Owner")) owner = tag.getUUID("Owner");
        enableType = TurretEnableType.byName(tag.getString("EnableType"));
        mode = TurretMode.byName(tag.getString("Mode"));
        playerTargeting = PlayerTargeting.byName(tag.getString("PlayerTargeting"));
        ownerName = tag.getString("OwnerName");
        energy.setEnergy(tag.getInt("Energy"));
    }

    public ItemStack getGunStack() {
        return getMainHandItem();
    }

    public void setGunStack(ItemStack stack) {
        setItemSlot(EquipmentSlot.MAINHAND, stack);
    }

    public ModernKineticGunItem getGunItem() {
        return hasGun() ? (ModernKineticGunItem) getGunStack().getItem() : null;
    }

    public boolean hasMinigun() {
        return hasGun() && TimelessAPI.getGunDisplay(getGunStack()).map(display -> display.getThirdPersonAnimation().equals("minigun")).orElse(false);
    }

    public boolean hasGun() {
        return getGunStack().getItem() instanceof ModernKineticGunItem;
    }

    public boolean gunHasAmmo() {
        if (!hasGun()) return false;
        if (getGunItem().useInventoryAmmo(getGunStack())) {
            return getGunItem().hasInventoryAmmo(this, getGunStack(), gunOperator.needCheckAmmo());
        }
        return getGunItem().getCurrentAmmoCount(getGunStack()) > 0 || hasChamberedRound();
    }

    private boolean hasChamberedRound() {
        ItemStack gunStack = getGunStack();
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null || !iGun.hasBulletInBarrel(gunStack)) return false;
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(gunStack))
                .map(index -> index.getGunData().getBolt() != Bolt.OPEN_BOLT)
                .orElse(false);
    }

    public boolean isEnabled() {
        return TurretState.getState(this) != TurretState.DISABLED;
    }

    public boolean isSniper() {
        if (!hasGun()) return false;
        return TimelessAPI.getCommonGunIndex(getGunItem().getGunId(getGunStack()))
                .map(index -> ServerConfig.SNIPER_GUN_TYPES.get().contains(index.getType()))
                .orElse(false);
    }

    public double getRange() {
        return isSniper() ? ServerConfig.SNIPER_TURRET_RANGE.get() : ServerConfig.TURRET_RANGE.get();
    }

    public void tryShoot() {
        if (!isEnabled()) return;
        if (isConservingAmmo() && tickCount - lastShotTick < CONSERVATIVE_SHOT_INTERVAL) return;
        gunOperator.aim(true);
        ShootResult result = shoot();
        switch (result) {
            case SUCCESS -> {
                lastShotTick = tickCount;
                if (ServerConfig.TURRET_RECOIL.get()) entityData.set(RECOIL, RECOIL_TICKS);
                if (ServerConfig.REQUIRE_ENERGY.get()) energy.consume(ServerConfig.ENERGY_PER_SHOT.get());
            }
            case NEED_BOLT -> gunOperator.bolt();
            case NO_AMMO -> {
                collectAmmo();
                gunOperator.reload();
            }
            case NOT_DRAW -> gunOperator.draw(this::getGunStack);
        }
    }

    private ShootResult shoot() {
        float inaccuracy = getInaccuracy();
        if (inaccuracy <= 0.0F) {
            return gunOperator.shoot(() -> getViewXRot(1), () -> getViewYRot(1));
        }
        float pitchOffset = (float) random.nextGaussian() * inaccuracy;
        float yawOffset = (float) random.nextGaussian() * inaccuracy;
        return gunOperator.shoot(() -> getViewXRot(1) + pitchOffset, () -> getViewYRot(1) + yawOffset);
    }

    private float getInaccuracy() {
        return switch (ServerConfig.INACCURACY_MODE.get()) {
            case RANDOM -> ServerConfig.RANDOM_INACCURACY.get().floatValue();
            case DISTANCE -> {
                LivingEntity target = BrainUtils.getTargetOfEntity(this);
                if (target == null) yield 0.0F;
                double distance = Math.sqrt(distanceToSqr(target));
                yield (float) (ServerConfig.DISTANCE_INACCURACY.get() * Math.min(1.0D, distance / getRange()));
            }
        };
    }

    public boolean hasAmmo() {
        if (!gunOperator.consumesAmmoOrNot()) return true;
        if (gunHasAmmo()) return true;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (isRightAmmo(getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    public boolean isRightAmmo(ItemStack stack) {
        if (stack.getItem() instanceof IAmmoBox ammoBox) {
            if (ammoBox.isAllTypeCreative(stack)) {
                return true;
            }
            return hasGun() && ammoBox.isAmmoBoxOfGun(getGunStack(), stack);
        }
        return hasGun() && stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(getGunStack(), stack);
    }

    public boolean hasCreativeAmmo() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (isCreativeAmmo(getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    public boolean isCreativeAmmo(ItemStack stack) {
        return stack.getItem() instanceof IAmmoBox ammoBox && (ammoBox.isCreative(stack) || ammoBox.isAllTypeCreative(stack));
    }

    @Nullable
    private BlockEntity getSupplyBlockEntity() {
        BlockEntity blockEntity = level().getBlockEntity(blockPosition());
        return blockEntity == null ? level().getBlockEntity(blockPosition().below()) : blockEntity;
    }

    private <T> @Nullable T getSupplyCapability(BlockCapability<T, Direction> cap) {
        var entity = getSupplyBlockEntity();
        if (entity == null) {
            return null;
        }
        return cap.getCapability(level(), entity.getBlockPos(), entity.getBlockState(), entity, Direction.UP);
    }

    public void collectAmmo() {
        if (shouldCollectAmmo()) {
            var handler = getSupplyCapability(Capabilities.ItemHandler.BLOCK);
            if (handler != null) {
                for (int invSlot = 0; invSlot < getSlots(); invSlot++) {
                    for (int handlerSlot = 0; handlerSlot < handler.getSlots(); handlerSlot++) {
                        ItemStack handlerStack = handler.getStackInSlot(handlerSlot);
                        if (isRightAmmo(handlerStack) && getStackInSlot(invSlot).getCount() < getStackInSlot(invSlot).getMaxStackSize()) {
                            ItemStack remainder = insertItem(invSlot, handler.extractItem(handlerSlot, handlerStack.getCount(), false), false);
                            if (!remainder.isEmpty()) {
                                handler.insertItem(handlerSlot, remainder, false);
                            }
                        }
                        if (hasCreativeAmmo()) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean shouldCollectAmmo() {
        return gunOperator.consumesAmmoOrNot() && isEnabled() && !hasCreativeAmmo();
    }

    public boolean hasEnoughEnergy() {
        if (!ServerConfig.REQUIRE_ENERGY.get()) return true;
        return energy.getEnergyStored() >= Math.max(1, ServerConfig.ENERGY_PER_SHOT.get());
    }

    private void tickEnergy() {
        if (!ServerConfig.REQUIRE_ENERGY.get()) return;
        if (ServerConfig.ENERGY_IDLE_DRAIN.get() > 0 && !enableType.shouldDisable(level(), blockPosition())) {
            energy.consume(ServerConfig.ENERGY_IDLE_DRAIN.get());
        }
        collectEnergy();
    }

    private void collectEnergy() {
        int space = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (space <= 0) return;
        var source = getSupplyCapability(Capabilities.EnergyStorage.BLOCK);
        if (source != null) {
            int available = source.extractEnergy(space, true);
            int accepted = energy.receiveEnergy(available, true);
            if (accepted > 0) energy.receiveEnergy(source.extractEnergy(accepted, false), false);
        }
    }

    private void tickPassiveHealing() {
        if (!ServerConfig.PASSIVE_HEALING.get()) return;
        if (getHealth() >= getMaxHealth()) return;
        if (tickCount % ServerConfig.PASSIVE_HEAL_INTERVAL.get() != 0) return;
        heal(ServerConfig.PASSIVE_HEAL_AMOUNT.get().floatValue());
    }

    @Override
    public void tick() {
        super.tick();
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
        if (!level().isClientSide()) {
            dropOverflow();
            int recoil = entityData.get(RECOIL);
            if (recoil > 0) entityData.set(RECOIL, recoil - 1);
            tickEnergy();
            tickPassiveHealing();
            if (isEnabled()) {
                if (hasGun()) {
                    ModernKineticGunItem gun = getGunItem();
                    if (!gunDrawn) {
                        gunOperator.draw(this::getGunStack);
                        gunDrawn = true;
                    }
                    ItemStack gunItem = getGunStack();
                    ResourceLocation gunId = gun.getGunId(gunItem);
                    IGun iGun = IGun.getIGunOrNull(gunItem);
                    if (iGun != null) {
                        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
                        if (gunIndexOptional.isPresent()) {
                            CommonGunIndex gunIndex = gunIndexOptional.get();
                            GunData gunData = gunIndex.getGunData();
                            AttachmentCacheProperty property = new AttachmentCacheProperty();
                            property.eval(getMainHandItem(), gunData);
                        }
                    }

                    if (isEnabled()) {
                        if (gunOperator.getSynReloadState().getStateType().isReloading()) {
                            TurretState.RELOADING.setState(this);
                        } else {
                            if (hasAmmo()) {
                                TurretState.ACTIVE.setState(this);
                            } else {
                                TurretState.NO_AMMO.setState(this);
                                collectAmmo();
                            }
                        }
                    }
                } else {
                    TurretState.NO_GUN.setState(this);
                }
                if (shouldDisable()) {
                    TurretState.DISABLED.setState(this);
                }
            } else if (!shouldDisable()) {
                TurretState.NO_GUN.setState(this);
            }
        }
        if (!level().isClientSide() && hasGun() && !gunHasAmmo() && !gunOperator.getSynReloadState().getStateType().isReloading()) {
            gunOperator.reload();
        }
    }

    public boolean isConservingAmmo() {
        if (mode == TurretMode.CONSERVATIVE) return true;
        if (mode != TurretMode.ADAPTIVE) return false;
        LivingEntity target = BrainUtils.getTargetOfEntity(this);
        if (target == null) return false;
        return distanceToSqr(target) > ServerConfig.ADAPTIVE_RANGE.get() * ServerConfig.ADAPTIVE_RANGE.get();
    }

    private float getRecoilProgress(float partialTick) {
        int recoil = entityData.get(RECOIL);
        if (recoil <= 0) return 0.0F;
        return Mth.clamp((recoil - partialTick) / RECOIL_TICKS, 0.0F, 1.0F);
    }

    public float getRecoilDegrees(float partialTick) {
        if (ClientConfig.RECOIL_TYPE.get() != RecoilType.BOUNCE) return 0.0F;
        return RECOIL_DEGREES * getRecoilProgress(partialTick);
    }

    public float getRecoilPush(float partialTick) {
        if (ClientConfig.RECOIL_TYPE.get() != RecoilType.PUSH) return 0.0F;
        return RECOIL_PUSH * getRecoilProgress(partialTick);
    }

    private boolean shouldDisable() {
        return enableType.shouldDisable(level(), blockPosition()) || !hasEnoughEnergy();
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        boolean manages = canInteract(player);
        if (!manages && player.isCrouching()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (manages) {
            InteractionResult result = manageInteract(player, hand);
            if (result != null) return result;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TurretLayout layout = TurretLayout.fromConfig();
            serverPlayer.openMenu(this, buf -> {
                buf.writeVarInt(getId());
                buf.writeByte(layout.rows);
                buf.writeByte(layout.columns);
                buf.writeUtf(getOwnerName());
                Set<UUID> allies = getAllies();
                buf.writeVarInt(allies.size());
                allies.forEach(buf::writeUUID);
                buf.writeBoolean(manages);
            });
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult manageInteract(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (!player.isCrouching() && ServerConfig.getRepairItems().matches(heldStack)) {
            if (getHealth() < getMaxHealth()) {
                heal(ServerConfig.REPAIR_AMOUNT.get().floatValue());
                if (!player.getAbilities().instabuild) heldStack.shrink(1);
                playRepairSound();
                spawnRepairParticles();
            }
            lastRepairTick = tickCount;
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching()) {
            ItemStack gunStack = getGunStack();
            setGunStack(ItemStack.EMPTY);
            if (!gunStack.isEmpty() && !player.getInventory().add(gunStack)) {
                spawnAtLocation(gunStack);
            }
            for (int slot = 0; slot < getSlots(); slot++) {
                ItemStack slotStack = extractItem(slot, getStackInSlot(slot).getCount(), false);
                if (!slotStack.isEmpty() && !player.getInventory().add(slotStack)) {
                    spawnAtLocation(slotStack);
                }
            }
            if (!player.getAbilities().instabuild) {
                player.getInventory().add(new ItemStack(ItemRegistry.TURRET.get()));
            }
            playTurretSound(SoundRegistry.TURRET_PICKUP.get());
            discard();
            return InteractionResult.SUCCESS;
        }
        if (tickCount - lastRepairTick < REPAIR_INTERACT_GRACE) {
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    public void playTurretSound(SoundEvent sound) {
        if (!ServerConfig.ENABLE_SOUNDS.get()) return;
        playSound(sound, 1.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    private void playRepairSound() {
        if (!ServerConfig.ENABLE_SOUNDS.get()) return;
        List<? extends String> sounds = ServerConfig.REPAIR_SOUNDS.get();
        if (sounds.isEmpty()) return;
        ResourceLocation soundId = ResourceLocation.tryParse(sounds.get(random.nextInt(sounds.size())));
        if (soundId == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound != null) playSound(sound, 1.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    private void spawnRepairParticles() {
        if (!ServerConfig.REPAIR_PARTICLES.get()) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.6D, getZ(), 12, getBbWidth() * 0.5D, getBbHeight() * 0.4D, getBbWidth() * 0.5D, 0.0D);
    }

    private void resizeInventory() {
        int desired = Math.max(1, ServerConfig.TURRET_SLOT_ROWS.get() * ServerConfig.TURRET_SLOT_LENGTH.get());
        if (inventory.getSlots() == desired) return;
        List<ItemStack> kept = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) kept.add(inventory.getStackInSlot(slot));
        inventory.setSize(desired);
        for (int slot = 0; slot < kept.size(); slot++) {
            ItemStack stack = kept.get(slot);
            if (stack.isEmpty()) continue;
            if (slot < desired) {
                inventory.setStackInSlot(slot, stack);
            } else {
                overflow.add(stack);
            }
        }
    }

    private void dropOverflow() {
        if (overflow.isEmpty()) return;
        for (ItemStack stack : overflow) spawnAtLocation(stack);
        overflow.clear();
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new TurretMenu(containerId, playerInventory, this, TurretLayout.fromConfig(), getOwnerName(), getAllies(), canInteract(player));
    }

    public boolean canInteract(Player player) {
        return isOwnedBy(player) || (ServerConfig.ALLIES_HAVE_PERMS.get() && isAlliedWithOwner(player));
    }

    public boolean isOwnedBy(Player player) {
        return owner == null || player.getUUID().equals(owner) || player.isCreative() || (ServerConfig.OP_BYPASS.get() && player.hasPermissions(2));
    }

    public boolean canAcceptAmmo(ItemStack stack) {
        if (isRightAmmo(stack)) return true;
        return !hasGun() && (stack.getItem() instanceof IAmmo || stack.getItem() instanceof IAmmoBox);
    }

    public boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof ModernKineticGunItem;
    }

    public void onInventoryChanged() {
        gunDrawn = false;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcastAndSend(this, new ClientboundSetEquipmentPacket(getId(), List.of(Pair.of(EquipmentSlot.MAINHAND, getMainHandItem()))));
        }
    }

    public TurretEnableType getEnableType() {
        return enableType;
    }

    public void setEnableType(TurretEnableType type) {
        enableType = type;
    }

    public TurretMode getMode() {
        return mode;
    }

    public void setMode(TurretMode turretMode) {
        mode = turretMode;
    }

    public TurretEnergyStorage getEnergyStorage() {
        return energy;
    }

    public int getEnergyStored() {
        return getEnergyStorage().getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return getEnergyStorage().getMaxEnergyStored();
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source, boolean recentlyHit) {
        for (int i = 0; i < getSlots(); i++) {
            if (!getStackInSlot(i).isEmpty()) spawnAtLocation(extractItem(i, getStackInSlot(i).getCount(), false));
        }
        if (hasGun()) spawnAtLocation(getGunStack());
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (source.getEntity() instanceof TurretEntity) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity entity) {
            if (entity instanceof Player player) markRetaliation(player);
            if (isValidTarget(entity)) {
                alertTo(entity);
                List<TurretEntity> entities = level().getEntitiesOfClass(TurretEntity.class, AABB.ofSize(position(), 64, 16, 64));
                List<TurretEntity> filter1 = entities.stream().filter((e) -> e.hasLineOfSight(entity) || BehaviorUtils.entityIsVisible(e.getBrain(), entity)).toList();
                for (TurretEntity turret : filter1) {
                    if (entity instanceof Player player && Objects.equals(turret.owner, owner))
                        turret.markRetaliation(player);
                    if (turret.isValidTarget(entity)) turret.alertTo(entity);
                }
            }
        }

        return super.hurt(source, damage);
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        if (source.getEntity() != null && source.getEntity().getUUID().equals(owner)) {
            return false;
        }
        if (!ServerConfig.TURRETS_TAKE_DAMAGE.get()) {
            return !source.isCreativePlayer() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public void setTarget(@Nullable LivingEntity entity) {
        if (!isEnabled()) return;

        if (getTarget() == null && entity != null) {
            ALERT_INTERVAL.sample(random);
        }

        if (entity instanceof Player) {
            setLastHurtByPlayer((Player) entity);
        }

        super.setTarget(entity);
    }

    protected Brain.@NotNull Provider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    protected void customServerAiStep() {
        if (retaliateTicks < 0 && ServerConfig.RETALIATE_TARGETING.get() != RetaliateTargeting.CLEAR_ON_DEATH)
            retaliateTicks = retaliationTicks();
        if (retaliateTicks > 0 && --retaliateTicks == 0) retaliateTarget = null;
        tickBrain(this);
        retargetImmediately();
        spreadTargets();
    }

    private void retargetImmediately() {
        LivingEntity current = BrainUtils.getTargetOfEntity(this);
        if (current != null && current.isAlive()) {
            hadTarget = true;
            return;
        }
        if (!hadTarget) return;
        hadTarget = false;
        if (!isEnabled()) return;

        double range = getRange();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range), entity -> !(entity instanceof TurretEntity) && shouldTarget(entity) && hasLineOfSight(entity))) {
            double distance = distanceToSqr(candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        if (best != null) alertTo(best);
    }

    private void spreadTargets() {
        if (!ServerConfig.BETTER_TARGETING.get()) return;
        if (tickCount % TARGET_SPREAD_INTERVAL != 0) return;

        LivingEntity current = BrainUtils.getTargetOfEntity(this);
        if (current == null) return;

        double range = getRange();
        double currentDistance = distanceToSqr(current);
        double reachSquared = Math.clamp(currentDistance * TARGET_SPREAD_REACH, TARGET_SPREAD_MIN_RADIUS * TARGET_SPREAD_MIN_RADIUS, range * range);
        double reach = Math.sqrt(reachSquared);

        List<TurretEntity> allies = level().getEntitiesOfClass(TurretEntity.class, getBoundingBox().inflate(range), turret -> turret != this);
        int currentClaims = countClaims(allies, current);

        LivingEntity best = current;
        int bestClaims = currentClaims;
        double bestDistance = currentDistance;
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(reach), entity -> entity != current && !(entity instanceof TurretEntity) && distanceToSqr(entity) <= reachSquared && shouldTarget(entity) && hasLineOfSight(entity))) {
            int claims = countClaims(allies, candidate);
            double distance = distanceToSqr(candidate);
            if (claims < bestClaims || (claims == bestClaims && distance < bestDistance)) {
                best = candidate;
                bestClaims = claims;
                bestDistance = distance;
            }
        }

        if (best != current && (bestClaims < currentClaims || bestDistance < currentDistance * TARGET_SWITCH_MARGIN)) {
            alertTo(best);
        }
    }

    private int countClaims(List<TurretEntity> allies, LivingEntity target) {
        int claims = 0;
        for (TurretEntity ally : allies) {
            if (BrainUtils.getTargetOfEntity(ally) == target) claims++;
        }
        return claims;
    }

    @Override
    public BrainActivityGroup<? extends TurretEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(new TargetOrRetaliate<>().isAllyIf((e, l) -> l instanceof TurretEntity).attackablePredicate(l -> l != null && isValidTarget(l) && hasLineOfSight(l)).alertAlliesWhen((m, e) -> e != null && m.hasLineOfSight(e)).runFor((e) -> 999), (new LookAtTarget<>()).runFor((entity) -> RandomSource.create().nextInt(40, 300)));
    }

    public BrainActivityGroup<? extends TurretEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(new FirstApplicableBehaviour<>(new TargetOrRetaliate<>().attackablePredicate(l -> l != null && isValidTarget(l) && hasLineOfSight(l)), new SetPlayerLookTarget<>(), new SetRandomLookTarget<>()), new Idle<>().runFor((entity) -> RandomSource.create().nextInt(30, 60)));
    }

    public BrainActivityGroup<? extends TurretEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(new InvalidateAttackTarget<TurretEntity>().invalidateIf((entity, target) -> !target.isAlive() || (target instanceof Player player && player.getAbilities().invulnerable) || !entity.hasLineOfSight(target) || !entity.isValidTarget(target) || entity.distanceToSqr(target) > entity.getRange() * entity.getRange()).ignoreFailedPathfinding(), new SetRetaliateTarget<>(), new TaczShootAttack<>(ServerConfig.TURRET_RANGE.get()).startCondition((x$0) -> getMainHandItem().is(ModItems.MODERN_KINETIC_GUN.get()) && gunOperator.getSynShootCoolDown() == 0));
    }

    @Override
    public List<? extends ExtendedSensor<? extends TurretEntity>> getSensors() {
        int range = Math.max(ServerConfig.TURRET_RANGE.get(), ServerConfig.SNIPER_TURRET_RANGE.get());
        return ObjectArrayList.of(new NearbyPlayersSensor<TurretEntity>().setRadius(range).setPredicate((p, e) -> e.lastHurtByPlayer != null && p.getUUID().equals(e.lastHurtByPlayer.getUUID())), new HurtBySensor<>(), new NearbyLivingEntitySensor<TurretEntity>().setRadius(range).setPredicate((target, entity) -> shouldTarget(target)));
    }

    @Nullable
    public Player getOwnerPlayer() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    public void alertTo(LivingEntity target) {
        if (!isEnabled()) return;
        setTarget(target);
        getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
    }

    public boolean isAlliedWithOwner(LivingEntity target) {
        if (owner != null && level().getServer() != null && TurretAllies.get(level().getServer()).isAlly(owner, target.getUUID()))
            return true;
        if (!ServerConfig.RESPECT_TEAMS.get()) return false;
        if (target.getTeam() == null) return false;
        if (isAlliedTo(target)) return true;
        Player ownerPlayer = getOwnerPlayer();
        return ownerPlayer != null && ownerPlayer.isAlliedTo(target);
    }

    private boolean canTargetPlayers() {
        return ServerConfig.DAMAGE_PLAYERS.get() && playerTargeting != PlayerTargeting.NEVER;
    }

    public void markRetaliation(Player player) {
        retaliateTarget = player.getUUID();
        retaliateTicks = ServerConfig.RETALIATE_TARGETING.get() == RetaliateTargeting.CLEAR_ON_DEATH ? RETALIATE_UNTIL_DEATH : retaliationTicks();
    }

    private static int retaliationTicks() {
        return Math.max(1, ServerConfig.RETALIATION_TIMER.get() * 20);
    }

    public void forgetRetaliation(UUID target) {
        if (target.equals(retaliateTarget)) {
            retaliateTarget = null;
            retaliateTicks = 0;
        }
    }

    private boolean isRetaliating(LivingEntity target) {
        return retaliateTicks != 0 && target.getUUID().equals(retaliateTarget);
    }

    private boolean canEngagePlayer(Player player) {
        return playerTargeting == PlayerTargeting.ALL || isRetaliating(player);
    }

    public PlayerTargeting getPlayerTargeting() {
        return playerTargeting;
    }

    public void setPlayerTargeting(PlayerTargeting targeting) {
        playerTargeting = targeting;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Set<UUID> getAllies() {
        if (owner == null || level().getServer() == null) return Set.of();
        return TurretAllies.get(level().getServer()).getAllies(owner);
    }

    public boolean isValidTarget(LivingEntity target) {
        if (!isEnabled()) return false;
        if (target == this || !target.isAlive()) return false;
        if (target instanceof TurretEntity) return false;
        if (distanceToSqr(target) > getRange() * getRange()) return false;
        if (target.getUUID().equals(owner)) return false;
        if (isAlliedWithOwner(target)) return false;

        if (target.getType().is(TagRegistry.TURRET_IGNORED)) return false;
        if (ServerConfig.getBlacklist().matches(target.getType())) return false;

        if (target instanceof Player player)
            return canTargetPlayers() && !player.isCreative() && !player.isSpectator() && canEngagePlayer(player);
        return true;
    }

    public boolean isProtectedFromFire(LivingEntity victim) {
        if (!ServerConfig.DAMAGE_PLAYERS.get() && victim instanceof Player) return true;
        if (ServerConfig.OWNER_TAKES_NO_DAMAGE.get() && victim.getUUID().equals(owner)) return true;
        if (ServerConfig.ALLIES_CANNOT_BE_DAMAGED.get() && isAlliedWithOwner(victim)) return true;
        return !canDamage(victim);
    }

    public boolean canDamage(LivingEntity victim) {
        if (victim instanceof Player || victim == getTarget() || victim == getLastHurtByMob()) return true;
        TargetFilter filter = ServerConfig.getDamageable();
        return filter.isEmpty() ? isTargetableType(victim) : filter.matches(victim.getType());
    }

    private boolean shouldTarget(LivingEntity target) {
        if (!isValidTarget(target)) return false;
        if (target instanceof Player) return true;

        if (target == getTarget()) return true;
        return isTargetableType(target);
    }

    private boolean isTargetableType(LivingEntity target) {
        if (target.getType().is(TagRegistry.TURRET_IGNORED)) return false;
        if (ServerConfig.getBlacklist().matches(target.getType())) return false;

        if (ServerConfig.getWhitelist().matches(target.getType())) return true;
        if (target.getType().is(TagRegistry.TURRET_TARGETS)) return true;

        if (ServerConfig.TARGET_ALL_MOBS.get()) return true;

        if (target instanceof Monster) return true;
        return target.getType().getCategory() == MobCategory.MONSTER;
    }

    @Override
    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return hasGun() && isRightAmmo(stack);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {

    }

    @Override
    public boolean ignoreExplosion(@NotNull Explosion explosion) {
        return true;
    }
}
