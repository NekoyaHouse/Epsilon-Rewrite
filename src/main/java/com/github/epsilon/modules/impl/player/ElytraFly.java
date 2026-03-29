package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.events.MoveEvent;
import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.events.TravelEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * ElytraFly Module - Enhanced with multiple flight modes
 * By Nemophilist2009
 */
public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);
    }

    public enum Mode {
        CONTROL,
        BOOST,
        BOUNCE,
        PACKET,
        ONE_DOT_21,
        PITCH_40
    }

    public enum FreezeMode {
        NORMAL,
        PACKET
    }

    // Mode selection
    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.CONTROL);

    // Visual Spoof (for Armor mode compatibility)
    private final BoolSetting visualSpoof = boolSetting("VisualSpoof", false);

    // Common Settings
    private final BoolSetting lagLimit = boolSetting("LagLimit", true);
    private final BoolSetting autoFly = boolSetting("AutoFly", false, () -> mode.is(Mode.CONTROL));

    // Control Mode Settings
    private final DoubleSetting speed = doubleSetting("Speed", 1.0, 0.1, 5.0, 0.1, () -> mode.is(Mode.CONTROL));
    private final DoubleSetting verticalSpeed = doubleSetting("VerticalSpeed", 1.0, 0.1, 5.0, 0.1, () -> mode.is(Mode.CONTROL));
    private final BoolSetting fireworks = boolSetting("Fireworks", false, () -> mode.is(Mode.CONTROL));
    private final DoubleSetting delay = doubleSetting("Delay", 10.0, 0.0, 40.0, 1.0, () -> mode.is(Mode.CONTROL) && fireworks.getValue());
    private final BoolSetting swapAlternative = boolSetting("SwapAlternative", false, () -> mode.is(Mode.CONTROL) && fireworks.getValue());
    private final BoolSetting rotate = boolSetting("Rotate", false, () -> mode.is(Mode.CONTROL));
    private final BoolSetting freeze = boolSetting("Freeze", false, () -> mode.is(Mode.CONTROL));
    private final EnumSetting<FreezeMode> freezeMode = enumSetting("FreezeMode", FreezeMode.NORMAL, () -> mode.is(Mode.CONTROL) && freeze.getValue());
    private final BoolSetting noRotation = boolSetting("NoRotation", true, () -> mode.is(Mode.CONTROL) && freeze.getValue());

    // Boost Mode Settings
    private final BoolSetting boost = boolSetting("Boost", false, () -> mode.is(Mode.BOOST));

    // Bounce Mode Settings
    private final DoubleSetting bouncePitch = doubleSetting("Pitch", -80.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.BOUNCE));
    private final DoubleSetting maxBps = doubleSetting("MaxBps", 140.0, 0.0, 300.0, 1.0, () -> mode.is(Mode.BOUNCE));
    private final BoolSetting bounceBoost = boolSetting("Boost", false, () -> mode.is(Mode.BOUNCE));

    // Packet Mode Settings
    private final BoolSetting grimV3 = boolSetting("GrimV3", false, () -> mode.is(Mode.PACKET));
    private final DoubleSetting grimDelay = doubleSetting("GrimDelay", 5.0, 0.0, 10.0, 1.0, () -> mode.is(Mode.PACKET) && grimV3.getValue());
    private final BoolSetting packetFly = boolSetting("Packet", false, () -> mode.is(Mode.PACKET));

    // 1.21+ Mode Settings
    private final DoubleSetting minBps = doubleSetting("MinBps", 10.0, 0.0, 200.0, 1.0, () -> mode.is(Mode.ONE_DOT_21));

    // Pitch40 Mode Settings
    private final DoubleSetting pitch40Value = doubleSetting("Pitch", -40.0, -90.0, 90.0, 1.0, () -> mode.is(Mode.PITCH_40));

    // Obstacle Passer
    private final BoolSetting obstaclePasser = boolSetting("ObstaclePasser", false, () -> mode.is(Mode.CONTROL));

    // State variables
    private float yaw;
    private float rotationPitch;
    private boolean lagBackDetected;
    private boolean isFrozen;
    private Vec3 freezePos;
    private int fireworkTimer;

    // Timers
    private final TimerUtils fireworkTimerUtils = new TimerUtils();

    private boolean isControlMode() {
        return mode.is(Mode.CONTROL);
    }

    private boolean isBoostMode() {
        return mode.is(Mode.BOOST);
    }

    private boolean isBounceMode() {
        return mode.is(Mode.BOUNCE);
    }

    private boolean isPacketMode() {
        return mode.is(Mode.PACKET);
    }

    private boolean isOneDot21Mode() {
        return mode.is(Mode.ONE_DOT_21);
    }

    private boolean isPitch40Mode() {
        return mode.is(Mode.PITCH_40);
    }

    public boolean shouldVisualSpoof() {
        return isEnabled() && visualSpoof.getValue();
    }

    private void setShiftKey(boolean shift) {
        Input input = mc.player.input.keyPresses;
        mc.player.input.keyPresses = new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), shift, input.sprint());
        mc.player.setShiftKeyDown(shift);
        mc.getConnection().send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));
    }

    private void releaseShiftKey() {
        this.setShiftKey(false);
    }

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            this.yaw = mc.player.getYRot();
            this.rotationPitch = mc.player.getXRot();
            this.freezePos = null;
            this.lagBackDetected = false;
            this.isFrozen = false;
            this.fireworkTimer = 0;
            this.fireworkTimerUtils.reset();

            if (this.autoFly.getValue() && mc.player.onGround()) {
                mc.player.jumpFromGround();
            }
        }
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            this.releaseShiftKey();
            this.freezePos = null;
            this.isFrozen = false;
        }
    }

    @SubscribeEvent
    public void onUpdateRotate(MotionEvent event) {
        if (!nullCheck()) {
            if (mc.player.isFallFlying()) {
                if (this.mode.is(Mode.BOUNCE)) {
                    event.setPitch((float) (double) this.bouncePitch.getValue());
                } else if (this.mode.is(Mode.PITCH_40)) {
                    event.setPitch((float) (double) this.pitch40Value.getValue());
                } else if (this.mode.is(Mode.CONTROL) && this.rotate.getValue()) {
                    if (this.isMoving()) {
                        if (mc.options.keyJump.isDown()) {
                            this.rotationPitch = -50.0F;
                        } else if (mc.options.keyShift.isDown()) {
                            this.rotationPitch = 50.0F;
                        } else {
                            this.rotationPitch = 0.1F;
                        }
                    } else if (mc.options.keyJump.isDown()) {
                        this.rotationPitch = -90.0F;
                    } else if (mc.options.keyShift.isDown()) {
                        this.rotationPitch = 90.0F;
                    } else {
                        this.rotationPitch = 0.0F;
                    }
                    event.setPitch(this.rotationPitch);
                }
            }
        }
    }

    @SubscribeEvent
    public void onUpdate(PlayerTickEvent.Pre event) {
        if (!nullCheck() && event.getEntity() == mc.player) {
            if (!mc.player.isFallFlying()) {
                // AutoFly logic
                if (this.autoFly.getValue() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                    if (mc.player.onGround() && mc.player.isSprinting()) {
                        mc.player.jumpFromGround();
                    } else if (!mc.player.onGround()) {
                        mc.player.startFallFlying();
                    }
                }
                return;
            }

            // Mode-specific updates
            if (this.mode.is(Mode.CONTROL)) {
                this.updateControlMode();
            } else if (this.mode.is(Mode.BOOST)) {
                this.updateBoostMode();
            } else if (this.mode.is(Mode.BOUNCE)) {
                this.updateBounceMode();
            } else if (this.mode.is(Mode.PACKET)) {
                this.updatePacketMode();
            } else if (this.mode.is(Mode.ONE_DOT_21)) {
                this.updateOneDot21Mode();
            } else if (this.mode.is(Mode.PITCH_40)) {
                this.updatePitch40Mode();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerMove(MoveEvent event) {
        if (nullCheck() || !mc.player.isFallFlying()) {
            return;
        }

        // Anti-collision check for unloaded chunks
        int chunkX = (int) Math.floor(mc.player.getX() / 16.0);
        int chunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
        if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            event.setX(0.0);
            event.setY(0.0);
            event.setZ(0.0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!nullCheck()) {
            if (this.lagLimit.getValue() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                this.lagBackDetected = true;
            }
        }
    }

    @SubscribeEvent
    private void onTravel(TravelEvent event) {
        if (!nullCheck() && mc.player.isFallFlying()) {
            if (this.mode.is(Mode.CONTROL)) {
                this.applyControlMovement();
                event.setCanceled(true);
            }
        }
    }

    // ==================== Control Mode ====================

    private void updateControlMode() {
        // Lagback check
        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        // Firework logic
        if (this.fireworks.getValue() && this.isMoving()) {
            int delayMs = this.delay.getValue().intValue() * 100;
            if (this.fireworkTimerUtils.passedMillise((long) delayMs)) {
                this.useFirework();
                this.fireworkTimerUtils.reset();
            }
        }

        // Freeze logic
        if (this.freeze.getValue() && !this.isMoving()) {
            if (this.freezePos == null) {
                this.freezePos = mc.player.position();
            }
            this.applyFreeze();
        } else {
            this.freezePos = null;
            this.isFrozen = false;
        }
    }

    private void applyControlMovement() {
        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        double verticalSpd = this.verticalSpeed.getValue();
        if (mc.options.keyJump.isDown()) {
            this.setY(verticalSpd);
        } else if (mc.options.keyShift.isDown()) {
            this.setY(-verticalSpd);
        } else {
            this.setY(0.0);
        }

        if (mc.options.keyUp.isDown()) {
            float yaw = (float) Math.toRadians(mc.player.getYRot());
            double forwardSpeed = this.speed.getValue();
            this.setX(-Mth.sin(yaw) * forwardSpeed);
            this.setZ(Mth.cos(yaw) * forwardSpeed);
        } else {
            this.setX(0.0);
            this.setZ(0.0);
        }
    }

    private void applyFreeze() {
        if (this.freezePos == null) return;

        FreezeMode freezeMode = this.freezeMode.getValue();
        if (freezeMode == FreezeMode.NORMAL) {
            if (this.noRotation.getValue()) {
                mc.player.setDeltaMovement(0.0, 0.0, 0.0);
                mc.player.setPos(this.freezePos.x, this.freezePos.y, this.freezePos.z);
            } else {
                mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            }
            this.isFrozen = true;
        } else if (freezeMode == FreezeMode.PACKET) {
            // Packet freeze - send position packets
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            this.isFrozen = true;
        }
    }

    private void useFirework() {
        if (mc.player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            return;
        }

        if (this.swapAlternative.getValue()) {
            // Inventory swap logic
            FindItemResult inventorySlot = InvUtils.find(Items.FIREWORK_ROCKET);
            if (inventorySlot.found()) {
                InvUtils.invSwap(inventorySlot.slot());
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                InvUtils.invSwapBack();
                return;
            }
        }

        FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (firework.found()) {
            InvUtils.swap(firework.slot(), true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            InvUtils.swapBack();
        }
    }

    // ==================== Boost Mode ====================

    private void updateBoostMode() {
        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        if (this.boost.getValue() && mc.options.keyJump.isDown()) {
            this.applyVanillaBoost();
        }
    }

    private void applyVanillaBoost() {
        float yaw = mc.player.getYRot();
        final double GRIM_AIR_FRICTION = 0.0264444413;
        final double x = GRIM_AIR_FRICTION * Math.cos(Math.toRadians(yaw + 90.0f));
        final double z = GRIM_AIR_FRICTION * Math.sin(Math.toRadians(yaw + 90.0f));
        double motionX = mc.player.getDeltaMovement().x + x;
        double motionZ = mc.player.getDeltaMovement().z + z;
        mc.player.setDeltaMovement(motionX, mc.player.getDeltaMovement().y, motionZ);
    }

    // ==================== Bounce Mode ====================

    private void updateBounceMode() {
        if (!mc.player.isFallFlying()) {
            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                mc.player.startFallFlying();
            }
            return;
        }

        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        if (this.bounceBoost.getValue()) {
            Vec3 vel = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(vel.x, 0.1, vel.z);
        }
    }

    // ==================== Packet Mode ====================

    private void updatePacketMode() {
        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        if (this.grimV3.getValue()) {
            this.setShiftKey(true);
            mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            this.releaseShiftKey();
        }

        if (this.packetFly.getValue()) {
            // Packet fly logic
            mc.player.setDeltaMovement(0.0, -0.01, 0.0);
        }
    }

    // ==================== 1.21+ Mode ====================

    private void updateOneDot21Mode() {
        if (!mc.player.isFallFlying()) {
            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                mc.player.startFallFlying();
            }
            return;
        }

        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        // High speed bounce logic
        if (mc.player.onGround()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.5, mc.player.getDeltaMovement().z);
            mc.player.jumpFromGround();
        }
    }

    // ==================== Pitch40 Mode ====================

    private void updatePitch40Mode() {
        if (!mc.player.isFallFlying()) {
            if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                mc.player.startFallFlying();
            }
            return;
        }

        if (this.lagLimit.getValue() && this.lagBackDetected) {
            return;
        }

        // Infinite flight / AFK mode
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x * 0.99, 0.0, mc.player.getDeltaMovement().z * 0.99);
    }

    // ==================== Utility Methods ====================

    private boolean isMoving() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    private void setX(double x) {
        Vec3 movement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(x, movement.y, movement.z);
    }

    private void setY(double y) {
        Vec3 movement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(movement.x, y, movement.z);
    }

    private void setZ(double z) {
        Vec3 movement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(movement.x, movement.y, z);
    }
}
