package jhi.fab;

import java.sql.SQLException;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;

@Provider
public class SQLExceptionMapper implements ExceptionMapper<SQLException>
{
	@Override
	public Response toResponse(SQLException e)
	{
		// Log it
		e.printStackTrace();

		// Then return it
		return Response.serverError()
			.entity(e.getMessage())
			.type(MediaType.TEXT_PLAIN)
			.build();
	}
}