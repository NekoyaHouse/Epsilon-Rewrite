package dev.sakura.server.impl.handler.implemention;

import dev.sakura.server.impl.IRCServer;
import dev.sakura.server.impl.interfaces.Connection;
import dev.sakura.server.impl.interfaces.PacketHandler;
import dev.sakura.server.impl.storage.UserRepository;
import dev.sakura.server.impl.user.User;
import dev.sakura.server.impl.user.UserManager;
import dev.sakura.server.packet.implemention.c2s.CloudConfigC2S;
import dev.sakura.server.packet.implemention.s2c.CloudConfigS2C;
import org.tinylog.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CloudConfigHandler implements PacketHandler<CloudConfigC2S> {
    @Override
    public void handle(CloudConfigC2S packet, Connection connection, UserManager userManager, User user) {
        if (user == null) {
            connection.disconnect("请先登录或注册");
            return;
        }

        String action = packet.getAction() == null ? "" : packet.getAction().trim().toLowerCase(Locale.ROOT);
        String username = user.getUsername();
        UserRepository repo = IRCServer.getInstance().getUserRepository();
        Logger.info("CloudConfig: action={} user={} name={} owner={}", action, username, packet.getName(), packet.getOwner());

        if (action.equals("list")) {
            handleList(repo, username, connection);
            return;
        }

        if (action.equals("get")) {
            handleGet(repo, username, packet, connection);
            return;
        }

        if (action.equals("upload")) {
            handleUpload(repo, username, packet, connection);
            return;
        }

        if (action.equals("delete")) {
            handleDelete(repo, username, packet, connection);
            return;
        }

        connection.sendPacket(new CloudConfigS2C(action, false, username, packet.getName(), "", Collections.emptyList(), 0, "未知操作"));
    }

    private static void handleList(UserRepository repo, String username, Connection connection) {
        try (java.sql.Connection db = IRCServer.getInstance().getDatabase().openConnection()) {
            int max = repo.getMaxCloudConfigs(db, username);
            List<String> names = repo.listCloudConfigNames(db, username);
            connection.sendPacket(new CloudConfigS2C("list", true, username, "", "", names, max, ""));
        } catch (Exception e) {
            connection.sendPacket(new CloudConfigS2C("list", false, username, "", "", Collections.emptyList(), 0, "获取列表失败"));
        }
    }

    private static void handleGet(UserRepository repo, String username, CloudConfigC2S packet, Connection connection) {
        String owner = packet.getOwner() == null ? "" : packet.getOwner().trim();
        if (owner.isEmpty()) {
            owner = username;
        }
        String name = packet.getName() == null ? "" : packet.getName().trim();
        if (name.isEmpty()) {
            connection.sendPacket(new CloudConfigS2C("get", false, owner, name, "", Collections.emptyList(), 0, "配置名不能为空"));
            return;
        }

        try (java.sql.Connection db = IRCServer.getInstance().getDatabase().openConnection()) {
            String content = repo.getCloudConfigContent(db, owner, name);
            boolean ok = content != null;
            connection.sendPacket(new CloudConfigS2C("get", ok, owner, name, ok ? content : "", Collections.emptyList(), 0, ok ? "" : "配置不存在"));
        } catch (Exception e) {
            connection.sendPacket(new CloudConfigS2C("get", false, owner, name, "", Collections.emptyList(), 0, "读取失败"));
        }
    }

    private static void handleUpload(UserRepository repo, String username, CloudConfigC2S packet, Connection connection) {
        String name = packet.getName() == null ? "" : packet.getName().trim();
        if (name.isEmpty()) {
            connection.sendPacket(new CloudConfigS2C("upload", false, username, name, "", Collections.emptyList(), 0, "配置名不能为空"));
            return;
        }

        String content = packet.getContent() == null ? "" : packet.getContent();
        try (java.sql.Connection db = IRCServer.getInstance().getDatabase().openConnection()) {
            db.setAutoCommit(false);
            try {
                if (repo.cloudConfigExists(db, username, name)) {
                    boolean ok = repo.updateCloudConfig(db, username, name, content);
                    db.commit();
                    connection.sendPacket(new CloudConfigS2C("upload", ok, username, name, "", Collections.emptyList(), 0, ok ? "" : "更新失败"));
                    return;
                }

                int max = repo.getMaxCloudConfigs(db, username);
                int count = repo.countCloudConfigs(db, username);
                if (count >= max) {
                    db.rollback();
                    connection.sendPacket(new CloudConfigS2C("upload", false, username, name, "", Collections.emptyList(), max, "已达到最大数量"));
                    return;
                }

                boolean ok = repo.insertCloudConfig(db, username, name, content);
                if (!ok) {
                    db.rollback();
                    connection.sendPacket(new CloudConfigS2C("upload", false, username, name, "", Collections.emptyList(), max, "写入失败"));
                    return;
                }

                db.commit();
                connection.sendPacket(new CloudConfigS2C("upload", true, username, name, "", Collections.emptyList(), max, ""));
            } catch (Exception e) {
                db.rollback();
                connection.sendPacket(new CloudConfigS2C("upload", false, username, name, "", Collections.emptyList(), 0, "写入失败"));
            }
        } catch (Exception e) {
            connection.sendPacket(new CloudConfigS2C("upload", false, username, name, "", Collections.emptyList(), 0, "写入失败"));
        }
    }

    private static void handleDelete(UserRepository repo, String username, CloudConfigC2S packet, Connection connection) {
        String owner = packet.getOwner() == null ? "" : packet.getOwner().trim();
        if (owner.isEmpty()) {
            owner = username;
        }
        String name = packet.getName() == null ? "" : packet.getName().trim();
        if (name.isEmpty()) {
            connection.sendPacket(new CloudConfigS2C("delete", false, owner, name, "", Collections.emptyList(), 0, "配置名不能为空"));
            return;
        }

        if (!owner.equals(username)) {
            connection.sendPacket(new CloudConfigS2C("delete", false, owner, name, "", Collections.emptyList(), 0, "只能删除自己的配置"));
            return;
        }

        try (java.sql.Connection db = IRCServer.getInstance().getDatabase().openConnection()) {
            boolean ok = repo.deleteCloudConfig(db, owner, name);
            connection.sendPacket(new CloudConfigS2C("delete", ok, owner, name, "", Collections.emptyList(), 0, ok ? "" : "配置不存在"));
        } catch (Exception e) {
            connection.sendPacket(new CloudConfigS2C("delete", false, owner, name, "", Collections.emptyList(), 0, "删除失败"));
        }
    }
}
