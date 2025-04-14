package jhi.fab;

import java.sql.*;
import java.time.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.UserSessions.USER_SESSIONS;
import static jhi.fab.codegen.tables.Users.USERS;

@Path("/users")
public class UserSessionsResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsers(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		// You must be at least logged in use this method
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		// And you must be an admin
		if (user.isAdmin() == false)
			return Response.status(Response.Status.FORBIDDEN).build();

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			List<Users> results = context.selectFrom(USERS)
				.fetchInto(Users.class);

			return Response.ok(results).build();
		}
	}

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			Users dbUser = context.selectFrom(USERS)
				.where(USERS.USER_ID.eq(user.getUserID()))
				.fetchOneInto(Users.class);

			if (dbUser != null)
				return Response.ok(dbUser).build();
			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(@QueryParam("email") String email)
		throws Exception, SQLException
	{
		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			Users user = context.selectFrom(USERS)
				.where(USERS.EMAIL.eq(email))
				.fetchOneInto(Users.class);

			// If the user exists, create and push a new token for them
			if (user != null)
			{
				UUID uuid = UUID.randomUUID();
				System.out.println("Created " + uuid + " for " + email);

				context.insertInto(USER_SESSIONS)
					.set(USER_SESSIONS.USER_ID, user.getUserId())
					.set(USER_SESSIONS.TOKEN, uuid.toString())
					.set(USER_SESSIONS.CREATED_ON, LocalDateTime.now())
					.execute();

				// Now email...
				String host = System.getenv("FAB_URL");
				String message = "<p>To log in to Flight Against Blight, please click the link below:</p>"
					+ "<p>&nbsp;&nbsp;&nbsp;&nbsp;<a href='" + host + "?token=" + uuid.toString() + "'>Login</a></p>";

				FAB.email(user.getEmail(), "Login to FlightAgainstBlight", message);
			}

			// We'll always return OK, even when the user isn't found as the UI
			// should just be informing the user that *if* their address was
			// found, they'll be receiving an email
			return Response.ok().build();
		}
	}
}