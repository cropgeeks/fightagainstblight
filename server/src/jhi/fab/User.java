package jhi.fab;

import java.sql.*;
import java.time.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.UserSessions.USER_SESSIONS;
import static jhi.fab.codegen.tables.Users.USERS;

class User
{
	private final static int OK = 200;
	private final static int NO_TOKEN = 10;
	private final static int TOKEN_INVALID = 20;
	private int status = NO_TOKEN;

	private int userID = -1;
	private boolean isAdmin = false;

	int getUserID() {
		return userID;
	}

	boolean isAdmin() {
		return isAdmin;
	}

	boolean isOK() {
		return status == OK;
	}

	boolean noToken() {
		return status == NO_TOKEN;
	}

	boolean tokenInvalid() {
		return status == TOKEN_INVALID;
	}

	User(String authHeader)
		throws SQLException
	{
		// Check if the Authorization header is not null and starts with "Bearer "
		if (authHeader != null && authHeader.startsWith("Bearer "))
		{
			// Extract the Bearer token from the Authorization header
			String token = authHeader.substring("Bearer ".length()).trim();

			status = TOKEN_INVALID;

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
					long expire = Long.parseLong(System.getenv("FAB_TOKEN_EXPIRE"));
					if (tokenMilli < (now-expire))
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
							userID = user.getUserId();

							// Are they an admin?
							isAdmin = user.getIsAdmin();

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
}