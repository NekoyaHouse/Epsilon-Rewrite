package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.EnumSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.player.FindItemResult;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.player.MoveUtils;
import com.github.epsilon.utils.rotation.Priority;
import com.github.epsilon.utils.rotation.RaytraceUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import com.github.epsilon.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector2f;

import java.util.List;

public class Surround extends Module {

    public static final Surround INSTANCE = new Surround();

    private Surround() {
        super("Surround", Category.COMBAT);
    }

    private static final List<BlockPos> OFFSETS = List.of(
            new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(-1, 0, 0)
    );

    private enum SwapMode {
        Normal,
        Silent,
        InvSwitch
    }

    private final EnumSetting<SwapMode> swapMode = enumSetting("Swap Mode", SwapMode.Silent);
    private final IntSetting placeDelay = intSetting("Place Delay", 50, 0, 1000, 1);
    private final BoolSetting groundCheck = boolSetting("Ground Check", true);
    private final BoolSetting autoCenter = boolSetting("Auto Center", true);
    private final BoolSetting rotate = boolSetting("Rotate", false);
    private final BoolSetting attackCrystal = boolSetting("Attack Crystal", true);
    private final BoolSetting disableOnMove = boolSetting("Disable On Move", true);
    private final BoolSetting useEnderChest = boolSetting("Use Ender Chest", true);

    private BlockPos anchorPos;
    private final TimerUtils placeTimer = new TimerUtils();

    @Override
    protected void onEnable() {
        placeTimer.setMs(placeDelay.getValue().longValue());
        anchorPos = nullCheck() ? null : mc.player.blockPosition();
        if (!nullCheck() && autoCenter.getValue()) {
            centerPlayer();
        }
    }

    @Override
    protected void onDisable() {
        anchorPos = null;
    }

    @SubscribeEvent
    private void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (groundCheck.getValue() && !mc.player.onGround()) return;

        BlockPos currentPos = mc.player.blockPosition();
        if (anchorPos == null) {
            anchorPos = currentPos;
        }

        if (disableOnMove.getValue() && !currentPos.equals(anchorPos) && MoveUtils.isMoving()) {
            toggle();
            return;
        }

        if (!MoveUtils.isMoving()) {
            anchorPos = currentPos;
        }

        if (!placeTimer.passedMillise(placeDelay.getValue())) return;

        for (BlockPos offset : OFFSETS) {
            BlockPos targetPos = anchorPos.offset(offset);
            if (!mc.level.getBlockState(targetPos).canBeReplaced()) continue;

            if (attackCrystal.getValue()) {
                fuckCrystal(targetPos);
            }

            if (!BlockUtils.canPlaceAt(targetPos)) continue;

            PlaceTarget placeTarget = updatePlaceTarget(targetPos);
            if (placeTarget == null) continue;

            FindItemResult result = swapMode.is(SwapMode.InvSwitch) ? InvUtils.find(this::item) : InvUtils.findInHotbar(this::item);
            if (!result.found()) return;

            tryPlace(placeTarget, result);

            return;
        }
    }

    private void centerPlayer() {
        double centeredX = Math.floor(mc.player.getX()) + 0.5;
        double centeredZ = Math.floor(mc.player.getZ()) + 0.5;
        mc.player.setPos(centeredX, mc.player.getY(), centeredZ);
        anchorPos = mc.player.blockPosition();
    }

    private boolean item(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item == Items.OBSIDIAN) {
            return true;
        } else if (useEnderChest.getValue() && item == Items.ENDER_CHEST) {
            return true;
        }
        return false;
    }

    private void fuckCrystal(BlockPos targetPos) {
        for (Entity entity : mc.level.getEntities(null, new AABB(targetPos))) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
            mc.gameMode.attack(mc.player, crystal);
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            break;
        }
    }

    private PlaceTarget updatePlaceTarget(BlockPos targetPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = targetPos.relative(side);
            if (mc.level.getBlockState(neighborPos).canBeReplaced()) continue;

            Direction hitSide = side.getOpposite();
            Vec3 hitVec = new Vec3(
                    neighborPos.getX() + 0.5 + hitSide.getStepX() * 0.5,
                    neighborPos.getY() + 0.5 + hitSide.getStepY() * 0.5,
                    neighborPos.getZ() + 0.5 + hitSide.getStepZ() * 0.5
            );
            Vector2f rotationVec = RotationUtils.calculate(neighborPos, hitSide);
            return new PlaceTarget(targetPos, neighborPos, hitSide, hitVec, rotationVec);
        }

        return null;
    }

    private void tryPlace(PlaceTarget placeTarget, FindItemResult item) {
        if (!rotate.getValue()) {
            placeBlock(placeTarget, item);
            return;
        }

        final int requestPriority = Priority.High.priority;
        RotationManager.INSTANCE.applyRotation(placeTarget.rotation(), 10, requestPriority, record -> {
            if (record.selectedPriorityValue() != requestPriority) return;

            boolean b = RaytraceUtils.overBlock(record.currentRotation(), placeTarget.neighborPos(), placeTarget.side(), false);
            if (!b) {
                return;
            }

            placeBlock(placeTarget, item);
        });
    }

    private void placeBlock(PlaceTarget placeTarget, FindItemResult item) {
        if (!mc.level.getBlockState(placeTarget.targetPos()).canBeReplaced()) {
            return;
        }

        if (swapMode.is(SwapMode.InvSwitch)) {
            InvUtils.invSwap(item.slot());
        } else {
            InvUtils.swap(item.slot(), swapMode.is(SwapMode.Silent));
        }

        BlockHitResult hitResult = new BlockHitResult(placeTarget.hitVec(), placeTarget.side(), placeTarget.neighborPos(), false);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, item.getHand(), hitResult);
        if (result.consumesAction()) {
            mc.getConnection().send(new ServerboundSwingPacket(item.getHand()));
            placeTimer.reset();
        }

        if (swapMode.is(SwapMode.Silent)) {
            InvUtils.swapBack();
        } else if (swapMode.is(SwapMode.InvSwitch)) {
            InvUtils.invSwapBack();
        }
    }

    private record PlaceTarget(BlockPos targetPos, BlockPos neighborPos, Direction side, Vec3 hitVec, Vector2f rotation) {
    }

}
