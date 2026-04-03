package com.github.epsilon.modules.impl.combat;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.*;
import com.github.epsilon.utils.math.MathUtils;
import com.github.epsilon.utils.render.Render3DUtils;
import com.github.epsilon.utils.rotation.RotationUtils;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/*
 * Author Moli
 * 平滑测试 8, 73, 180, 10, 500, 0.1, 0.25, 0.56, 3, 30, 1.2, 2
 * todo 更牛逼的反作弊绕过。
 */

public class AimAssist extends Module {

    public static final AimAssist INSTANCE = new AimAssist();

    public EnumSetting<Mode> mode = enumSetting("Mode", Mode.Smooth);
    public DoubleSetting range = doubleSetting("Range", 4.2, 1.0, 8.0, 0.1);
    public IntSetting aimStrength = intSetting("AimStrength", 30, 1, 100, 1);
    public IntSetting aimSmooth = intSetting("AimSmooth", 45, 1, 180, 1);
    public IntSetting aimTime = intSetting("AimTime", 2, 1, 10, 1);
    public BoolSetting ignoreWalls = boolSetting("IgnoreWalls", true);
    public IntSetting reactionTime = intSetting("ReactionTime", 80, 1, 500, 1);
    public BoolSetting ignoreInvisible = boolSetting("IgnoreInvis", false);
    public BoolSetting ignoreScreen = boolSetting("IgnoreScreen", true);
    public BoolSetting ignoreInventory = boolSetting("IgnoreInventory", true);
    public BoolSetting player = boolSetting("Player", true);
    public BoolSetting mob = boolSetting("Mob", true);
    public BoolSetting animal = boolSetting("Animal", true);
    public DoubleSetting humanJitter = doubleSetting("HumanJitter", 0.3, 0.0, 2.0, 0.1);
    public DoubleSetting humanOvershoot = doubleSetting("HumanOvershoot", 0.15, 0.0, 0.5, 0.05);
    public DoubleSetting inertia = doubleSetting("Inertia", 0.85, 0.0, 0.99, 0.01);
    public BoolSetting lockTarget = boolSetting("LockTarget", false);
    public BoolSetting lockedEsp = boolSetting("LockedESP", false);
    public ColorSetting lockedEspColor = colorSetting("LockedESPColor", new Color(255, 0, 0, 150));
    public BoolSetting closeTargetBoost = boolSetting("CloseTargetBoost", false);
    public DoubleSetting closeTargetBoostStrength = doubleSetting("BoostStrength", 1.5, 1.0, 3.0, 0.1);
    public DoubleSetting closeTargetThreshold = doubleSetting("CloseThreshold", 10.0, 1.0, 30.0, 1.0);
    public DoubleSetting responsiveness = doubleSetting("Responsiveness", 1.2, 1.0, 2.0, 0.1);
    public EnumSetting<PriorityMode> targetPriority = enumSetting("TargetPriority", PriorityMode.Distance);
    public BoolSetting prediction = boolSetting("Prediction", true);
    public IntSetting predictionTicks = intSetting("PredictionTicks", 2, 0, 20, 1);
    public BoolSetting extrapolateVelocity = boolSetting("ExtrapolateVelocity", true);
    public EnumSetting<AimPart> aimPart = enumSetting("AimPart", AimPart.Torso);

    private float rotationYaw, rotationPitch;
    private float angularVelocityYaw, angularVelocityPitch;
    private int aimTicks = 0;
    private long visibleTime = System.currentTimeMillis();
    private LivingEntity currentTarget;
    private LivingEntity lockedTarget;

    private float targetYaw, targetPitch;
    private float lastYaw, lastPitch;

    private float jitterYaw, jitterPitch;
    private float overshootYaw, overshootPitch;
    private int jitterTick = 0;

    private Mode previousMode = Mode.Smooth;
    private float transitionYaw, transitionPitch;
    private int transitionTicks = 0;
    private static final int TRANSITION_DURATION = 10;

    private float rawAcceleration = 0;
    private float smoothedAcceleration = 0;

    private double playerPrevX, playerPrevY, playerPrevZ;
    private double playerVelocityX, playerVelocityY, playerVelocityZ;
    private double playerAccelerationX, playerAccelerationY, playerAccelerationZ;

