package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.ViewSubsamples.VIEW_SUBSAMPLES;

public class SubsamplesResource
{
	@Produces(MediaType.APPLICATION_JSON)
	public static Response getSubsamples(String authHeader, Integer outbreakID)
		throws SQLException
	{
		User user = new User(authHeader);

		// You must be at least logged in to try this method
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			// First up, query the outbreak itself so we can see who "owns" it
			Outbreaks outbreak = context.select()
				.from(OUTBREAKS)
				.where(OUTBREAKS.OUTBREAK_ID.eq(outbreakID))
				.fetchOneInto(Outbreaks.class);

			if (outbreak == null)
				return Response.status(Response.Status.NOT_FOUND).build();

			// Need to be either the owner or an admin to see this
			if (user.getUserID() == outbreak.getUserId() || user.isAdmin())
			{
				List<ViewSubsamples> results = context.selectFrom(VIEW_SUBSAMPLES)
					.where(VIEW_SUBSAMPLES.OUTBREAK_ID.eq(outbreak.getOutbreakId()))
					.fetchInto(ViewSubsamples.class);

				return Response.ok(results).build();
			}
			else
				return Response.status(Response.Status.FORBIDDEN).build();
		}
	}
}