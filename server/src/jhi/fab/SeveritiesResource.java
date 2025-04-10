package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Severities.SEVERITIES;

@Path("/severities")
public class SeveritiesResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response varieties(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			List<Severities> list = context.selectFrom(SEVERITIES)
				.fetchInto(Severities.class);

			return Response.ok(list).build();
		}
	}
}