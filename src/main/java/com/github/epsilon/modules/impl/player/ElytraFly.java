package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.MotionEvent;
import com.github.epsilon.events.MoveEvent;
import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.events.TravelEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.player.ChatUtils;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.player.MoveUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * @author ZhouJiaMei
 * Skidding from alien v4
 */
public class ElytraFly extends Module {

    public static final ElytraFly INSTANCE = new ElytraFly();

    private ElytraFly() {
        super("ElytraFly", Category.PLAYER);
    }

    public enum Mode {
        Control,
        Boost,
        Bounce,
        Freeze,
        None,
        Rotation,
        Pitch
    }

    public final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Control);
    private final BoolSetting infiniteDura = boolSetting("InfiniteDura", false);
    private final BoolSetting packet = boolSetting("Packet", false);
    private final IntSetting packetDelay = intSetting("PacketDelay", 0, 0, 20, 1, packet::getValue);
    private final BoolSetting setFlag = boolSetting("SetFlag", false, () -> !mode.is(Mode.Bounce));
    private final BoolSetting firework = boolSetting("Firework", false);
    private final BoolSetting packetInteract = boolSetting("PacketInteract", true, firework::getValue);
    private final BoolSetting inventory = boolSetting("InventorySwap", true, firework::getValue);
    private final BoolSetting onlyOne = boolSetting("OnlyOne", true, firework::getValue);
    private final BoolSetting usingPause = boolSetting("UsingPause", true, firework::getValue);
    private final BoolSetting autoJump = boolSetting("AutoJump", true, () -> mode.is(Mode.Bounce));
    private final DoubleSetting upPitch = doubleSetting("UpPitch", 0.0, 0.0, 90.0, 1.0, () -> mode.is(Mode.Control));
    private final DoubleSetting upFactor = doubleSetting("UpFactor", 1.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final DoubleSetting downFactor = doubleSetting("FallSpeed", 1.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final DoubleSetting speed = doubleSetting("Speed", 1.0, 0.1, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final BoolSetting speedLimit = boolSetting("SpeedLimit", true, () -> mode.is(Mode.Control));
    private final DoubleSetting maxSpeed = doubleSetting("MaxSpeed", 2.5, 0.1, 10.0, 0.1, () -> speedLimit.getValue() && mode.is(Mode.Control));
    private final BoolSetting noDrag = boolSetting("NoDrag", false, () -> mode.is(Mode.Control));

    private final BoolSetting autoStop = boolSetting("AutoStop", true);
    private final BoolSetting sprint = boolSetting("Sprint", true, () -> mode.is(Mode.Bounce));
    private final DoubleSetting pitch = doubleSetting("Pitch", 88.0, -90.0, 90.0, 0.1, () -> mode.is(Mode.Bounce));
    private final BoolSetting instantFly = boolSetting("AutoStart", true, () -> !mode.is(Mode.Bounce));
    private final BoolSetting checkSpeed = boolSetting("CheckSpeed", false, () -> !mode.is(Mode.Bounce));
    private final DoubleSetting minSpeed = doubleSetting("MinSpeed", 70.0, 0.1, 200.0, 0.1, () -> !mode.is(Mode.Bounce));
    private final IntSetting delay = intSetting("Delay", 1000, 0, 20000, 50, () -> !mode.is(Mode.Bounce));
    private final DoubleSetting timeout = doubleSetting("Timeout", 0.0, 0.1, 1.0, 0.1, () -> !mode.is(Mode.Bounce));
    private final DoubleSetting sneakDownSpeed = doubleSetting("DownSpeed", 1.0, 0.1, 10.0, 0.1, () -> mode.is(Mode.Control));
    private final DoubleSetting boost = doubleSetting("Boost", 1.0, 0.1, 4.0, 0.1, () -> mode.is(Mode.Boost));
    private final BoolSetting freeze = boolSetting("Freeze", false, () -> mode.is(Mode.Rotation));
    private final BoolSetting motionStop = boolSetting("MotionStop", false, () -> mode.is(Mode.Rotation));
    private final DoubleSetting infiniteMaxSpeed = doubleSetting("InfiniteMaxSpeed", 150.0, 50.0, 170.0, 1.0, () -> mode.is(Mode.Pitch));
    private final DoubleSetting infiniteMinSpeed = doubleSetting("InfiniteMinSpeed", 25.0, 10.0, 70.0, 1.0, () -> mode.is(Mode.Pitch));
    private final DoubleSetting infiniteMaxHeight = doubleSetting("InfiniteMaxHeight", 200.0, -50.0, 360.0, 1.0, () -> mode.is(Mode.Pitch));
    private final BoolSetting releaseSneak = boolSetting("ReleaseSneak", false);

    private boolean flying = false;
    private int packetDelayInt = 0;

    private float yaw = 0;
    private float rotationPitch = 0;

    private boolean prev;
    private float prePitch;
    private boolean hasElytra = false;

    private final TimerUtils fireworkTimer = new TimerUtils();
    private final TimerUtils instantFlyTimer = new TimerUtils();

    private boolean down;
    private float lastInfinitePitch;
    private float infinitePitch;

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        hasElytra = false;
        yaw = mc.player.getYRot();
        rotationPitch = mc.player.getXRot();
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        if (releaseSneak.getValue()) {
            mc.options.keyShift.setDown(false);
            mc.player.setShiftKeyDown(false);
        }
    }

    public static boolean recastElytra(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (checkConditions(player) && ignoreGround(player)) {
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            if (INSTANCE.setFlag.getValue()) {
                mc.player.startFallFlying();
            }
            return true;
        } else return false;
    }

    public static boolean checkConditions(LocalPlayer player) {
        ItemStack itemStack = player.getItemBySlot(EquipmentSlot.CHEST);
        return !player.getAbilities().flying
                && !player.isPassenger()
                && !player.onClimbable()
                && itemStack.is(Items.ELYTRA)
                && LivingEntity.canGlideUsing(itemStack, EquipmentSlot.CHEST);
    }

    private static boolean ignoreGround(LocalPlayer player) {
        if (!player.isInWater() && !player.hasEffect(MobEffects.LEVITATION)) {
            ItemStack itemStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (itemStack.is(Items.ELYTRA) && LivingEntity.canGlideUsing(itemStack, EquipmentSlot.CHEST)) {
                player.startFallFlying();
                return true;
            } else return false;
        } else return false;
    }

    private void boost() {
        if (hasElytra) {
            if (!isFallFlying()) {
                return;
            }
            float yaw = (float) Math.toRadians(mc.player.getYRot());
            if (mc.options.keyUp.isDown()) {
                mc.player.addDeltaMovement(new Vec3(-Mth.sin(yaw) * boost.getValue() / 10, 0, Mth.cos(yaw) * boost.getValue() / 10));
            }
        }
    }

    private void startFallFlying() {
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }

    private void clickSlot(int slot) {
        int containerSlot = slot < 9 ? slot + 36 : slot;
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, containerSlot, 6, ContainerInput.SWAP, mc.player);
    }

    @SubscribeEvent
    private void onClientTickFirework(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (firework.getValue()) {
            if (fireworkTimer.passedMillise(delay.getValue()) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isFallFlying()) {
                ChatUtils.addChatMessage("b");
                off();
                fireworkTimer.reset();
            } else {
                ChatUtils.addChatMessage("fuck b");
            }
        }
    }

    @SubscribeEvent
    private void onRotation(MotionEvent event) {
        if (mode.is(Mode.Rotation)) {
            if (isFallFlying()) {
                if (MoveUtils.isMoving()) {
                    if (mc.options.keyJump.isDown()) {
                        rotationPitch = (-45);
                    } else if (mc.options.keyShift.isDown()) {
                        rotationPitch = (45);
                    } else {
                        rotationPitch = (-1.9f);
                        if (motionStop.getValue()) {
                            setY(0);
                        }
                    }
                } else {
                    if (mc.options.keyJump.isDown()) {
                        rotationPitch = (-89);
                    } else if (mc.options.keyShift.isDown()) {
                        rotationPitch = (89);
                    } else {
                        if (motionStop.getValue()) {
                            setY(0);
                        }
                    }
                }
                if (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()) {
                    yaw = getSprintYaw(mc.player.getYRot());
                } else if (motionStop.getValue()) {
                    setX(0);
                    setZ(0);
                }
                event.setYaw(yaw);
                event.setPitch(rotationPitch);
            }
        } else if (mode.is(Mode.Pitch)) {
            if (isFallFlying()) {
                event.setPitch(infinitePitch);
            }
        } else if (mode.is(Mode.Bounce)) {
            if (isFallFlying()) {
                event.setPitch(pitch.getValue().floatValue());
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private void onClientTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        getInfinitePitch();
        flying = false;
        if (packet.getValue()) {
            hasElytra = InvUtils.find(Items.ELYTRA).found();
        } else {
            ItemStack chestStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            hasElytra = chestStack.is(Items.ELYTRA) && LivingEntity.canGlideUsing(chestStack, EquipmentSlot.CHEST);
            if (infiniteDura.getValue()) {
                if (!mc.player.onGround() && hasElytra) {
                    flying = true;
                    startFallFlying();
                    if (setFlag.getValue()) mc.player.startFallFlying();
                }
            }
            if (mode.is(Mode.Bounce)) {
                return;
            }
        }
        double x = mc.player.getX() - mc.player.xo;
        double y = mc.player.getY() - mc.player.yo;
        double z = mc.player.getZ() - mc.player.zo;
        double dist = Math.sqrt(x * x + z * z + y * y) / 1000.0;
        double div = 0.05 / 3600.0;
        float timer = 1.0f;
        final double speed = dist / div * timer;
        if (mode.getValue() == Mode.Boost) {
            boost();
        }
        if (packet.getValue()) {
            if (mc.player.onGround()) return;
            packetDelayInt++;
            if (packetDelayInt <= packetDelay.getValue()) return;
            FindItemResult elytra = InvUtils.find(Items.ELYTRA);
            if (elytra.found()) {
                clickSlot(elytra.slot());
                startFallFlying();
                mc.player.startFallFlying();
                if ((!checkSpeed.getValue() || speed <= minSpeed.getValue()) && firework.getValue() && fireworkTimer.passedMillise(delay.getValue()) && (MoveUtils.isMoving() || mode.is(Mode.Rotation) && mc.options.keyJump.isDown()) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isFallFlying()) {
                    off();
                    fireworkTimer.reset();
                }
                clickSlot(elytra.slot());
                packetDelayInt = 0;
            } else {
                elytra = InvUtils.findInHotbar(Items.ELYTRA);
                if (elytra.found()) {
                    clickSlot(elytra.slot());
                    startFallFlying();
                    mc.player.startFallFlying();
                    if ((!checkSpeed.getValue() || speed <= minSpeed.getValue()) && firework.getValue() && fireworkTimer.passedMillise(delay.getValue()) && (MoveUtils.isMoving() || mode.is(Mode.Rotation) && mc.options.keyJump.isDown()) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isFallFlying()) {
                        off();
                        fireworkTimer.reset();
                    }
                    clickSlot(elytra.slot());
                    packetDelayInt = 0;
                }
            }
            return;
        }
        if ((!checkSpeed.getValue() || speed <= minSpeed.getValue()) && firework.getValue() && fireworkTimer.passedMillise(delay.getValue()) && (MoveUtils.isMoving() || mode.is(Mode.Rotation) && mc.options.keyJump.isDown()) && (!mc.player.isUsingItem() || !usingPause.getValue()) && isFallFlying()) {
            off();
            fireworkTimer.reset();
        }
        if (!isFallFlying() && hasElytra) {
            fireworkTimer.setMs(99999999);
            if (!mc.player.onGround() && instantFly.getValue() && mc.player.getDeltaMovement().y < 0D && !infiniteDura.getValue()) {
                if (!instantFlyTimer.passedMillise((long) (1000 * timeout.getValue()))) return;
                instantFlyTimer.reset();
                startFallFlying();
                if (setFlag.getValue()) mc.player.startFallFlying();
            }
        }
    }

    @SubscribeEvent
    private void onPlayerMove(MoveEvent event) {
        if (autoStop.getValue()) {
            if (isFallFlying()) {
                int chunkX = (int) ((mc.player.getX()) / 16);
                int chunkZ = (int) ((mc.player.getZ()) / 16);
                if (!mc.level.hasChunk(chunkX, chunkZ)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Bounce) && hasElytra) {
            if (autoJump.getValue()) {
                mc.options.keyJump.setDown(true);
            }

            if (checkConditions(mc.player) && sprint.getValue()) {
                mc.player.setSprinting(true);
            }
        }
    }

    @SubscribeEvent
    private void onTickPost(ClientTickEvent.Post event) {
        if (!isFallFlying()) {
            startFallFlying();
        }

        if (checkConditions(mc.player)) {
            if (!sprint.getValue()) {
                // Sprinting all the time (when not on ground) makes it rubberband on certain anticheats.
                if (isFallFlying()) {
                    mc.player.setSprinting(mc.player.onGround());
                } else {
                    mc.player.setSprinting(true);
                }
            }
        }
    }

    @SubscribeEvent
    private void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Bounce) && hasElytra && event.getPacket() instanceof ServerboundPlayerCommandPacket packet && packet.getAction() == ServerboundPlayerCommandPacket.Action.START_FALL_FLYING && !sprint.getValue()) {
            mc.player.setSprinting(true);
        }
    }

    @SubscribeEvent
    private void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (mode.is(Mode.Bounce) && hasElytra && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            mc.player.stopFallFlying();
        }
    }

    @SubscribeEvent
    private void onTravel(TravelEvent event) {
        if (nullCheck()) return;
        /*if (!AntiCheat.INSTANCE.movementSync()) {
            if (mode.is(Mode.Bounce) && hasElytra) {
                if (event.isPre()) {
                    prev = true;
                    prePitch = mc.player.getPitch();
                    mc.player.setPitch(pitch.getValueFloat());
                } else {
                    if (prev) {
                        prev = false;
                        mc.player.setPitch(prePitch);
                    }
                }
            } else if (mode.is(Mode.Pitch) && isFallFlying()) {
                if (event.isPre()) {
                    prev = true;
                    prePitch = mc.player.getPitch();
                    mc.player.setPitch(lastInfinitePitch);
                } else {
                    if (prev) {
                        prev = false;
                        mc.player.setPitch(prePitch);
                    }
                }
            }
        }*/
    }

    @SubscribeEvent
    public void onMove(TravelEvent event) {
        if (nullCheck() || !hasElytra || !isFallFlying()) return;
        if (mode.is(Mode.Freeze) || mode.is(Mode.Rotation) && freeze.getValue()) {
            if (!MoveUtils.isMoving() && !mc.options.keyJump.isDown() && !mc.options.keyShift.isDown()) {
                event.setCanceled(true);
                return;
            }
        }
        if (mode.getValue() == Mode.Control) {
            if (firework.getValue()) {
                if (!(mc.options.keyShift.isDown() && mc.player.input.keyPresses.jump())) {
                    if (mc.options.keyShift.isDown()) {
                        setY(-sneakDownSpeed.getValue());
                    } else if (mc.player.input.keyPresses.jump()) {
                        setY(upFactor.getValue());
                    } else {
                        setY(-0.00000000003D * downFactor.getValue());
                    }
                } else {
                    setY(0);
                }
                double[] dir = directionSpeed(speed.getValue());
                setX(dir[0]);
                setZ(dir[1]);
            } else {
                Vec3 lookVec = getRotationVec(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                double motionDist = Math.sqrt(getX() * getX() + getZ() * getZ());
                if (mc.player.input.keyPresses.shift()) {
                    setY(-sneakDownSpeed.getValue());
                } else if (!mc.player.input.keyPresses.jump()) {
                    setY(-0.00000000003D * downFactor.getValue());
                }
                if (mc.player.input.keyPresses.jump()) {
                    if (motionDist > upFactor.getValue() / upFactor.getMax()) {
                        double rawUpSpeed = motionDist * 0.01325D;
                        setY(getY() + rawUpSpeed * 3.2D);
                        setX(getX() - lookVec.x * rawUpSpeed / lookDist);
                        setZ(getZ() - lookVec.z * rawUpSpeed / lookDist);
                    } else {
                        double[] dir = directionSpeed(speed.getValue());
                        setX(dir[0]);
                        setZ(dir[1]);
                    }
                }
                if (lookDist > 0.0D) {
                    setX(getX() + (lookVec.x / lookDist * motionDist - getX()) * 0.1D);
                    setZ(getZ() + (lookVec.z / lookDist * motionDist - getZ()) * 0.1D);
                }
                if (!mc.player.input.keyPresses.jump()) {
                    double[] dir = directionSpeed(speed.getValue());
                    setX(dir[0]);
                    setZ(dir[1]);
                }
                if (!noDrag.getValue()) {
                    setY(getY() * 0.9900000095367432D);
                    setX(getX() * 0.9800000190734863D);
                    setZ(getZ() * 0.9900000095367432D);
                }
                double finalDist = Math.sqrt(getX() * getX() + getZ() * getZ());
                if (speedLimit.getValue() && finalDist > maxSpeed.getValue()) {
                    setX(getX() * maxSpeed.getValue() / finalDist);
                    setZ(getZ() * maxSpeed.getValue() / finalDist);
                }
                event.setCanceled(true);
                mc.player.move(MoverType.SELF, mc.player.getDeltaMovement());
            }
        }
    }

    private double getX() {
        return mc.player.getDeltaMovement().x;
    }

    private double getY() {
        return mc.player.getDeltaMovement().y;
    }

    private double getZ() {
        return mc.player.getDeltaMovement().z;
    }

    private double[] directionSpeed(double speed) {
        float forward = mc.player.input.getMoveVector().y;
        float left = mc.player.input.getMoveVector().x;
        return directionSpeed(speed, forward, left);
    }

    private double[] directionSpeed(double speed, float forward, float side) {
        float yaw = mc.player.yRotO + (mc.player.getYRot() - mc.player.yRotO) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (side < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            side = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        final double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        final double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        final double posX = forward * speed * cos + side * speed * sin;
        final double posZ = forward * speed * sin - side * speed * cos;
        return new double[]{posX, posZ};
    }

    private Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    private Vec3 getRotationVec(float a) {
        return this.getRotationVector(-upPitch.getValue().floatValue(), mc.player.getYRot(a));
    }


    private void getInfinitePitch() {
        lastInfinitePitch = infinitePitch;
        double currentPlayerSpeed = Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo);
        if (mc.player.getY() < infiniteMaxHeight.getValue()) {
            if (currentPlayerSpeed * 72f < infiniteMinSpeed.getValue() && !down)
                down = true;
            if (currentPlayerSpeed * 72f > infiniteMaxSpeed.getValue() && down)
                down = false;
        } else down = true;

        if (down) infinitePitch += 3;
        else infinitePitch -= 3;

        infinitePitch = Mth.clamp(infinitePitch, -40, 40);
    }

    public void off() {
        if (onlyOne.getValue()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof FireworkRocketEntity fireworkRocketEntity) {
                    if (fireworkRocketEntity.getOwner() == mc.player) {
                        return;
                    }
                }
            }
        }

        fireworkTimer.reset();

        FindItemResult firework;
        if (mc.player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
            if (packetInteract.getValue()) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(), RotationManager.INSTANCE.getYaw(), RotationManager.INSTANCE.getPitch()));
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
        } else if (inventory.getValue() && (firework = InvUtils.find(Items.FIREWORK_ROCKET)).found()) {
            InvUtils.invSwap(firework.slot());
            if (packetInteract.getValue()) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(), RotationManager.INSTANCE.getYaw(), RotationManager.INSTANCE.getPitch()));
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            InvUtils.invSwapBack();
            mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        } else if ((firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET)).found()) {
            InvUtils.swap(firework.slot(), true);
            if (packetInteract.getValue()) {
                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(), RotationManager.INSTANCE.getYaw(), RotationManager.INSTANCE.getPitch()));
            } else {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            InvUtils.swapBack();
        }
    }

    private boolean isFallFlying() {
        return mc.player.isFallFlying() || packet.getValue() && hasElytra && !mc.player.onGround() || flying;
    }

    private void setX(double x) {
        Vec3 deltaMovement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(x, deltaMovement.y, deltaMovement.z);
    }

    private void setY(double y) {
        Vec3 deltaMovement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(deltaMovement.x, y, deltaMovement.z);
    }

    private void setZ(double z) {
        Vec3 deltaMovement = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(deltaMovement.x, deltaMovement.y, z);
    }

    private float getSprintYaw(float yaw) {
        if (mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) {
            if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                yaw -= 45f;
            } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                yaw += 45f;
            }
        } else if (mc.options.keyDown.isDown() && !mc.options.keyUp.isDown()) {
            yaw += 180f;
            if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                yaw += 45f;
            } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                yaw -= 45f;
            }
        } else if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
            yaw -= 90f;
        } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
            yaw += 90f;
        }
        return Mth.wrapDegrees(yaw);
    }

}
