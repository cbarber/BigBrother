/**
 * 
 */
package me.taylorkelly.bigbrother;

import me.taylorkelly.bigbrother.tablemgrs.BBUsersTable;

import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

/**
 * @author N3X15
 * 
 */
public class BBPlayerInfo {

    public static BBPlayerInfo ENVIRONMENT;
    
    /**
     * Are we waiting for this guy to do something after he opens a chest? (Workaround for lack of inventory update events)
     */
    private ItemStack[] chestContents=null;
    
    private String name = "";
    private int id = -1;
    private boolean watched;

    private Chest myOpenChest=null;
    
    /**
     * For caching a new player.
     * 
     * @param name
     */
    public BBPlayerInfo(String name) {
        this.name = name;

        if (BBSettings.autoWatch) {
            setWatched(true);
        }
    }

    public static BBPlayerInfo findOrCreateByName(String name) {
        BBUsersTable table = BBUsersTable.getInstance();

        BBPlayerInfo playerInfo = table.findByName(name);

        if(null == playerInfo) {
            playerInfo = new BBPlayerInfo(name);
            table.create(playerInfo);

            BBLogging.debug("New user: " + name + " -> #" + playerInfo.getID());
        }

        return playerInfo;
    }
    
    /**
     * For bringing in a user from the database.
     * @param id
     * @param name
     * @param flags
     */
    public BBPlayerInfo(int id, String name, boolean watched) {
        this.id = id;
        this.name = name;
        this.watched = watched;
    }
    
    void save() {
        BBUsersTable.getInstance().save(this);
    }
    
    /**
     * Reload from the database.
     */
    public void refresh() {
        BBPlayerInfo clone;
        BBLogging.debug("BBPlayerInfo.refresh(): "+name+"#"+Integer.valueOf(id));

        clone = BBUsersTable.getInstance().findById(id);
        
        this.id = clone.id;
        this.watched = clone.watched;
        this.name = clone.name;
    }

    public int getID() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setId(int value) {
        this.id = value;
    }

    /**
     * @param b
     */
    public void setWatched(boolean b) {
        this.watched = b;
    }
    
    /**
     * Are we tracking this user?
     * @return
     */
    public boolean getWatched() {
        return this.watched;
    }
    
    public boolean isNew() {
        return id <= -1;
    }

    /**
     * Set true when user has opened a chest.
     * Set false when they move/do stuff that can only be done outside of inventory.
     * @param b
     */
    public void setHasOpenedChest(Chest c,ItemStack[] contents) {
        myOpenChest=c;
        
        if (contents!=null)	{
        chestContents = new ItemStack[contents.length];
        	for(int i = 0;i<contents.length;i++)	{
        		if(contents[i]==null)
        		{
        			chestContents[i] = null;
        		}
        		else
        		{
        			//primitive cloning  - I can't figure, how to get Data field as well (ag)
        			chestContents[i] = new ItemStack(contents[i].getTypeId(),contents[i].getAmount(),contents[i].getDurability());
        		}
        	}
        } else {
        	chestContents = null;
        }
    }
    
    /**
     * True if the user is most likely messing around with their chest inventory.
     * @return
     */
    public boolean hasOpenedChest() {
        return chestContents!=null;
    }
    
    /**
     * Format username, colorize if necessary
     */
    public String toString() {
        String player=this.getName();
        /* TODO: Future consideration, working to get this hunk of bugs out the door atm. - N3X
        if(BBSettings.colorPlayerNames) {
            player=BBPermissions.getPrefix(player)+player+BBPermissions.getSuffix(player);
        }
        */
        return player;
    }

    public ItemStack[] getOldChestContents() {
        if(chestContents==null) {
            BBLogging.severe("getOldChestContents is about to return a null.  Please report this.");
        }
        return chestContents;
    }

    public Chest getOpenedChest() {
        return myOpenChest;
    }
}
