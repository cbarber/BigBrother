package me.taylorkelly.bigbrother;

import me.taylorkelly.bigbrother.tablemgrs.BBUsersTable;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public class Watcher {

    private Server server;

    public Watcher(Server server) {
        this.server = server;
    }

    public boolean watching(Player player) {
        return BBUsersTable.getInstance().findByName(player.getName()).getWatched();
    }

    public boolean toggleWatch(String player) {
        BBPlayerInfo pi = BBUsersTable.getInstance().findByName(player);

        if(null == pi) {
            return false;
        }
        
        pi.setWatched(!pi.getWatched());
        return pi.getWatched();
    }

    public String getWatchedPlayers() {
        StringBuilder list = new StringBuilder();
        BBPlayerInfo[] watchedPlayers = BBUsersTable.getInstance().getWatchedPlayers();

        for (int i = 0; i < watchedPlayers.length; ++i) {
            if (i > 0) {
                list.append(", ");
            }
            list.append(watchedPlayers[i].getName());
        }
        return list.toString();
    }

    public String getUnwatchedPlayers() {
        StringBuilder list = new StringBuilder();
        BBPlayerInfo[] watchedPlayers = BBUsersTable.getInstance().getUnwatchedPlayers();

        for (int i = 0; i < watchedPlayers.length; ++i) {
            if (i > 0) {
                list.append(", ");
            }
            list.append(watchedPlayers[i].getName());
        }
        return list.toString();
    }

    public boolean haveSeen(Player player) {
        return null != BBUsersTable.getInstance().findByName(player.getName());
    }

    public void watchPlayer(Player player) {
        BBPlayerInfo playerInfo = BBPlayerInfo.findOrCreateByName(player.getName());
        playerInfo.setWatched(true);
        playerInfo.save();
    }

    public void unwatchPlayer(Player player) {
        BBPlayerInfo playerInfo = BBPlayerInfo.findOrCreateByName(player.getName());
        playerInfo.setWatched(false);
        playerInfo.save();
    }

}
