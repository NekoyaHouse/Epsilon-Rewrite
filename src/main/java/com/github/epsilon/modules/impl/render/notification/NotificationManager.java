package com.github.epsilon.modules.impl.render.notification;

import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {

    public static final NotificationManager INSTANCE = new NotificationManager();

    private NotificationManager() {
    }

    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    public void post(String title, String subTitle, NotificationMode mode, long time) {
        notifications.add(new Notification(title, subTitle, mode, time));
    }

    public void update(float speed, float frameTime) {
        for (Notification n : notifications) {
            n.update(speed, frameTime);
            if (n.isRequestedToRemove && n.getAlpha() < 0.001f) {
                notifications.remove(n);
            }
        }
    }

    public CopyOnWriteArrayList<Notification> getNotifications() {
        return notifications;
    }

}