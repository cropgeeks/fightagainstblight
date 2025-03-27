package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;
import static jhi.fab.codegen.Tables.OUTBREAKS;
import jhi.fab.codegen.tables.pojos.*;

@Path("/outbreaks")
public class OutbreaksResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		// You don't *need* a token for this call, but if we have one (and a
		// valid user) we'll use it to fill out a more detailed response
		RequestToken token = new RequestToken(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			List<Outbreaks> list = context.selectFrom(OUTBREAKS)
				.fetchInto(Outbreaks.class);

			return Response.ok(list).build();
		}
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader, @PathParam("id") int id)
		throws SQLException
	{
		// You don't *need* a token for this call, but if we have one (and a
		// valid user) we'll use it to fill out a more detailed response
		RequestToken token = new RequestToken(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			Outbreaks outbreak = context.selectFrom(OUTBREAKS)
				.where(OUTBREAKS.OUTBREAK_ID.eq(id))
				.fetchOneInto(Outbreaks.class);

			if (outbreak != null)
				return Response.ok(outbreak).build();
			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}
}