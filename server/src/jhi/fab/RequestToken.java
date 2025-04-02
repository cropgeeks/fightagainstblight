package jhi.fab;

import java.sql.*;

import jakarta.ws.rs.core.*;
import java.time.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.UserSessions.USER_SESSIONS;
import static jhi.fab.codegen.tables.Users.USERS;

class RequestToken
{
	private static int OK = 200;
	private static int UNAUTHORIZED = 401;
	private static int FORBIDDEN = 403;
	private int status = UNAUTHORIZED;

	private int userID;
	private String token;

	int getUserID() {
		return userID;
	}

	RequestToken(String authHeader)
		throws SQLException
	{
		// Check if the Authorization header is not null and starts with "Bearer "
		if (authHeader != null && authHeader.startsWith("Bearer "))
		{
			// Extract the Bearer token from the Authorization header
			token = authHeader.substring("Bearer ".length()).trim();

			// Does this token exist?
			try (Connection conn = DatabaseUtils.getConnection())
			{
				DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
				UserSessions session = context.selectFrom(USER_SESSIONS)
					.where(USER_SESSIONS.TOKEN.eq(token))
					.fetchOneInto(UserSessions.class);

				if (session != null)
				{
					System.out.println("Found session: " + session.getToken());

					LocalDateTime tokenTime = session.getCreatedOn();
					long tokenMilli = tokenTime.toInstant(ZoneOffset.UTC).toEpochMilli();
					long now = System.currentTimeMillis();

					// Has the token expired?
					// TODO: How and when will we clean up expired tokens?
					if (tokenMilli < (now-(30*60*1000)))
						System.out.println("Token has expired");

					else
					{
						// If not, map to a user and reset the time on it
						Users user = context.selectFrom(USERS)
							.where(USERS.USER_ID.eq(session.getUserId()))
							.fetchOneInto(Users.class);

						// The user *should* exist, but just in case...
						if (user != null)
						{
							System.out.println("Token linked to " + user.getEmail());

							context.update(USER_SESSIONS)
								.set(USER_SESSIONS.CREATED_ON, LocalDateTime.now())
								.where(USER_SESSIONS.TOKEN.eq(token))
								.execute();

							status = OK;
						}
					}
				}
			}
		}
	}

	boolean isOK() {
		return status == OK;
	}

	Response returnResponse()
	{
		if (status == UNAUTHORIZED)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		else if (status == FORBIDDEN)
			return Response.status(Response.Status.FORBIDDEN).build();

		return Response.status(Response.Status.OK).build();
	}
}