package com.github.epsilon.modules.impl.render.notification;

import net.minecraft.util.Mth;

public class Notification {

    private final NotificationMode mode;
    private final String title, subTitle;
    private final long displayTime;
    private final long startTime;

    public boolean isRequestedToRemove;
    private float animationFactor = 0f;

    public Notification(String title, String subTitle, NotificationMode mode, long displayTime) {
        this.title = title;
        this.subTitle = subTitle;
        this.mode = mode;
        this.displayTime = displayTime;
        this.startTime = System.currentTimeMillis();
    }

    public void update(float speed, float frameTime) {
        if (System.currentTimeMillis() - startTime > displayTime) {
            isRequestedToRemove = true;
        }
        float target = isRequestedToRemove ? 0.0f : 1.0f;
        animationFactor = Mth.lerp(speed * frameTime, animationFactor, target);
    }

    public float getAlpha() {
        return animationFactor;
    }

    public NotificationMode getMode() {
        return mode;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getTitle() {
        return title;
    }

}