    private double targetPrevX, targetPrevY, targetPrevZ;
    private double targetVelocityX, targetVelocityY, targetVelocityZ;
    private double targetAccelerationX, targetAccelerationY, targetAccelerationZ;

    private float prevAngularVelYaw, prevAngularVelPitch;
    private int framesWithoutTarget = 0;
    private float espAlpha = 0f;

    private double smoothedPredictedX, smoothedPredictedY, smoothedPredictedZ;

    private static final float JITTER_UPDATE_INTERVAL = 2;
    private static final float JITTER_PITCH_RATIO = 0.6f;
    private static final float MIN_SMOOTH_FACTOR = 0.05f;
    private static final float MAX_SMOOTH_FACTOR = 0.95f;
    private static final float DYNAMIC_SMOOTH_THRESHOLD = 15.0f;
    private static final float OVERSHOOT_THRESHOLD = 20.0f;
    private static final float OVERSHOOT_DECAY = 0.85f;
    private static final float YAW_OVERSHOOT_MULTIPLIER = 0.3f;
    private static final float PITCH_OVERSHOOT_MULTIPLIER = 0.2f;
    private static final float ACCELERATION_DECAY = 0.9f;
    private static final float SMOOTHED_ACCEL_DECAY = 0.95f;
    private static final float GCD_MOUSE_SENS_MULTIPLIER = 0.6f;
    private static final float GCD_MOUSE_SENS_OFFSET = 0.2f;
    private static final float GCD_FINAL_MULTIPLIER = 1.2f;
    private static final float GCD_TOLERANCE = 0.5f;
    private static final float PARTIAL_TICK_OFFSET = 0.5f;
    private static final float INSTANT_MODE_SMOOTH = 0.8f;
    private static final float EASE_OUT_POWER = 3;
    private static final float ACCELERATION_SMOOTH_LERP = 0.2f;
    private static final float ANGULAR_VELOCITY_DAMPING = 0.92f;
    private static final float ANGULAR_ACCELERATION_DAMPING = 0.85f;
    private static final float MAX_ANGULAR_VELOCITY = 15.0f;
    private static final float CENTRIPETAL_COMPENSATION_STRENGTH = 0.35f;
    private static final float LAG_COMPENSATION_FACTOR = 0.15f;
    private static final float ESP_FADE_SPEED = 0.15f;
    private static final float PREDICTION_VELOCITY_FACTOR = 0.95f;
    private static final float PREDICTION_SMOOTH_FACTOR = 0.35f;

    private AimAssist() {
        super("AimAssist", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        previousMode = mode.getValue();
        transitionTicks = 0;
        lockedTarget = null;
        visibleTime = 0;
    }

    @Override
    protected void onDisable() {
        resetAllStates();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (shouldResetStates()) {
            resetAllStates();
            return;
        }

        if (isScreenPaused()) {
            pauseAndSlowDown();
            return;
        }

        updateAimTicks();
        if (shouldStopAiming()) {
            rawAcceleration = 0;
            return;
        }

        currentTarget = findTarget();
        updateAcceleration();
        handleModeTransition();

        updatePlayerMotion();
        updateTargetMotion();

        if (currentTarget != null) {
            processTargetAim();
            framesWithoutTarget = 0;
        } else {
            framesWithoutTarget++;
            if (framesWithoutTarget > 5) {
                resetAimStates();
            } else {
                applyAngularVelocityDamping();
            }
        }

        updateEspAlpha();
    }

    @SubscribeEvent
    public void onRenderFrame(RenderFrameEvent.Pre event) {
        if (shouldSkipRender()) return;

        if (shouldTakeControl()) {
            float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);

            if (isInTransition()) {
                applyTransition(partialTicks);
                return;
            }

            if (mode.getValue() == Mode.Smooth) {
                applySmoothRotation(partialTicks);
            } else {
                applyInstantRotation();
            }
        }
    }

    private boolean shouldResetStates() {
        return nullCheck();
    }

    private boolean isScreenPaused() {
        return (ignoreScreen.getValue() && mc.screen != null) ||
                (ignoreInventory.getValue() && (mc.screen instanceof AbstractContainerScreen));
    }

    private boolean shouldSkipRender() {
        return nullCheck() || isScreenPaused();
    }

    private boolean shouldTakeControl() {
        return currentTarget != null && !Float.isNaN(rotationYaw) && !Float.isNaN(rotationPitch);
    }

