package me.taylorkelly.bigbrother.tablemgrs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import me.taylorkelly.bigbrother.BBLogging;
import me.taylorkelly.bigbrother.BBPlayerInfo;
import me.taylorkelly.bigbrother.BBSettings;
import me.taylorkelly.bigbrother.BBSettings.DBMS;
import me.taylorkelly.bigbrother.datasource.BBDB;

import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

/**
 * Handle the user tracking table.
 * BBUsers(_id_,name,watched)
 * @author N3X15
 * @todo Handle INSERT/SELECT/DELETE stuff through here.
 */
public abstract class BBUsersTable extends DBTable {
    
    private static final int VERSION = 6;

    private Hashtable<Integer,BBPlayerInfo> knownPlayers = new Hashtable<Integer,BBPlayerInfo>();
    private Hashtable<String,Integer> knownNames = new Hashtable<String,Integer>();
    
    public void drop() {
        BBLogging.info("Dropping table "+getTableName());
        BBDB.executeUpdate("DROP TABLE IF EXISTS "+((BBDB.usingDBMS(DBMS.H2)) ? getActualTableName() : getTableName()));
        createTable();
        invalidateCache();
    }
    
    // Singletons :D
    private static BBUsersTable instance=null;
    
    /**
     * Get table name
     */
    public String getActualTableName() 
    {
        return "bbusers";
    }

    public static BBUsersTable getInstance() {
        if(instance==null) {
            BBLogging.debug("BBDB.dbms="+BBDB.dbms.toString());
            if(BBDB.usingDBMS(DBMS.MYSQL))
                instance=new BBUsersMySQL();
            else if(BBDB.usingDBMS(DBMS.POSTGRES))
                instance=new BBUsersPostgreSQL();
            else
                instance=new BBUsersH2();
        }
        return instance;
    }
    
    protected BBUsersTable() {
        if(BBDB.needsUpdate(BBSettings.dataFolder, getActualTableName(), VERSION))
            drop();
        if (!tableExists()) {
            BBLogging.info("Building `"+getTableName()+"` table...");
            createTable();
        } else {
            BBLogging.debug("`"+getTableName()+"` table already exists");

        }

        invalidateCache();
        
        onLoad();
    }

    /**
     * UPDATE or INSERT user.
     * @param pi
     */
    public void save(BBPlayerInfo playerInfo) {
        if(playerInfo.isNew()) {
            create(playerInfo);
        } else {
            update(playerInfo);
        }
    }
    
    public void create(BBPlayerInfo playerInfo) {
        PreparedStatement ps = null;
        ResultSet generatedKeys = null;
        
        try {
            ps = BBDB.prepare("INSERT INTO "+getActualTableName()+" (name,watched) VALUES (?,?)");
            ps.setString(1,playerInfo.getName());
            ps.setBoolean(2,playerInfo.getWatched());

            BBLogging.debug(ps.toString());
            ps.executeUpdate();
            BBDB.commit();

            generatedKeys = ps.getGeneratedKeys();
            if(!generatedKeys.next()) {
                BBLogging.severe("Failed to get generated key for user `"+playerInfo.getName()+"`.");
                return;
            }
            playerInfo.setId(generatedKeys.getInt(1));

        } catch (SQLException e) {
            BBLogging.severe("Can't create the user `"+playerInfo.getName()+"`.", e);
        } finally {
            BBDB.cleanup( "BBUsersTable.create", ps, generatedKeys);
        }

        cachePlayer(playerInfo);
    }

    public void update(BBPlayerInfo playerInfo) {
        invalidateCache(playerInfo.getName());

        PreparedStatement ps = null;
        try {
            ps = BBDB.prepare("UPDATE "+getActualTableName()+" SET watched = ? WHERE id = ?");
            ps.setBoolean(1, playerInfo.getWatched());
            ps.setInt(2, playerInfo.getID());

            BBLogging.debug(ps.toString());
            ps.executeUpdate();
            BBDB.commit();
        } catch (SQLException e) {
            BBLogging.severe("Can't create the user `"+playerInfo.getName()+"`.", e);
        } finally {
            BBDB.cleanup("BBUsersTable.update", ps, null);
        }

        cachePlayer(playerInfo);
    }

    // TODO: This should move into a database specific adapter class instead of
    // duplicating across TableManager classes
    protected abstract String quoteColumn(String string);

