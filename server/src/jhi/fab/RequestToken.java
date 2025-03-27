package jhi.fab;

import jakarta.ws.rs.core.*;

class RequestToken
{
	private static int OK = 200;
	private static int UNAUTHORIZED = 401;
	private static int FORBIDDEN = 403;
	private int status = OK;

	private int userID;
	private String token;

	int getUserID() {
		return userID;
	}

	RequestToken(String authHeader)
	{
		// Check if the Authorization header is not null and starts with "Bearer "
		if (authHeader != null && authHeader.startsWith("Bearer "))
		{
			// Extract the Bearer token from the Authorization header
			token = authHeader.substring("Bearer ".length()).trim();

			// TODO: Check token expiry
			// TODO: Map token to a UserID
		}
		else
		{
			status = UNAUTHORIZED;
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