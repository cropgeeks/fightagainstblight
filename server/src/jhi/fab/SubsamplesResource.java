package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.ViewSubsamples.VIEW_SUBSAMPLES;

public class SubsamplesResource
{
	@Produces(MediaType.APPLICATION_JSON)
	public static Response getSubsamples(String authHeader, Integer obId)
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
			Outbreaks ob = context.selectFrom(OUTBREAKS)
				.where(OUTBREAKS.OUTBREAK_ID.eq(obId))
				.fetchOneInto(Outbreaks.class);

			if (ob == null)
				return Response.status(Response.Status.NOT_FOUND).build();

			// Need to be either the owner or an admin to see this
			if (user.getUserID() == ob.getUserId() || user.isAdmin())
			{
				List<ViewSubsamples> results = context.selectFrom(VIEW_SUBSAMPLES)
					.where(VIEW_SUBSAMPLES.OUTBREAK_ID.eq(ob.getOutbreakId()))
					.fetchInto(ViewSubsamples.class);

				return Response.ok(results).build();
			}
			else
				return Response.status(Response.Status.FORBIDDEN).build();
		}
	}

	@Produces(MediaType.APPLICATION_JSON)
	public static Response postSubsamples(String authHeader, List<Subsamples> subsamples)
		throws SQLException
	{
		User user = new User(authHeader);

		// You must be at least logged in use this method
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		// And you must be an admin
		if (user.isAdmin() == false)
			return Response.status(Response.Status.FORBIDDEN).build();
		// And you must have passed in a list of subsamples
		if (subsamples == null)
			return Response.status(Response.Status.BAD_REQUEST).build();

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			ArrayList<Integer> keys = new ArrayList<>();
			for (Subsamples ss: subsamples)
			{
				Outbreaks ob = context.selectFrom(OUTBREAKS)
					.where(OUTBREAKS.OUTBREAK_ID.eq(ss.getOutbreakId()))
					.fetchOneInto(Outbreaks.class);

				// This shouldn't be null/notfound, but just in case
				if (ob == null)
					return Response.status(Response.Status.BAD_REQUEST).build();

				Subsamples newSS = context.insertInto(SUBSAMPLES)
					.set(SUBSAMPLES.OUTBREAK_ID, ob.getOutbreakId())
					.returning(SUBSAMPLES.fields())
					.fetchOneInto(Subsamples.class);

				keys.add(newSS.getSubsampleId());
			}

			return Response.ok(keys).build();
		}
	}
}