    public BBPlayerInfo findById(int id) {
        BBPlayerInfo playerInfo = getFromCache(id);

        if(null == playerInfo) {
            playerInfo = getUserFromDB(id);
            cachePlayer(playerInfo);
        }

        return playerInfo;
    }

    public BBPlayerInfo findByName(String name) {
        BBPlayerInfo playerInfo = getFromCache(name);

        if(null == playerInfo) {
            playerInfo = getUserFromDB(name);
            cachePlayer(playerInfo);
        }

        return playerInfo;
    }

    public void userOpenedChest(String player, Chest c, ItemStack[] contents) {
        BBPlayerInfo pi = findByName(player);
        pi.setHasOpenedChest(c,contents);
        knownPlayers.put(pi.getID(),pi);
    }

    protected BBPlayerInfo getFromCache(int id) {
        if(getInstance().knownPlayers.containsKey(id)) {
            return getInstance().knownPlayers.get(id);
        }
        return null;
    }

    protected BBPlayerInfo getFromCache(String name) {
        if(getInstance().knownNames.containsKey(name)) {
            return getFromCache(getInstance().knownNames.get(name));
        }
        return null;
    }

    protected BBPlayerInfo getUserFromDB(String name) {
        try {
            return queryUserFromDb("SELECT * FROM "+getTableName()+" WHERE " + quoteColumn("name") + " = ?", name);
        } catch (SQLException e) {
            BBLogging.severe("Error trying to find the user `"+name+"`.", e);
        }
        return null;
    }

    protected BBPlayerInfo getUserFromDB(int id) {
        try {
            return queryUserFromDb("SELECT * FROM " + getTableName() + " WHERE " + quoteColumn("id") + " = ?;", id);
        } catch (SQLException e) {
            BBLogging.severe("Can't find user #"+id+".", e);
        }
        return null;
    }

    protected BBPlayerInfo queryUserFromDb(String sql, Object... params) throws SQLException {
        ResultSet rs = null;
        try {
            BBLogging.debug(sql);
            rs = BBDB.executeQuery(sql, params);
            if(!rs.next()) {
                return null;
            }

            return new BBPlayerInfo(rs.getInt("id"), rs.getString("name"), rs.getBoolean("watched"));
        } finally {
            BBDB.cleanup( "BBUsersTable.queryUserFromDb(String)", null, rs);
        }
    }

    protected void cachePlayer(BBPlayerInfo playerInfo) {
        if(null == playerInfo) {
            return;
        }

        knownPlayers.put(playerInfo.getID(), playerInfo);
        knownNames.put(playerInfo.getName(), playerInfo.getID());
    }

    protected void invalidateCache(String name) {
        if(knownNames.containsKey(name)) {
            Integer id = knownNames.get(name);

            knownPlayers.remove(id);
            knownNames.remove(name);
        }
    }

    private void invalidateCache() {
        knownPlayers.clear();
        knownNames.clear();
    }

    public BBPlayerInfo[] getWatchedPlayers() {
        try {
            return queryUsersFromDb("SELECT * FROM " + getTableName() + " WHERE watched = ?;", true);
        } catch (SQLException e) {
            BBLogging.severe("Failed to query watched players", e);
        }
        return new BBPlayerInfo[] {};
    }

    public BBPlayerInfo[] getUnwatchedPlayers() {
        try {
            return queryUsersFromDb("SELECT * FROM " + getTableName() + " WHERE watched = ?;", false);
        } catch (SQLException e) {
            BBLogging.severe("Failed to query unwatched players", e);
        }
        return new BBPlayerInfo[] {};
    }

    private BBPlayerInfo[] queryUsersFromDb(String sql, Object... params) throws SQLException {
        ArrayList<BBPlayerInfo> users = new ArrayList<BBPlayerInfo>();
        ResultSet rs = null;
        try {
            BBLogging.debug(sql);
            rs = BBDB.executeQuery(sql, params);

            while(rs.next()) {
                users.add(new BBPlayerInfo(rs.getInt("id"), rs.getString("name"), rs.getBoolean("watched")));
            }

        } finally {
            BBDB.cleanup( "BBUsersTable.queryUsersFromDb(String)", null, rs);
        }

        BBPlayerInfo[] ret = new BBPlayerInfo[users.size()];
        users.toArray(ret);
        return ret;
    }

}
