package jhi.fab;

import java.sql.*;

import jakarta.servlet.*;

import org.apache.commons.dbcp2.*;

public class DatabaseUtils
{
	private static BasicDataSource ds;

	public static void init(ServletContext context)
	{
		String username = System.getenv("FAB_USERNAME");
		String password = System.getenv("FAB_PASSWORD");
		String url = System.getenv("FAB_DATABASE");

		if (ds == null)
		{
			ds = new BasicDataSource();
			ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
			ds.setUsername(username);
			ds.setPassword(password);
			ds.setUrl(url);
		}
	}

	public static Connection getConnection()
		throws SQLException
	{
		return ds.getConnection();
	}

	public static void close()
	{
		try
		{
			ds.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}