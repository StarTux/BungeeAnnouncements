package com.winthier.announcements;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class AnnouncementsPlugin extends Plugin implements Listener
{
    final Set<UUID> waitingOnJoin = Collections.synchronizedSet(new HashSet<>());
    final Set<UUID> waitingOnLeave = Collections.synchronizedSet(new HashSet<>());
    
    @Override public void onEnable()
    {
        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onPostLoginEvent(PostLoginEvent event)
    {
        final UUID uuid = event.getPlayer().getUniqueId();
        if (waitingOnLeave.remove(uuid) || waitingOnJoin.remove(uuid)) return;
        waitingOnJoin.add(uuid);
        final String name = event.getPlayer().getName();
        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override public void run() {
                if (!waitingOnJoin.remove(uuid)) return;
                broadcast("Join", uuid, name);
            }
        }, 750, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event)
    {
        final UUID uuid = event.getPlayer().getUniqueId();
        if (waitingOnLeave.remove(uuid) || waitingOnJoin.remove(uuid)) return;
        waitingOnLeave.add(uuid);
        final String name = event.getPlayer().getName();
        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override public void run() {
                if (!waitingOnLeave.remove(uuid)) return;
                broadcast("Leave", uuid, name);
            }
        }, 750, TimeUnit.MILLISECONDS);
    }

    void broadcast(String msg, UUID uuid, String name) {
        for (ServerInfo serverInfo: getProxy().getServers().values()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            try {
                out.writeUTF(msg);
                out.writeUTF(uuid.toString());
                out.writeUTF(name);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return;
            }
            boolean result = serverInfo.sendData("LogChannel", bos.toByteArray(), false);
            // System.out.println("Sent "+msg+" "+uuid+" to "+serverInfo.getName()+": " + result)
        }
    }
}
