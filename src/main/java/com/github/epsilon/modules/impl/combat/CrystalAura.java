package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.managers.TargetManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.combat.DamageUtils;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RaytraceUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CrystalAura extends Module {

    public static final CrystalAura INSTANCE = new CrystalAura();

    private CrystalAura() {
        super("CrystalAura", Category.COMBAT);
    }

    // General
    private final DoubleSetting targetRange = doubleSetting("Target Range", 6.0, 0.0, 12.0, 0.5);
    private final EnumSetting<AimOptimizeMode> aimOptimizeMode = enumSetting("Aim Optimize Mode", AimOptimizeMode.MaxDamage);
    private final DoubleSetting aimDamageSacrifice = doubleSetting("Aim Damage Sacrifice", 1.0, 0.0, 10.0, 0.25,
            () -> aimOptimizeMode.is(AimOptimizeMode.LowRotation));
    private final BoolSetting eatingPause = boolSetting("Eating Pause", false);

    // Calculation
    private final BoolSetting noSuicide = boolSetting("No Suicide", true);
    private final DoubleSetting lethalMaxSelfDamage = doubleSetting("Lethal Max Self Dmg", 8.0, 0.0, 36.0, 0.25);
    private final BoolSetting motionPrediction = boolSetting("Motion Prediction", false);
    private final IntSetting predictTick = intSetting("Predict Tick", 6, 0, 10, 1, motionPrediction::getValue);

    // Force Place
    private final DoubleSetting forcePlaceHealth = doubleSetting("Force Place Health", 8.0, 0.0, 36.0, 0.5);
    private final IntSetting forcePlaceArmorRate = intSetting("Force Place Armor Rate", 3, 0, 25, 1);
    private final DoubleSetting forcePlaceMinDamage = doubleSetting("Force Place Min Dmg", 2.0, 0.0, 20.0, 0.25);
    private final DoubleSetting forcePlaceBalance = doubleSetting("Force Place Balance", -3.0, -10.0, 10.0, 0.25);

    // Place
    private final EnumSetting<SwingMode> placeSwing = enumSetting("Place Swing", SwingMode.None);
    private final DoubleSetting placeRotationSpeed = doubleSetting("Place Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final EnumSetting<SwapMode> placeSwapMode = enumSetting("Place Swap Mode", SwapMode.None);
    private final DoubleSetting placeMinDmg = doubleSetting("Place Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting placeMaxSelfDmg = doubleSetting("Place Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting placeBalance = doubleSetting("Place Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 10);
    private final DoubleSetting placeRange = doubleSetting("Place Range", 4.0, 1.0, 6.0, 0.1);

    // Break
    private final EnumSetting<SwingMode> breakSwing = enumSetting("Break Swing", SwingMode.None);
    private final DoubleSetting breakRotationSpeed = doubleSetting("Break Rotation Speed", 10.0, 1.0, 10.0, 0.5);
    private final BoolSetting antiWeak = boolSetting("Anti Weak", false);
    private final EnumSetting<SwapMode> antiWeakSwapMode = enumSetting("Anti Weak Swap Mode", SwapMode.Silent, antiWeak::getValue);
    private final DoubleSetting breakMinDmg = doubleSetting("Break Min Dmg", 6.0, 0.0, 20.0, 0.25);
    private final DoubleSetting breakMaxSelfDmg = doubleSetting("Break Max Self Dmg", 10.0, 0.0, 36.0, 0.25);
    private final DoubleSetting breakBalance = doubleSetting("Break Balance", -3.0, -10.0, 10.0, 0.25);
    private final IntSetting breakDelay = intSetting("Break Delay", 50, 0, 1000, 10);
    private final DoubleSetting breakRange = doubleSetting("Break Range", 4.0, 1.0, 6.0, 0.1);

    // Render
    private final ColorSetting filledColor = colorSetting("Filled Color", new Color(255, 150, 120, 100));
    private final ColorSetting outlineColor = colorSetting("Outline Color", new Color(255, 150, 120, 170));
    private final IntSetting movingLength = intSetting("Moving Length", 400, 0, 1000, 50);
    private final IntSetting fadeLength = intSetting("Fade Length", 200, 0, 1000, 50);

    private LivingEntity target;
    private Vec3 lastTargetPos;
    private final TimerUtils placeTimer = new TimerUtils();
    private final TimerUtils breakTimer = new TimerUtils();
    private final TimerUtils renderTimer = new TimerUtils();
    private final List<RenderRecord> renderRecords = new ArrayList<>();

    @Override
    protected void onEnable() {
        lastTargetPos = null;
        placeTimer.reset();
        breakTimer.reset();
        renderTimer.reset();
        renderRecords.clear();
    }

    @Override
    protected void onDisable() {
        target = null;
        lastTargetPos = null;
        renderRecords.clear();
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (eatingPause.getValue() && mc.player.isUsingItem()) return;

        target = TargetManager.INSTANCE.acquirePrimary(
                TargetManager.TargetRequest.of(
                        targetRange.getValue(),
                        360.0f,
                        true,
                        false,
                        false,
                        false,
                        true,
                        1
                )
        );

        if (target == null || !target.isAlive()) {
            lastTargetPos = null;
            return;
        }

        Vec3 predictedPos = getPredictedTargetPos(target);

        tryBreakCrystal();
        tryPlaceCrystal(predictedPos);
        lastTargetPos = target.position();
    }

    private void tryBreakCrystal() {
        if (!breakTimer.passedMillise(breakDelay.getValue())) return;

        List<BreakCandidate> candidates = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!crystal.isAlive()) continue;

            double distToPlayer = mc.player.distanceTo(crystal);
            if (distToPlayer > breakRange.getValue()) continue;

            Vec3 crystalPos = crystal.position();

            float targetDmg = DamageUtils.crystalDamage(target, crystalPos, DamageUtils.ArmorEnchantmentMode.None);
            float selfDmg = DamageUtils.selfCrystalDamage(crystalPos);
            if (exceedsSelfDamageLimit(selfDmg, breakMaxSelfDmg.getValue())) continue;
            if (targetDmg < breakMinDmg.getValue()) continue;
            float balance = targetDmg - selfDmg;
            if (balance < breakBalance.getValue()) continue;

            Vector2f targetRotation = RotationUtils.calculate(crystal);
            float rotationDelta = getRotationDelta(getPrevTickRotation(), targetRotation);
            candidates.add(new BreakCandidate(crystal, targetDmg, targetRotation, rotationDelta));
        }

        EndCrystal bestCrystal = selectBestBreakCandidate(candidates);
        if (bestCrystal != null) {
            doBreakCrystal(bestCrystal);
        }
    }

    private void doBreakCrystal(EndCrystal crystal) {
        boolean swapped = false;
        if (antiWeak.getValue() && mc.player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) {
            FindItemResult sword = InvUtils.findInHotbar(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.IRON_SWORD, Items.STONE_SWORD);
            if (sword.found()) {
                switch (antiWeakSwapMode.getValue()) {
                    case Swap -> InvUtils.swap(sword.slot(), false);
                    case Silent -> swapped = InvUtils.swap(sword.slot(), true);
                    default -> { }
                }
            }
        }

        final boolean silentSwapped = swapped;
        final int requestPriority = Priority.High.priority;
        final int crystalId = crystal.getId();
        final Vector2f rotation = RotationUtils.calculate(crystal);

        RotationManager.INSTANCE.applyRotation(rotation, breakRotationSpeed.getValue(), requestPriority, record -> {
            if (!isEnabled() || nullCheck()) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }
            if (record.selectedPriorityValue() != requestPriority) return;

            Entity current = mc.level.getEntity(crystalId);
            if (!(current instanceof EndCrystal currentCrystal) || !currentCrystal.isAlive()) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }

            if (!RaytraceUtils.facingEnemy(currentCrystal, breakRange.getValue(), record.currentRotation())) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }

            mc.gameMode.attack(mc.player, currentCrystal);
            doSwing(breakSwing.getValue());
            breakTimer.reset();
            addRenderRecord(currentCrystal.blockPosition().below());

            if (silentSwapped) InvUtils.swapBack();
        });
    }

    private void tryPlaceCrystal(Vec3 predictedTargetPos) {
        if (!placeTimer.passedMillise(placeDelay.getValue())) return;

        FindItemResult crystalItem = findCrystalItem();
        if (!crystalItem.found()) return;
        List<PlaceCandidate> candidates = collectPlaceCandidates(predictedTargetPos);

        PlaceCandidate bestNormal = findBestCandidate(candidates,
                placeMinDmg.getValue().floatValue(),
                placeMaxSelfDmg.getValue().floatValue(),
                placeBalance.getValue().floatValue());

        if (bestNormal != null) {
            doPlaceCrystal(bestNormal, crystalItem);
            return;
        }

        if (shouldForcePlace()) {
            PlaceCandidate bestForce = findBestCandidate(candidates,
                    forcePlaceMinDamage.getValue().floatValue(),
                    placeMaxSelfDmg.getValue().floatValue(),
                    forcePlaceBalance.getValue().floatValue());

            if (bestForce != null) {
                doPlaceCrystal(bestForce, crystalItem);
            }
        }
    }

    /**
     * Checks whether Force Place conditions are met:
     * - target health ≤ forcePlaceHealth  OR
     * - target armor durability rate ≤ forcePlaceArmorRate (total armor value)
     */
    private boolean shouldForcePlace() {
        if (target.getHealth() <= forcePlaceHealth.getValue()) return true;

        float targetArmor = (float) target.getAttributeValue(Attributes.ARMOR);
        return targetArmor <= forcePlaceArmorRate.getValue();
    }

    private List<PlaceCandidate> collectPlaceCandidates(Vec3 predictedTargetPos) {
        List<PlaceCandidate> candidates = new ArrayList<>();

        BlockPos center = BlockPos.containing(predictedTargetPos);
        int range = 5;
        double placeRangeSq = placeRange.getValue() * placeRange.getValue();
        Vec3 playerEye = mc.player.getEyePosition();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos supportPos = center.offset(x, y, z);

                    BlockState supportState = mc.level.getBlockState(supportPos);
                    if (!supportState.is(Blocks.OBSIDIAN) && !supportState.is(Blocks.BEDROCK)) continue;
                    BlockPos crystalBlockPos = supportPos.above();
                    if (!mc.level.getBlockState(crystalBlockPos).isAir()) continue;
                    if (!mc.level.getBlockState(crystalBlockPos.above()).isAir()) continue;
                    AABB crystalBB = new AABB(crystalBlockPos);
                    if (!mc.level.getEntities(null, crystalBB).isEmpty()) continue;
                    Vec3 supportCenter = Vec3.atCenterOf(supportPos);
                    if (playerEye.distanceToSqr(supportCenter) > placeRangeSq) continue;
                    Vec3 crystalPos = new Vec3(
                            supportPos.getX() + 0.5,
                            supportPos.getY() + 1.0,
                            supportPos.getZ() + 0.5
                    );

                    float targetDmg = DamageUtils.crystalDamage(target, crystalPos, DamageUtils.ArmorEnchantmentMode.None);
                    float selfDmg = DamageUtils.selfCrystalDamage(crystalPos);

                    candidates.add(new PlaceCandidate(supportPos, crystalPos, targetDmg, selfDmg));
                }
            }
        }

        return candidates;
    }

    private PlaceCandidate findBestCandidate(List<PlaceCandidate> candidates,
                                              float minDmg, float maxSelfDmg, float minBalance) {
        List<PlaceCandidate> valid = new ArrayList<>();

        for (PlaceCandidate c : candidates) {
            if (exceedsSelfDamageLimit(c.selfDmg, maxSelfDmg)) continue;
            if (c.targetDmg < minDmg) continue;
            float balance = c.targetDmg - c.selfDmg;
            if (balance < minBalance) continue;
            valid.add(c);
        }

        if (valid.isEmpty()) return null;

        float bestDamage = Float.MIN_VALUE;
        for (PlaceCandidate c : valid) {
            if (c.targetDmg > bestDamage) {
                bestDamage = c.targetDmg;
            }
        }

        if (aimOptimizeMode.is(AimOptimizeMode.MaxDamage)) {
            PlaceCandidate best = null;
            float bestScore = Float.MIN_VALUE;
            for (PlaceCandidate c : valid) {
                if (c.targetDmg > bestScore) {
                    bestScore = c.targetDmg;
                    best = c;
                }
            }
            return best;
        }

        float threshold = bestDamage - aimDamageSacrifice.getValue().floatValue();
        PlaceCandidate selected = null;
        float bestDelta = Float.MAX_VALUE;
        Vector2f prev = getPrevTickRotation();
        for (PlaceCandidate c : valid) {
            if (c.targetDmg < threshold) continue;
            Vector2f targetRot = RotationUtils.calculate(c.supportPos, Direction.UP);
            float delta = getRotationDelta(prev, targetRot);
            if (delta < bestDelta) {
                bestDelta = delta;
                selected = c;
            }
        }

        return selected != null ? selected : valid.get(0);
    }

    private void doPlaceCrystal(PlaceCandidate candidate, FindItemResult crystalItem) {
        boolean swapped = false;
        InteractionHand hand = crystalItem.getHand();
        if (crystalItem.slot() != mc.player.getInventory().getSelectedSlot() && crystalItem.slot() != 40) {
            switch (placeSwapMode.getValue()) {
                case Swap -> InvUtils.swap(crystalItem.slot(), false);
                case Silent -> swapped = InvUtils.swap(crystalItem.slot(), true);
                default -> { }
            }
            hand = InteractionHand.MAIN_HAND;
        }
        final boolean silentSwapped = swapped;
        final InteractionHand placeHand = hand;
        final int requestPriority = Priority.High.priority;
        final Vector2f rotation = RotationUtils.calculate(candidate.supportPos, Direction.UP);
        final Vec3 hitVec = new Vec3(
                candidate.supportPos.getX() + 0.5,
                candidate.supportPos.getY() + 1.0,
                candidate.supportPos.getZ() + 0.5
        );
        RotationManager.INSTANCE.applyRotation(rotation, placeRotationSpeed.getValue(), requestPriority, record -> {
            if (!isEnabled() || nullCheck()) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }
            if (record.selectedPriorityValue() != requestPriority) return;
            if (!RaytraceUtils.overBlock(record.currentRotation(), Direction.UP, candidate.supportPos, false)) {
                if (silentSwapped) InvUtils.swapBack();
                return;
            }

            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, candidate.supportPos, false);
            InteractionResult result = mc.gameMode.useItemOn(mc.player, placeHand, hitResult);
            if (result.consumesAction()) {
                doSwing(placeSwing.getValue());
                addRenderRecord(candidate.supportPos);
                placeTimer.reset();
            }
            if (silentSwapped) InvUtils.swapBack();
        });
    }

    /**
     * Combined self-damage check — returns {@code true} when the crystal should be
     * <b>rejected</b> (self-damage is too high).
     * <ol>
     *   <li>selfDmg must not exceed the per-action max (placeMaxSelfDmg / breakMaxSelfDmg)</li>
     *   <li>noSuicide: selfDmg must not kill the player (hp + absorption - selfDmg &gt; 0.5)</li>
     *   <li>lethalMaxSelfDamage: when the remaining HP would be critically low, selfDmg
     *       must also be below lethalMaxSelfDamage.</li>
     * </ol>
     */
    private boolean exceedsSelfDamageLimit(float selfDmg, double maxSelfDmg) {
        if (selfDmg > maxSelfDmg) return true;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (noSuicide.getValue() && hp - selfDmg <= 0.5f) return true;
        float remainingHp = hp - selfDmg;
        if (noSuicide.getValue() && remainingHp <= lethalMaxSelfDamage.getValue()
                && selfDmg > lethalMaxSelfDamage.getValue()) {
            return true;
        }

        return false;
    }

    private Vec3 getPredictedTargetPos(LivingEntity entity) {
        if (!motionPrediction.getValue() || lastTargetPos == null) {
            return entity.position();
        }

        Vec3 currentPos = entity.position();
        Vec3 velocity = currentPos.subtract(lastTargetPos);
        int ticks = predictTick.getValue();

        return currentPos.add(velocity.scale(ticks));
    }

    private FindItemResult findCrystalItem() {
        return InvUtils.findInHotbar(Items.END_CRYSTAL);
    }

    private void doSwing(SwingMode mode) {
        switch (mode) {
            case Client -> mc.player.swing(InteractionHand.MAIN_HAND);
            case Packet -> mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            default -> { }
        }
    }

    private void addRenderRecord(BlockPos pos) {
        renderRecords.removeIf(r -> r.pos.equals(pos));
        renderRecords.add(new RenderRecord(pos, renderTimer.getMs()));
    }

    @SubscribeEvent
    private void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck()) return;
        if (renderRecords.isEmpty()) return;

        long now = renderTimer.getMs();
        long totalLife = movingLength.getValue() + fadeLength.getValue();
        renderRecords.removeIf(r -> now - r.time > totalLife);

        for (RenderRecord record : renderRecords) {
            long age = now - record.time;
            float alpha;

            if (age <= movingLength.getValue()) {
                alpha = 1.0f;
            } else {
                float fadeProgress = (float) (age - movingLength.getValue()) / Math.max(1, fadeLength.getValue());
                alpha = 1.0f - Math.min(1.0f, fadeProgress);
            }

            if (alpha <= 0.01f) continue;

            Color fc = filledColor.getValue();
            Color oc = outlineColor.getValue();

            Color filled = new Color(fc.getRed(), fc.getGreen(), fc.getBlue(),
                    Math.max(0, Math.min(255, (int) (fc.getAlpha() * alpha))));
            Color outline = new Color(oc.getRed(), oc.getGreen(), oc.getBlue(),
                    Math.max(0, Math.min(255, (int) (oc.getAlpha() * alpha))));

            AABB box = new AABB(record.pos);
            Render3DUtils.drawFilledBox(box, filled);
            Render3DUtils.drawOutlineBox(event.getPoseStack(), box, outline.getRGB(), 1.5f);
        }
    }

    private record PlaceCandidate(BlockPos supportPos, Vec3 crystalPos, float targetDmg, float selfDmg) { }

    private EndCrystal selectBestBreakCandidate(List<BreakCandidate> candidates) {
        if (candidates.isEmpty()) return null;

        float bestDamage = Float.MIN_VALUE;
        BreakCandidate maxDamage = null;
        for (BreakCandidate c : candidates) {
            if (c.targetDmg > bestDamage) {
                bestDamage = c.targetDmg;
                maxDamage = c;
            }
        }

        if (maxDamage == null || aimOptimizeMode.is(AimOptimizeMode.MaxDamage)) {
            return maxDamage != null ? maxDamage.crystal : null;
        }

        float threshold = bestDamage - aimDamageSacrifice.getValue().floatValue();
        BreakCandidate selected = null;
        float bestDelta = Float.MAX_VALUE;
        for (BreakCandidate c : candidates) {
            if (c.targetDmg < threshold) continue;
            if (c.rotationDelta < bestDelta) {
                bestDelta = c.rotationDelta;
                selected = c;
            }
        }

        return selected != null ? selected.crystal : maxDamage.crystal;
    }

    private Vector2f getPrevTickRotation() {
        Vector2f prev = RotationManager.INSTANCE.lastRotations;
        if (prev == null) {
            return new Vector2f(mc.player.getYRot(), mc.player.getXRot());
        }
        return new Vector2f(prev.x, prev.y);
    }

    private float getRotationDelta(Vector2f from, Vector2f to) {
        float yawDiff = Math.abs(Mth.wrapDegrees(to.x - from.x));
        float pitchDiff = Math.abs(to.y - from.y);
        return (float) Math.hypot(yawDiff, pitchDiff);
    }

    private record BreakCandidate(
            EndCrystal crystal,
            float targetDmg,
            Vector2f targetRotation,
            float rotationDelta
    ) { }

    private record RenderRecord(BlockPos pos, long time) { }

    private enum SwapMode {
        None,
        Swap,
        Silent,
    }

    private enum SwingMode {
        None,
        Client,
        Packet,
    }

    private enum AimOptimizeMode {
        MaxDamage,
        LowRotation
    }

}
