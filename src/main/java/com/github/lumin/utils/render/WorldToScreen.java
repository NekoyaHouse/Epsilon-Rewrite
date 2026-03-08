package com.github.lumin.utils.render;

import com.github.lumin.mixins.IGameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4d;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.List;

public final class WorldToScreen {

    private static final Minecraft mc = Minecraft.getInstance();

    public static Vector4d getEntityPositionsOn2D(LivingEntity target, float tickDelta) {
        int[] viewport = new int[]{0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()};
        Matrix4f projectionMatrix = createProjectionMatrix(tickDelta);

        Vec3 position = interpolate(target, tickDelta);

        float width = target.getBbWidth() / 2f;
        float height = target.getBbHeight() + (target.isCrouching() ? 0.1f : 0.2f);

        AABB boundingBox = new AABB(
                position.x - width,
                position.y,
                position.z - width,
                position.x + width,
                position.y + height,
                position.z + width
        );

        Vector4d projection = projectEntity(viewport, projectionMatrix, boundingBox);

        projection.div(mc.getWindow().getGuiScale());

        projection.z -= projection.x;
        projection.w -= projection.y;

        return projection;
    }

    public static Vector4d projectEntity(final int[] viewport, final Matrix4f matrix, final AABB boundingBox) {
        final Vector4f windowCoords = new Vector4f();

        final List<Vec3> list = getBoxBounds(boundingBox);
        Vector4d projected = null;

        for (final Vec3 pos : list) {
            matrix.project((float) pos.x, (float) pos.y, (float) pos.z, viewport, windowCoords);
            windowCoords.y = viewport[3] - windowCoords.y;

            if (windowCoords.w != 1) {
                break;
            }

            if (projected == null) {
                projected = new Vector4d(windowCoords.x, windowCoords.y, 0, 0);
            } else {
                final double windowX = windowCoords.x;
                final double windowY = windowCoords.y;

                projected.x = Math.min(windowX, projected.x);
                projected.y = Math.min(windowY, projected.y);
                projected.z = Math.max(windowX, projected.z);
                projected.w = Math.max(windowY, projected.w);
            }
        }

        return projected;
    }

    public static Matrix4f createProjectionMatrix(final float tickDelta) {
        IGameRenderer gameRendererAccessor = (IGameRenderer) mc.gameRenderer;
        Matrix4fStack matrixStack = new Matrix4fStack();
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();

        float fov = gameRendererAccessor.callGetFov(camera, tickDelta, true);

        matrixStack.mul(mc.gameRenderer.getProjectionMatrix(fov));

        matrixStack.rotateX((float) Math.toRadians(camera.xRot()));
        matrixStack.rotateY((float) Math.toRadians(camera.yRot() + 180.0f));

        return matrixStack;
    }

    public static List<Vec3> getBoxBounds(final AABB boundingBox) {
        return Arrays.asList(
                new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ),
                new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ),
                new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ),
                new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ),
                new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ),
                new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ),
                new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
        );
    }

    public static Vec3 interpolate(final LivingEntity entity, final float tickDelta) {
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        return new Vec3(
                Mth.lerp(tickDelta, entity.xOld, entity.getX()) - camera.position().x,
                Mth.lerp(tickDelta, entity.yOld, entity.getY()) - camera.position().y,
                Mth.lerp(tickDelta, entity.zOld, entity.getZ()) - camera.position().z
        );
    }

    public static Vector4d getHeadPositionOn2D(LivingEntity target, float tickDelta) {
        int[] viewport = new int[]{0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight()};
        Matrix4f projectionMatrix = createProjectionMatrix(tickDelta);

        Vec3 position = interpolate(target, tickDelta);
        Vec3 headPos = position.add(0.0, target.getBbHeight() + 0.35, 0.0);

        final Vector4f windowCoords = new Vector4f();
        projectionMatrix.project((float) headPos.x, (float) headPos.y, (float) headPos.z, viewport, windowCoords);
        windowCoords.y = viewport[3] - windowCoords.y;

        double guiScale = mc.getWindow().getGuiScale();
        return new Vector4d(windowCoords.x / guiScale, windowCoords.y / guiScale, 0, 0);
    }

}
