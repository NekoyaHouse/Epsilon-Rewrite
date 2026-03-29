package com.github.epsilon.modules.impl.world;

import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.StringSetting;
import com.github.epsilon.utils.combat.DamageUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakePlayer extends Module {

    public static final FakePlayer INSTANCE = new FakePlayer();

    private FakePlayer() {
        super("FakePlayer", Category.WORLD);
    }

    private enum HealingMode {
        Vanilla,
        Fast,
        Off
    }

    private final StringSetting name = stringSetting("Name", "王宁");
    private final BoolSetting copyInventory = boolSetting("CopyInventory", true);
    private final BoolSetting copyHealth = boolSetting("CopyHealth", true);
    private final BoolSetting copyEffects = boolSetting("CopyEffects", true);
    private final BoolSetting buffs = boolSetting("Buffs", true);
    private final BoolSetting autoTotem = boolSetting("AutoTotem", true);
    private final BoolSetting record = boolSetting("Record", false);
    private final BoolSetting playback = boolSetting("Playback", false);
    private final BoolSetting loopPlayback = boolSetting("LoopPlayback", true, playback::getValue);
    private final BoolSetting explosionDamage = boolSetting("ExplosionDamage", true);
    private final BoolSetting disableOnDeath = boolSetting("DisableOnDeath", true);
    private final EnumSetting<HealingMode> healingMode = enumSetting("HealingMode", HealingMode.Vanilla);

    private static final int FAKE_PLAYER_ID = -917813;
    private static final int MAX_RECORDED_STATES = 20 * 60 * 5;

    private RemotePlayer fakePlayer;
    private int playbackIndex;
    private int deathTicks;
    private boolean wasRecording;
    private boolean wasPlayingBack;

    private final List<PlayerState> states = new ArrayList<>();

    public RemotePlayer getFakePlayer() {
        return fakePlayer;
    }

    @Override
    protected void onEnable() {
        playbackIndex = 0;
        deathTicks = 0;
        wasRecording = false;
        wasPlayingBack = false;

        if (nullCheck()) {
            setEnabled(false);
            return;
        }

        spawnFakePlayer();
    }

    @Override
    protected void onDisable() {
        removeFakePlayer();
        states.clear();
        playbackIndex = 0;
        deathTicks = 0;
        wasRecording = false;
        wasPlayingBack = false;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) {
            setEnabled(false);
            return;
        }

        if (fakePlayer == null || fakePlayer.level() != mc.level) {
            spawnFakePlayer();
        }

        if (fakePlayer == null) {
            return;
        }

        if (record.getValue() && !wasRecording) {
            states.clear();
            playbackIndex = 0;
            if (playback.getValue()) playback.setValue(false);
        }

        if (playback.getValue() && !wasPlayingBack) {
            playbackIndex = 0;
            if (record.getValue()) record.setValue(false);
        }

        if (record.getValue()) {
            states.add(PlayerState.capture(mc.player));
            if (states.size() > MAX_RECORDED_STATES) states.remove(0);
        } else if (playback.getValue() && !states.isEmpty()) {
            if (playbackIndex >= states.size()) {
                if (!loopPlayback.getValue()) {
                    playback.setValue(false);
                    playbackIndex = 0;
                    return;
                }
                playbackIndex = 0;
            }
            PlayerState state = states.get(playbackIndex++);
            applyState(state);
        } else {
            playbackIndex = 0;
        }

        if (autoTotem.getValue() && !fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        tickHealing();

        wasRecording = record.getValue();
        wasPlayingBack = playback.getValue();

        if (!fakePlayer.isAlive()) {
            deathTicks++;
            if (disableOnDeath.getValue() && deathTicks > 10) {
                setEnabled(false);
            }
        } else {
            deathTicks = 0;
        }
    }

    @SubscribeEvent
    private void onAttackEntity(AttackEntityEvent event) {
        if (fakePlayer == null || event.getTarget() != fakePlayer || event.getEntity() != mc.player) return;

        event.setCanceled(true);

        if (!fakePlayer.isAlive()) return;

        DamageSource damageSource = mc.player.damageSources().playerAttack(mc.player);
        float damage = calculateAttackDamage(mc.player, fakePlayer, damageSource);

        fakePlayer.hurtTime = fakePlayer.hurtDuration = 10;
        fakePlayer.invulnerableTime = 0;
        fakePlayer.animateHurt(mc.player.getYRot());
        fakePlayer.setDeltaMovement(fakePlayer.getDeltaMovement().add(mc.player.getLookAngle().scale(0.12)));

        mc.level.playLocalSound(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_HURT, fakePlayer.getSoundSource(), 1.0f, 1.0f, false);

        if (isCriticalAttack(mc.player, fakePlayer)) {
            mc.level.playLocalSound(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, fakePlayer.getSoundSource(), 1.0f, 1.0f, false);
        } else {
            boolean fullStrength = mc.player.getAttackStrengthScale(0.5f) > 0.9f;
            mc.level.playLocalSound(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), fullStrength ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK, fakePlayer.getSoundSource(), 1.0f, 1.0f, false);
        }

        applyDamage(damage, damageSource);
    }

    @SubscribeEvent
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck() || fakePlayer == null || !explosionDamage.getValue() || !fakePlayer.isAlive()) return;

        if (event.getPacket() instanceof ClientboundExplodePacket packet) {
            float damage = DamageUtils.explosionDamage(fakePlayer, packet.center(), packet.radius(), DamageUtils.ArmorEnchantmentMode.None);
            if (damage > 0.0f) {
                applyDamage(damage, mc.player.damageSources().explosion(mc.player, mc.player));
            }
        }
    }

    @SubscribeEvent
    private void onPacketSend(PacketEvent.Send event) {
        if (nullCheck() || fakePlayer == null || !fakePlayer.isAlive()) {
            return;
        }

        if (event.getPacket() instanceof ServerboundAttackPacket(int entityId) && entityId == fakePlayer.getId()) {
            event.setCanceled(true);
            return;
        }

        if (event.getPacket() instanceof ServerboundInteractPacket packet && packet.entityId() == fakePlayer.getId()) {
            event.setCanceled(true);
        }
    }

    private void spawnFakePlayer() {
        removeFakePlayer();

        String fakeName = name.getValue().isBlank() ? "滚木" : name.getValue();
        UUID uuid = UUID.nameUUIDFromBytes(("epsilon:fake_player:" + fakeName).getBytes(StandardCharsets.UTF_8));
        RemotePlayer player = new RemotePlayer(mc.level, new GameProfile(uuid, fakeName));

        player.setId(FAKE_PLAYER_ID);
        player.setUUID(uuid);
        player.snapTo(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot());
        player.setOldPosAndRot();
        player.setYHeadRot(mc.player.getYHeadRot());
        player.setYBodyRot(mc.player.yBodyRot);
        player.setOnGround(mc.player.onGround());
        player.setShiftKeyDown(mc.player.isShiftKeyDown());
        player.setSprinting(mc.player.isSprinting());
        player.setInvisible(mc.player.isInvisible());

        if (copyHealth.getValue()) {
            player.setHealth(mc.player.getHealth());
            player.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        } else {
            player.setHealth(player.getMaxHealth());
        }

        if (copyInventory.getValue()) {
            copyInventory(player, mc.player);
        } else {
            player.setItemSlot(EquipmentSlot.MAINHAND, mc.player.getMainHandItem().copy());
            player.setItemSlot(EquipmentSlot.OFFHAND, mc.player.getOffhandItem().copy());
            player.setItemSlot(EquipmentSlot.HEAD, mc.player.getItemBySlot(EquipmentSlot.HEAD).copy());
            player.setItemSlot(EquipmentSlot.CHEST, mc.player.getItemBySlot(EquipmentSlot.CHEST).copy());
            player.setItemSlot(EquipmentSlot.LEGS, mc.player.getItemBySlot(EquipmentSlot.LEGS).copy());
            player.setItemSlot(EquipmentSlot.FEET, mc.player.getItemBySlot(EquipmentSlot.FEET).copy());
        }

        if (copyEffects.getValue()) {
            copyEffects(player, mc.player);
        }

        if (buffs.getValue()) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 999999, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 999999, 3, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 999999, 0, false, false));
        }

        if (autoTotem.getValue()) {
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        mc.level.addEntity(player);
        fakePlayer = player;
    }

    private void removeFakePlayer() {
        if (fakePlayer == null) {
            return;
        }

        if (mc.level != null) {
            mc.level.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }

        fakePlayer = null;
    }

    private void copyInventory(RemotePlayer target, Player source) {
        for (int slot = 0; slot < source.getInventory().getContainerSize(); slot++) {
            target.getInventory().setItem(slot, source.getInventory().getItem(slot).copy());
        }
        target.getInventory().setSelectedSlot(source.getInventory().getSelectedSlot());
    }

    private void copyEffects(RemotePlayer target, Player source) {
        target.removeAllEffects();
        for (MobEffectInstance effect : source.getActiveEffects()) {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    private void applyState(PlayerState state) {
        fakePlayer.snapTo(state.x, state.y, state.z, state.yaw, state.pitch);
        fakePlayer.setOldPosAndRot();
        fakePlayer.setYHeadRot(state.headYaw);
        fakePlayer.setYBodyRot(state.bodyYaw);
        fakePlayer.setOnGround(state.onGround);
        fakePlayer.setShiftKeyDown(state.sneaking);
        fakePlayer.setSprinting(state.sprinting);
        fakePlayer.setDeltaMovement(state.velocity);
    }

    /**
     * 自动回血
     */
    private void tickHealing() {
        if (fakePlayer == null || !fakePlayer.isAlive()) return;
        if (healingMode.is(HealingMode.Off)) return;

        MobEffectInstance regeneration = fakePlayer.getEffect(MobEffects.REGENERATION);
        if (regeneration != null && fakePlayer.getHealth() < fakePlayer.getMaxHealth()) {
            int interval = getHealingInterval(regeneration.getAmplifier());
            if (fakePlayer.tickCount % interval == 0) {
                fakePlayer.setHealth(Math.min(fakePlayer.getMaxHealth(), fakePlayer.getHealth() + 1.0f));
            }
        }

        MobEffectInstance absorption = fakePlayer.getEffect(MobEffects.ABSORPTION);
        if (absorption != null) {
            float maxAbsorption = 4.0f * (absorption.getAmplifier() + 1);
            if (fakePlayer.getAbsorptionAmount() < maxAbsorption) {
                fakePlayer.setAbsorptionAmount(maxAbsorption);
            }
        }
    }

    private int getHealingInterval(int amplifier) {
        int vanillaInterval = Math.max(1, 50 >> amplifier);
        return switch (healingMode.getValue()) {
            case Vanilla -> vanillaInterval;
            case Fast -> Math.max(1, vanillaInterval / 2);
            case Off -> Integer.MAX_VALUE;
        };
    }

    /**
     * 假人伤害计算
     */
    private void applyDamage(float damage, DamageSource source) {
        if (fakePlayer == null) {
            return;
        }

        float totalProtection = fakePlayer.getHealth() + fakePlayer.getAbsorptionAmount();
        if (damage >= totalProtection && canUseTotem()) {
            popTotem();
            return;
        }

        float remainingDamage = damage;
        float absorption = fakePlayer.getAbsorptionAmount();
        if (absorption > 0.0f) {
            float absorbed = Math.min(absorption, remainingDamage);
            fakePlayer.setAbsorptionAmount(absorption - absorbed);
            remainingDamage -= absorbed;
        }

        if (remainingDamage > 0.0f) {
            fakePlayer.setHealth(fakePlayer.getHealth() - remainingDamage);
        }

        if (fakePlayer.getHealth() <= 0.0f) {
            if (canUseTotem()) {
                popTotem();
            } else {
                fakePlayer.setHealth(0.0f);
                fakePlayer.handleDamageEvent(source);
            }
        }
    }

    private boolean canUseTotem() {
        return autoTotem.getValue() && fakePlayer != null && fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
    }

    private void popTotem() {
        if (fakePlayer == null) return;

        fakePlayer.setHealth(1.0f);
        fakePlayer.setAbsorptionAmount(8.0f);
        fakePlayer.invulnerableTime = 20;
        fakePlayer.hurtTime = fakePlayer.hurtDuration = 10;
        fakePlayer.removeAllEffects();
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        fakePlayer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, autoTotem.getValue() ? new ItemStack(Items.TOTEM_OF_UNDYING) : ItemStack.EMPTY);
        mc.particleEngine.createTrackingEmitter(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.level.playLocalSound(fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.TOTEM_USE, fakePlayer.getSoundSource(), 1.0f, 1.0f, false);
    }

    private float calculateAttackDamage(Player attacker, Entity target, DamageSource damageSource) {
        float attackStrength = attacker.getAttackStrengthScale(0.5f);
        float baseDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        baseDamage *= 0.2f + attackStrength * attackStrength * 0.8f;
        baseDamage += attacker.getMainHandItem().getItem().getAttackDamageBonus(target, baseDamage, damageSource);

        if (isCriticalAttack(attacker, target) && attackStrength > 0.9f) {
            baseDamage *= 1.5f;
        }

        return Math.max(baseDamage, attackStrength > 0.9f ? 1.0f : 0.5f);
    }

    private boolean isCriticalAttack(Player attacker, Entity target) {
        return attacker.fallDistance > 0.0f && !attacker.onGround() && !attacker.onClimbable() && !attacker.isInWater() && !attacker.isPassenger() && !attacker.isSprinting() && target instanceof Player;
    }

    private record PlayerState(double x, double y, double z, float yaw, float pitch, float headYaw, float bodyYaw,
                               boolean onGround, boolean sneaking, boolean sprinting, Vec3 velocity) {
        private static PlayerState capture(Player player) {
            return new PlayerState(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.getYHeadRot(), player.yBodyRot, player.onGround(), player.isShiftKeyDown(), player.isSprinting(), player.getDeltaMovement());
        }
    }
}
