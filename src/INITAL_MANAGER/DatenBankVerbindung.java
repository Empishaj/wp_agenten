package INITAL_MANAGER;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DatenBankVerbindung implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public Connection conn = null;
    public Statement stmt = null;
    public ResultSet rs = null;
    private MysqlDataSource mysqlDS = null;

    private String dbname = "160_wipo";
    private String dbuser = "wplagagent";
    private String url = "jdbc:mysql://localhost:3306/" + dbname;

    public DatenBankVerbindung()

    {
	mysqlDS = new MysqlDataSource();
	mysqlDS.setURL(url);
	mysqlDS.setUser(dbuser);
	mysqlDS.setPassword("Jm4rzd3gaWeg_pL9");
	mysqlDS.setCharacterEncoding("utf-8");
	mysqlDS.setAllowMultiQueries(true);
	mysqlDS.setAutoReconnectForPools(true);
	mysqlDS.setAutoReconnectForConnectionPools(true);
	mysqlDS.setAutoReconnect(true);
	mysqlDS.setInteractiveClient(true);
	mysqlDS.setIsInteractiveClient(true);
	mysqlDS.setParanoid(true);

	try {

	    conn = mysqlDS.getConnection();

	} catch (SQLException e) {

	    e.printStackTrace();
	}

    }

    public void readFromDatabase(String SQL, String aufrufer) throws SQLException {

	try {

	    conn.setClientInfo(SQL, aufrufer);

	    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

	    rs = stmt.executeQuery(SQL);
	} catch (java.sql.SQLException e) {

	    System.err.println(e);
	}

    }

    public void insertToDatabase(String SQL, String aufrufer) throws ClassNotFoundException, SQLException {

	conn.setClientInfo(SQL, aufrufer);

	stmt = conn.createStatement();

	stmt.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);

	rs = stmt.getGeneratedKeys();
	


    }

    public void updateOnDatabase(String SQL) {

	try {
	    conn.setClientInfo(SQL, "update");

	    stmt = conn.createStatement();
	    stmt.executeUpdate(SQL);
	} catch (SQLException e) {
	
	    e.printStackTrace();
	}

    }

    public String getDbname() {
	return dbname;
    }

    public void setDbname(String dbname) {
	this.dbname = dbname;
    }

    public String getDbuser() {
	return dbuser;
    }

    public void setDbuser(String dbuser) {
	this.dbuser = dbuser;
    }

}
