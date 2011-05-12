/**
 * 
 */
package me.taylorkelly.bigbrother.tablemgrs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * @author N3X15
 *
 */
public class BBUsersMySQL extends BBUsersTable {
    public final int revision = 2;
    public String toString() {
        return "BBUsers MySQL Driver r"+Integer.valueOf(revision);
    }
    
    /* (non-Javadoc)
     * @see me.taylorkelly.bigbrother.tablemgrs.DBTable#onLoad()
     */
    @Override
    protected void onLoad() {
    }
    
    /* (non-Javadoc)
     * @see me.taylorkelly.bigbrother.tablemgrs.DBTable#getCreateSyntax()
     */
    @Override
    public String getCreateSyntax() {
        return "CREATE TABLE `"+getTableName()+"` ("
        + "`id` INT NOT NULL AUTO_INCREMENT," 
        + "`name` varchar(32) NOT NULL DEFAULT 'Player'," 
        + "`watched` BOOLEAN NOT NULL DEFAULT '0',"
        + "PRIMARY KEY (`id`),"
        + "UNIQUE(`name`));"; //Engine doesn't matter, really.
    }

    @Deprecated
    // Never used
    public int getSubversion(File file) {
        try {
            Scanner scan = new Scanner(file);
            String version = scan.nextLine();
            try {
                int numVersion = Integer.parseInt(version);
                return numVersion;
            } catch (Exception e) {
                return 0;
            }
            
        } catch (FileNotFoundException e) {
            return 0;
        }
    }

    @Override
    protected String quoteColumn(String columnName) {
        return "`" + columnName + "`";
    }
}