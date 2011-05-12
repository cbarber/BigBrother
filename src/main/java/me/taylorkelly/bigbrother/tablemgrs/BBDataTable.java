package me.taylorkelly.bigbrother.tablemgrs;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import me.taylorkelly.bigbrother.BBLogging;
import me.taylorkelly.bigbrother.BBSettings;
import me.taylorkelly.bigbrother.BBSettings.DBMS;
import me.taylorkelly.bigbrother.WorldManager;
import me.taylorkelly.bigbrother.datablock.BBDataBlock;
import me.taylorkelly.bigbrother.datablock.BBDataBlock.Action;
import me.taylorkelly.bigbrother.datasource.BBDB;

import org.bukkit.block.Block;

/**
 * Handler class for the bbdata table
 *
 * @author tkelly
 * @todo Handle INSERT/UPDATE/DELETEs through here
 */
public abstract class BBDataTable extends DBTable {

    private static final int VERSION = 6;
    // Singletons :D
    private static BBDataTable instance=null;

    /**
     * Get table name + prefix
     */
    protected String getActualTableName() 
    {
        return "bbdata";
    }
    
    public static BBDataTable getInstance() {
        if(instance==null) {
            //BBLogging.info("BBSettings.databaseSystem="+BBSettings.databaseSystem.toString());
            if(BBDB.usingDBMS(DBMS.MYSQL))
                instance=new BBDataMySQL();
            else if(BBDB.usingDBMS(DBMS.POSTGRES))
                instance=new BBDataPostgreSQL();
            else
                instance=new BBDataH2();
        }
        return instance;
    }
    
    public BBDataTable() {
        if(BBDB.needsUpdate(BBSettings.dataFolder, getActualTableName(), VERSION))
            drop();
        if (!tableExists()) {
            BBLogging.info("Building `"+getTableName()+"` table...");
            createTable();
        } else {
            BBLogging.debug("`"+getTableName()+"` table already exists");

        }
        
        onLoad();
    }
    
    public String getPreparedDataBlockStatement() throws SQLException {
        return "INSERT INTO " + getTableName()
                + " (date, player, action, world, x, y, z, type, data, rbacked) VALUES (?,?,?,?,?,?,?,?,?,0)";
    }

    /**
     * Cleanse by age
     * @param timeAgo
     * @param deletesPerCleansing
     * @return
     */
	public abstract String getCleanseAged(Long timeAgo, long deletesPerCleansing);

	/**
	 * Cleanse by number
	 * @param stmt
	 * @param maxRecords
	 * @param deletesPerCleansing
	 * @return
	 * @throws SQLException
	 */
	public abstract int getCleanseByLimit(Statement stmt,Long maxRecords, long deletesPerCleansing) throws SQLException;

	public ArrayList<BBDataBlock> getBlockHistory(Block block,
			WorldManager manager) {
		PreparedStatement ps = null;
        ResultSet rs = null;
        ArrayList<BBDataBlock> blockList = new ArrayList<BBDataBlock>();

        try {
                // TODO maybe more customizable actions?
            	if(BBDB.usingDBMS(DBMS.POSTGRES)) // TODO: Someone made rbacked a BOOLEAN, which screws up the logic.
            		ps = BBDB.prepare("SELECT  bbdata.id, date, player, action, x, y, z, type, data, rbacked, bbworlds.name AS world FROM " + BBDataTable.getInstance().getTableName() + " AS bbdata INNER JOIN "+BBWorldsTable.getInstance().getTableName()+" AS bbworlds ON bbworlds.id = bbdata.world  WHERE rbacked = false AND x = ? AND y = ?  AND z = ? AND bbdata.world = ? ORDER BY bbdata.id ASC;");
            	else
            		ps = BBDB.prepare("SELECT  bbdata.id, date, player, action, x, y, z, type, data, rbacked, bbworlds.name AS world FROM " + BBDataTable.getInstance().getTableName() + " AS bbdata INNER JOIN "+BBWorldsTable.getInstance().getTableName()+" AS bbworlds ON bbworlds.id = bbdata.world  WHERE rbacked = 0 AND x = ? AND y = ?  AND z = ? AND bbdata.world = ? ORDER BY bbdata.id ASC;");
                
                ps.setInt(1, block.getX());
                ps.setInt(2, block.getY());
                ps.setInt(3, block.getZ());
                ps.setInt(4, manager.getWorld(block.getWorld().getName()));
                rs = ps.executeQuery();
                BBDB.commit();
                
                while (rs.next()) {
                	
                	String data = rs.getString("data");
                    BBDataBlock newBlock = BBDataBlock.getBBDataBlock(BBUsersTable.getInstance().findById(rs.getInt("player")), Action.values()[rs.getInt("action")], rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), rs.getInt("type"), data);
                    newBlock.date = rs.getLong("date");
                    blockList.add(newBlock);
                }
        } catch (SQLException ex) {
            BBLogging.severe("Find SQL Exception", ex);
        } finally {
            BBDB.cleanup( "Find",  ps, rs );
        }
        return blockList;

	}
}