    private void updateEspAlpha() {
        boolean shouldShow = lockedEsp.getValue() && lockTarget.getValue() && lockedTarget != null && lockedTarget.isAlive();
        espAlpha = Mth.clamp(espAlpha + (shouldShow ? ESP_FADE_SPEED : -ESP_FADE_SPEED), 0f, 1f);
    }

    private double getAimPartHeight() {
        return switch (aimPart.getValue()) {
            case Head -> 1.7f;
            case Neck -> 1.5f;
            case Torso -> 1.0f;
            case Legs -> 0.7f;
            case Feet -> 0.2f;
        };
    }

    private boolean isInTransition() {
        return transitionTicks > 0;
    }

    private void resetAllStates() {
        resetAimStates();
        resetMotionStates();
        resetAccelerationStates();
        framesWithoutTarget = 0;
        espAlpha = 0f;
        lockedTarget = null;
    }

    private void pauseAndSlowDown() {
        rotationYaw = Float.NaN;
        rotationPitch = Float.NaN;
        targetYaw = Float.NaN;
        targetPitch = Float.NaN;
        angularVelocityYaw *= 0.5f;
        angularVelocityPitch *= 0.5f;
    }

    private void resetAimStates() {
        resetCommonAimStates();
        resetAngularVelocityStates();
        resetPredictionStates();
        if (!lockTarget.getValue()) {
            lockedTarget = null;
        }
    }

    private void resetCommonAimStates() {
        rotationYaw = Float.NaN;
        rotationPitch = Float.NaN;
        targetYaw = Float.NaN;
        targetPitch = Float.NaN;
        resetJitterAndOvershoot();
    }

    private void resetJitterAndOvershoot() {
        jitterYaw = 0;
        jitterPitch = 0;
        overshootYaw = 0;
        overshootPitch = 0;
    }

    private void resetAngularVelocityStates() {
        angularVelocityYaw = 0;
        angularVelocityPitch = 0;
        prevAngularVelYaw = 0;
        prevAngularVelPitch = 0;
    }

    private void resetMotionStates() {
        playerVelocityX = 0;
        playerVelocityY = 0;
        playerVelocityZ = 0;
        playerAccelerationX = 0;
        playerAccelerationY = 0;
        playerAccelerationZ = 0;
        resetTargetMotion();
    }

    private void resetPredictionStates() {
        smoothedPredictedX = 0;
        smoothedPredictedY = 0;
        smoothedPredictedZ = 0;
    }

    private void resetAccelerationStates() {
        rawAcceleration = 0;
        smoothedAcceleration = 0;
    }

    private void updatePlayerMotion() {
        double newVelX = mc.player.getX() - playerPrevX;
        double newVelY = mc.player.getY() - playerPrevY;
        double newVelZ = mc.player.getZ() - playerPrevZ;

        playerAccelerationX = newVelX - playerVelocityX;
        playerAccelerationY = newVelY - playerVelocityY;
        playerAccelerationZ = newVelZ - playerVelocityZ;

        playerVelocityX = newVelX;
        playerVelocityY = newVelY;
        playerVelocityZ = newVelZ;

        playerPrevX = mc.player.getX();
        playerPrevY = mc.player.getY();
        playerPrevZ = mc.player.getZ();
    }

    private void updateTargetMotion() {
        if (currentTarget == null) {
            resetTargetMotion();
            return;
        }

        double newVelX = currentTarget.getX() - targetPrevX;
        double newVelY = currentTarget.getY() - targetPrevY;
        double newVelZ = currentTarget.getZ() - targetPrevZ;

        targetAccelerationX = newVelX - targetVelocityX;
        targetAccelerationY = newVelY - targetVelocityY;
        targetAccelerationZ = newVelZ - targetVelocityZ;

        targetVelocityX = newVelX;
        targetVelocityY = newVelY;
        targetVelocityZ = newVelZ;

        targetPrevX = currentTarget.getX();
        targetPrevY = currentTarget.getY();
        targetPrevZ = currentTarget.getZ();
    }

    private void resetTargetMotion() {
        targetVelocityX = 0;
        targetVelocityY = 0;
        targetVelocityZ = 0;
        targetAccelerationX = 0;
        targetAccelerationY = 0;
        targetAccelerationZ = 0;
    }

