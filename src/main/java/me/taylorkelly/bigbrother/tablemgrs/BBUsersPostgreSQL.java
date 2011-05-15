package me.taylorkelly.bigbrother.tablemgrs;

public class BBUsersPostgreSQL extends BBUsersMySQL {
    /* (non-Javadoc)
     * @see me.taylorkelly.bigbrother.tablemgrs.DBTable#getCreateSyntax()
     */
    @Override
    public String getCreateSyntax() {
    	return "CREATE TABLE \""+getTableName()+"\" ("
			+ "\"id\" SERIAL,"
			+ "\"name\" varchar(32) NOT NULL DEFAULT 'Player', "
                        + "\"watched\" BOOLEAN NOT NULL DEFAULT '0', "
			+ "PRIMARY KEY (\"id\"),"
			+ "UNIQUE(\"name\"));";
    }

    @Override
    protected String quoteColumn(String columnName) {
        return "\"" + columnName + "\"";
    }
}