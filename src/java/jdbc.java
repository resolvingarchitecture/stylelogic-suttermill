import java.sql.*;

public class JDBC {
	boolean 			echo;
	private String 		addr;
	private Connection	coreConn;
	private int 		count=0;
	public	Statement	stmt;
	private final static String username = "sldsn";
	private final static String password = "sl123";

	public JDBC(String _addr, boolean _echo)
	{

		echo=_echo;
		addr=_addr;
		try
		{
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		} catch(java.lang.ClassNotFoundException ex)
			{
				System.err.print("ClassNotFoundException: ");
				System.err.println(ex.getMessage());
			}
		try
		{
			coreConn = DriverManager.getConnection(addr, username, password);
			stmt = coreConn.createStatement();
		} catch(SQLException ex)
			{
				System.err.println("SQLException: " + ex.getMessage());
			}
	}
	public JDBC(String addr)
	{
		echo=true;
		try
		{
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		} catch(java.lang.ClassNotFoundException ex)
			{
				System.err.print("ClassNotFoundException: ");
				System.err.println(ex.getMessage());
			}
		try
		{
			coreConn = DriverManager.getConnection(addr, "sa", "");
			stmt = coreConn.createStatement();
		} catch(SQLException ex)
			{
				System.err.println("SQLException: " + ex.getMessage());
			}
	}

	public Connection resetConnection()
	{
		Connection con = null;
		System.out.println("Resetting Connection JDBC:ODBC");
		try
		{
			con = DriverManager.getConnection(addr, "sldsn", "sl123");
		} catch(SQLException ex)
		{
			System.err.println("SQLException: failed to resetConeection " + ex.getMessage());
		}
		return con;
	}

	public ResultSet Query(Connection con, String qry, int SCROLL, int CONCUR) throws SQLException
	{
		Statement stmt;
		ResultSet rs;

//			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY );
		if (echo) System.out.println(qry);
		stmt = con.createStatement(SCROLL, CONCUR );
		rs = stmt.executeQuery(qry);

//		if (count++ > 10000) resetConnection();
		SQLWarning warning = stmt.getWarnings();
		if (warning != null)
		{
			System.out.println("\n---Warning---\n");
			while (warning != null)
			{
				System.out.println("Message: "
											   + warning.getMessage());
				System.out.println("SQLState: "
											   + warning.getSQLState());
				System.out.print("Vendor error code: ");
				System.out.println(warning.getErrorCode());
				System.out.println("");
				warning = warning.getNextWarning();
			}
		}
		SQLWarning warn = rs.getWarnings();
		if (warn != null)
		{
			System.out.println("\n---Warning---\n");
			while (warn != null)
			{
				System.out.println("Message: "
											   + warn.getMessage());
				System.out.println("SQLState: "
											   + warn.getSQLState());
				System.out.print("Vendor error code: ");
				System.out.println(warn.getErrorCode());
				System.out.println("");
				warn = warn.getNextWarning();
			}
		}


		return rs;
	}

	public ResultSet Query(Connection con, String qry ) throws SQLException
	{
		ResultSet rs;

//		if (count++ > 10000) resetConnection();
		if (echo) System.out.println(qry);
		rs = stmt.executeQuery(qry);

		return rs;
	}


	public int postQuery(Connection con, String qry ) throws SQLException
	{
		int rs;

//		if (count++ > 10000) resetConnection();
		if (echo) System.out.println(qry);
		rs = stmt.executeUpdate(qry);

		SQLWarning warning = stmt.getWarnings();
		if (warning != null)
		{
			System.out.println("\n---Warning---\n");
			while (warning != null)
			{
				System.out.println("Message: "+ warning.getMessage());
				System.out.println("SQLState: "+ warning.getSQLState());
				System.out.print("Vendor error code: ");
				System.out.println(warning.getErrorCode());
				System.out.println("");
				warning = warning.getNextWarning();
			}
		}

		return rs;
	}

	public Connection createConnection() throws SQLException
	{
		return (DriverManager.getConnection(addr, username, password));
	}

	public PreparedStatement createPreparedStatement(Connection con, String qry ) throws SQLException
	{
		PreparedStatement ps;

		ps = con.prepareStatement( qry );
		return ps;
	}

	public ResultSet executePreparedStatement( PreparedStatement ps ) throws SQLException
	{
		ResultSet rs;

		rs = ps.executeQuery();
		return rs;
	}
}