    private void applyAngularVelocityDamping() {
        angularVelocityYaw *= ANGULAR_VELOCITY_DAMPING;
        angularVelocityPitch *= ANGULAR_VELOCITY_DAMPING;

        if (rotationYaw != Float.NaN && rotationPitch != Float.NaN) {
            rotationYaw = lastYaw + angularVelocityYaw;
            rotationPitch = lastPitch + angularVelocityPitch;
        }
    }

    private void updateAimTicks() {
        if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY)
            aimTicks++;
        else
            aimTicks = 0;
    }

    private boolean shouldStopAiming() {
        return aimTicks >= aimTime.getValue();
    }

    private void updateAcceleration() {
        if (currentTarget != null) {
            rawAcceleration += aimStrength.getValue() / 10000f;
            rawAcceleration = Mth.clamp(rawAcceleration, 0f, 1.0f);

            float eased = 1 - (float) Math.pow(1 - rawAcceleration, EASE_OUT_POWER);
            smoothedAcceleration = Mth.lerp(ACCELERATION_SMOOTH_LERP, smoothedAcceleration, eased);
        } else {
            rawAcceleration *= ACCELERATION_DECAY;
            smoothedAcceleration *= SMOOTHED_ACCEL_DECAY;
        }
    }

    private void handleModeTransition() {
        if (mode.getValue() != previousMode) {
            transitionTicks = TRANSITION_DURATION;
            transitionYaw = mc.player.getYRot();
            transitionPitch = mc.player.getXRot();
            previousMode = mode.getValue();
        }

        if (transitionTicks > 0) {
            transitionTicks--;
        }
    }

    private void processTargetAim() {
        if (!mc.player.hasLineOfSight(currentTarget)) {
            if (!ignoreWalls.getValue()) {
                if (visibleTime == 0) {
                    visibleTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - visibleTime > reactionTime.getValue()) {
                    resetAimStates();
                    return;
                }
            }
        } else {
            visibleTime = 0;
        }

        initializeAimIfNeeded();
        calculateIdealRotation();
        updateHumanJitter();
        calculateDynamicSmoothing();

        applyGCDFix();

        lastYaw = rotationYaw;
        lastPitch = rotationPitch;
    }

    private void initializeAimIfNeeded() {
        if (Float.isNaN(rotationYaw)) {
            rotationYaw = mc.player.getYRot();
            rotationPitch = mc.player.getXRot();
            targetYaw = rotationYaw;
            targetPitch = rotationPitch;
            lastYaw = rotationYaw;
            lastPitch = rotationPitch;
            resetJitterAndOvershoot();
            resetAngularVelocityStates();
        }
    }

    private void calculateIdealRotation() {
        if (prediction.getValue()) {
            double predictedX = currentTarget.getX();
            double predictedY = currentTarget.getY();
            double predictedZ = currentTarget.getZ();

            double predictedVelX = targetVelocityX;
            double predictedVelY = targetVelocityY;
            double predictedVelZ = targetVelocityZ;

            if (extrapolateVelocity.getValue()) {
                predictedVelX += targetAccelerationX * 0.5;
                predictedVelY += targetAccelerationY * 0.5;
                predictedVelZ += targetAccelerationZ * 0.5;
            }

            int ticks = predictionTicks.getValue();
            for (int i = 0; i < ticks; i++) {
                predictedX += predictedVelX * PREDICTION_VELOCITY_FACTOR;
                predictedY += predictedVelY * PREDICTION_VELOCITY_FACTOR;
                predictedZ += predictedVelZ * PREDICTION_VELOCITY_FACTOR;
            }

            if (smoothedPredictedX == 0 && smoothedPredictedY == 0 && smoothedPredictedZ == 0) {
                smoothedPredictedX = predictedX;
                smoothedPredictedY = predictedY;
                smoothedPredictedZ = predictedZ;
            }

            smoothedPredictedX = Mth.lerp(PREDICTION_SMOOTH_FACTOR, smoothedPredictedX, predictedX);
            smoothedPredictedY = Mth.lerp(PREDICTION_SMOOTH_FACTOR, smoothedPredictedY, predictedY);
            smoothedPredictedZ = Mth.lerp(PREDICTION_SMOOTH_FACTOR, smoothedPredictedZ, predictedZ);

            double dx = smoothedPredictedX - mc.player.getX();
            double targetHeight = getAimPartHeight();
            double dy = smoothedPredictedY + targetHeight - (mc.player.getY() + mc.player.getEyeHeight());
            double dz = smoothedPredictedZ - mc.player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90f);
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

            targetYaw = yaw;
            targetPitch = pitch;
        } else {
            double dx = currentTarget.getX() - mc.player.getX();
            double dy = currentTarget.getY() + getAimPartHeight() - (mc.player.getY() + mc.player.getEyeHeight());
            double dz = currentTarget.getZ() - mc.player.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90f);
            targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

            smoothedPredictedX = 0;
            smoothedPredictedY = 0;
            smoothedPredictedZ = 0;
        }
    }

    private void updateHumanJitter() {
        jitterTick++;
        if (jitterTick >= JITTER_UPDATE_INTERVAL) {
            jitterTick = 0;
            float jitterAmount = humanJitter.getValue().floatValue();
            jitterYaw = MathUtils.getRandom(-jitterAmount, jitterAmount);
            jitterPitch = MathUtils.getRandom(-jitterAmount * JITTER_PITCH_RATIO, jitterAmount * JITTER_PITCH_RATIO);
        }
    }

    private void calculateDynamicSmoothing() {
        float jitteredTargetYaw = targetYaw + jitterYaw;
        float jitteredTargetPitch = targetPitch + jitterPitch;

        float smoothFactor = aimSmooth.getValue() / 180.0f;
        smoothFactor = Mth.clamp(smoothFactor, MIN_SMOOTH_FACTOR, MAX_SMOOTH_FACTOR);

        float yawDiff = Mth.wrapDegrees(jitteredTargetYaw - rotationYaw);
        float pitchDiff = jitteredTargetPitch - rotationPitch;
        float distanceToTarget = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        float dynamicSmoothFactor = smoothFactor;
        if (distanceToTarget < DYNAMIC_SMOOTH_THRESHOLD) {
            dynamicSmoothFactor = smoothFactor * (0.3f + (distanceToTarget / DYNAMIC_SMOOTH_THRESHOLD) * 0.7f);
        }

        float overshootAmount = humanOvershoot.getValue().floatValue();
        if (distanceToTarget > OVERSHOOT_THRESHOLD) {
            overshootYaw = yawDiff * overshootAmount * YAW_OVERSHOOT_MULTIPLIER;
            overshootPitch = pitchDiff * overshootAmount * PITCH_OVERSHOOT_MULTIPLIER;
        } else {
            overshootYaw *= OVERSHOOT_DECAY;
            overshootPitch *= OVERSHOOT_DECAY;
        }

        float adjustedYawDiff = yawDiff + overshootYaw;
        float adjustedPitchDiff = pitchDiff + overshootPitch;

        float desiredAngularVelYaw = adjustedYawDiff * dynamicSmoothFactor;
        float desiredAngularVelPitch = adjustedPitchDiff * dynamicSmoothFactor;

        if (closeTargetBoost.getValue()) {
            float angleDistance = Mth.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            float threshold = closeTargetThreshold.getValue().floatValue();
            if (angleDistance < threshold) {
                float boostFactor = closeTargetBoostStrength.getValue().floatValue();
                float t = 1f - angleDistance / threshold;
                float smoothT = t * t * (3f - 2f * t);
                float boost = 1f + (boostFactor - 1f) * smoothT;
                desiredAngularVelYaw *= boost;
                desiredAngularVelPitch *= boost;
            }
        }

        float centripetalYaw = calculateCentripetalCompensationYaw();
        float centripetalPitch = calculateCentripetalCompensationPitch();

        desiredAngularVelYaw += centripetalYaw;
        desiredAngularVelPitch += centripetalPitch;

        float lagCompYaw = prevAngularVelYaw * LAG_COMPENSATION_FACTOR;
        float lagCompPitch = prevAngularVelPitch * LAG_COMPENSATION_FACTOR;
        desiredAngularVelYaw += lagCompYaw;
        desiredAngularVelPitch += lagCompPitch;

        float responsiveFactor = responsiveness.getValue().floatValue();
        desiredAngularVelYaw *= responsiveFactor;
        desiredAngularVelPitch *= responsiveFactor;

        float inertiaFactor = inertia.getValue().floatValue();
        angularVelocityYaw = angularVelocityYaw * inertiaFactor + desiredAngularVelYaw * (1 - inertiaFactor);
        angularVelocityPitch = angularVelocityPitch * inertiaFactor + desiredAngularVelPitch * (1 - inertiaFactor);

        float angularAccelYaw = angularVelocityYaw - prevAngularVelYaw;
        float angularAccelPitch = angularVelocityPitch - prevAngularVelPitch;
        angularAccelYaw *= ANGULAR_ACCELERATION_DAMPING;
        angularAccelPitch *= ANGULAR_ACCELERATION_DAMPING;
        angularVelocityYaw = prevAngularVelYaw + angularAccelYaw;
        angularVelocityPitch = prevAngularVelPitch + angularAccelPitch;

        angularVelocityYaw = Mth.clamp(angularVelocityYaw, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);
        angularVelocityPitch = Mth.clamp(angularVelocityPitch, -MAX_ANGULAR_VELOCITY, MAX_ANGULAR_VELOCITY);

        prevAngularVelYaw = angularVelocityYaw;
        prevAngularVelPitch = angularVelocityPitch;

        rotationYaw = lastYaw + angularVelocityYaw;
        rotationPitch = lastPitch + angularVelocityPitch;
    }

    private float calculateCentripetalCompensationYaw() {
        if (currentTarget == null) return 0;

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        double targetX = currentTarget.getX();
        double targetZ = currentTarget.getZ();

        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1) return 0;

        double relVelX = targetVelocityX - playerVelocityX;
        double relVelZ = targetVelocityZ - playerVelocityZ;
        double tangentialSpeed = (relVelX * dz - relVelZ * dx) / distance;

        double centripetalAccel = tangentialSpeed * tangentialSpeed / distance;

        double compensation = centripetalAccel * CENTRIPETAL_COMPENSATION_STRENGTH;

        return (float) (compensation * 0.5);
    }

    private float calculateCentripetalCompensationPitch() {
        if (currentTarget == null) return 0;

        double playerY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        double targetY = currentTarget.getY() + currentTarget.getEyeHeight(currentTarget.getPose());

        double dy = targetY - playerY;
        double horizontalDist = mc.player.distanceTo(currentTarget);
        if (horizontalDist < 1) return 0;

        double relVelY = targetVelocityY - playerVelocityY;
        double verticalSpeed = relVelY;

        double centripetalAccel = verticalSpeed * verticalSpeed / horizontalDist;
        double compensation = centripetalAccel * CENTRIPETAL_COMPENSATION_STRENGTH * 0.3;

        return (float) compensation;
    }

    private void applyGCDFix() {
        double sensitivity = Mth.clamp(mc.options.sensitivity().get(), 0.0, 1.0);
        double gcdFix = (Math.pow(sensitivity * GCD_MOUSE_SENS_MULTIPLIER + GCD_MOUSE_SENS_OFFSET, 3.0)) * GCD_FINAL_MULTIPLIER;
        gcdFix = Mth.clamp(gcdFix, 0.0, 100.0);
        rotationYaw = applySmartGCDFix(rotationYaw, lastYaw, gcdFix);
        rotationPitch = applySmartGCDFix(rotationPitch, lastPitch, gcdFix);
        rotationPitch = Mth.clamp(rotationPitch, -90.0f, 90.0f);
    }

    private void applyTransition(float partialTicks) {
        float t = 1 - (transitionTicks - partialTicks) / (float) TRANSITION_DURATION;
        t = Mth.clamp(t, 0f, 1f);
        t = t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;

        float yaw = Mth.lerp(t, transitionYaw, rotationYaw);
        float pitch = Mth.lerp(t, transitionPitch, rotationPitch);

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    private void applySmoothRotation(float partialTicks) {
        float yawDiff = Mth.wrapDegrees(rotationYaw - mc.player.getYRot());
        float pitchDiff = rotationPitch - mc.player.getXRot();

        float smoothAmount = smoothedAcceleration;

        float lerpFactor = Mth.clamp(smoothAmount * (PARTIAL_TICK_OFFSET + partialTicks * PARTIAL_TICK_OFFSET), 0.0f, 1.0f);

        float interpolatedYaw = mc.player.getYRot() + yawDiff * lerpFactor;
        float interpolatedPitch = mc.player.getXRot() + pitchDiff * lerpFactor;

        mc.player.setYRot(interpolatedYaw);
        mc.player.setXRot(interpolatedPitch);
    }

    private void applyInstantRotation() {
        float yawDiff = Mth.wrapDegrees(rotationYaw - mc.player.getYRot());
        float pitchDiff = rotationPitch - mc.player.getXRot();

        float interpolatedYaw = mc.player.getYRot() + yawDiff * INSTANT_MODE_SMOOTH;
        float interpolatedPitch = mc.player.getXRot() + pitchDiff * INSTANT_MODE_SMOOTH;

        mc.player.setYRot(interpolatedYaw);
        mc.player.setXRot(interpolatedPitch);
    }

    private float applySmartGCDFix(float target, float current, double gcd) {
        if (gcd <= 0) return target;

        double diff = target - current;
        double absDiff = Math.abs(diff);

        if (absDiff < gcd * GCD_TOLERANCE) {
            return target;
        }

        double roundedDiff = Math.round(diff / gcd) * gcd;
        return (float) (current + roundedDiff);
    }

    private LivingEntity findTarget() {
        if (lockTarget.getValue() && lockedTarget != null) {
            if (lockedTarget.isAlive() && mc.player.distanceTo(lockedTarget) <= range.getValue()) {
                if (ignoreWalls.getValue() || mc.player.hasLineOfSight(lockedTarget)) {
                    return lockedTarget;
                } else {
                    lockedTarget = null;
                }
            } else {
                lockedTarget = null;
            }
        }

        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!isValidAimTarget(living)) continue;
            candidates.add(living);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> switch (targetPriority.getValue()) {
            case Distance -> Double.compare(
                    mc.player.distanceTo(a),
                    mc.player.distanceTo(b)
            );
            case Health -> Float.compare(
                    a.getHealth(),
                    b.getHealth()
            );
            case Angle -> {
                float yawA = Mth.wrapDegrees(RotationUtils.getRotationsToEntity(a).x - mc.player.getYRot());
                float yawB = Mth.wrapDegrees(RotationUtils.getRotationsToEntity(b).x - mc.player.getYRot());
                yield Float.compare(Math.abs(yawA), Math.abs(yawB));
            }
        });

        LivingEntity target = candidates.getFirst();

        if (lockTarget.getValue() && target != null) {
            lockedTarget = target;
        }

        return target;
    }

    private boolean isValidAimTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isDeadOrDying()) return false;
        if (AntiBot.INSTANCE.isBot(entity)) return false;

        double dist = mc.player.distanceTo(entity);
        if (dist > range.getValue()) return false;

        if (entity instanceof Player) {
            if (!player.getValue()) return false;
            if (entity.isInvisible() && !ignoreInvisible.getValue()) return false;
        } else if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
            return false;
        } else if (entity instanceof net.minecraft.world.entity.animal.Animal) {
            if (!animal.getValue()) return false;
        } else if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            if (!mob.getValue()) return false;
        } else {
            return false;
        }

        return true;
    }

    @SubscribeEvent
    public void onRender3D(RenderLevelStageEvent.AfterLevel event) {
        if (nullCheck()) return;
        if (!lockedEsp.getValue()) return;
        if (!lockTarget.getValue() || lockedTarget == null) return;
        if (!lockedTarget.isAlive()) return;
        if (espAlpha <= 0.01f) return;

        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        double x = lockedTarget.xo + (lockedTarget.getX() - lockedTarget.xo) * partialTicks;
        double y = lockedTarget.yo + (lockedTarget.getY() - lockedTarget.yo) * partialTicks;
        double z = lockedTarget.zo + (lockedTarget.getZ() - lockedTarget.zo) * partialTicks;

        AABB interpolatedBox = lockedTarget.getBoundingBox()
                .move(x - lockedTarget.getX(), y - lockedTarget.getY(), z - lockedTarget.getZ());

        Color originalColor = lockedEspColor.getValue();
        int alpha = (int) (originalColor.getAlpha() * espAlpha);
        Color fadedColor = new Color(originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), alpha);

        Render3DUtils.drawFilledBox(interpolatedBox, fadedColor);
    }

    public enum Mode {
        Smooth,
        Instant
    }

    public enum PriorityMode {
        Distance,
        Health,
        Angle
    }

    public enum AimPart {
        Head,
        Neck,
        Torso,
        Legs,
        Feet
    }

    public boolean shouldBlockMouse() {
        if (!isEnabled()) return false;
        if (mode.getValue() != Mode.Smooth) return false;
        return !Float.isNaN(rotationYaw) && !Float.isNaN(rotationPitch);
    }

}
