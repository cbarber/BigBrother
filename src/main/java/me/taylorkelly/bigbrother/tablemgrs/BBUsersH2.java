package me.taylorkelly.bigbrother.tablemgrs;

public class BBUsersH2 extends BBUsersTable {
    public final int revision = 2;
    public String toString() {
        return "BBUsers H2 Driver r"+Integer.valueOf(revision);
    }
    
    @Override
    protected void onLoad() {
    }
    
    @Override
    public String getCreateSyntax() {
        return "CREATE TABLE IF NOT EXISTS `" + getActualTableName() + "` (" 
        + "`id` INT AUTO_INCREMENT PRIMARY KEY," 
        + "`name` varchar(32) NOT NULL DEFAULT 'Player'," 
        + "`watched` BOOLEAN NOT NULL DEFAULT '0'); "
        + "CREATE UNIQUE INDEX IF NOT EXISTS idxUsername ON `" + getActualTableName() + "` (`name`)"; // ANSI
    }

    @Override
    protected String quoteColumn(String columnName) {
        return "`" + columnName + "`";
    }
}
