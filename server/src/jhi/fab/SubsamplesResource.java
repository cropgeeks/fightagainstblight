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
	public static Response postSubsamples(String authHeader, int outbreakID, List<Subsamples> subsamples)
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
				// Does the passed-in subsample already have a database ID?
				// If so, update the record
				if (ss.getSubsampleId() != null)
				{
					Subsamples newSS = context.update(SUBSAMPLES)
						.set(SUBSAMPLES.SUBSAMPLE_CODE, ss.getSubsampleCode())
						.set(SUBSAMPLES.VARIETY_ID, ss.getVarietyId())
						.set(SUBSAMPLES.MATERIAL, ss.getMaterial())
						.set(SUBSAMPLES.DATE_GENOTYPED, ss.getDateGenotyped())
						.set(SUBSAMPLES.USER_COMMENTS, ss.getUserComments())
						.set(SUBSAMPLES.ADMIN_COMMENTS, ss.getAdminComments())
						.where(SUBSAMPLES.SUBSAMPLE_ID.eq(ss.getSubsampleId()))
						.returning(SUBSAMPLES.fields())
						.fetchOneInto(Subsamples.class);

					keys.add(newSS.getSubsampleId());
				}
				// Otherwise, create a new one
				else
				{
					Subsamples newSS = context.insertInto(SUBSAMPLES)
						.set(SUBSAMPLES.SUBSAMPLE_CODE, ss.getSubsampleCode())
						.set(SUBSAMPLES.OUTBREAK_ID, outbreakID)
						.set(SUBSAMPLES.VARIETY_ID, ss.getVarietyId())
						.set(SUBSAMPLES.MATERIAL, ss.getMaterial())
						.set(SUBSAMPLES.DATE_GENOTYPED, ss.getDateGenotyped())
						.set(SUBSAMPLES.USER_COMMENTS, ss.getUserComments())
						.set(SUBSAMPLES.ADMIN_COMMENTS, ss.getAdminComments())
						.returning(SUBSAMPLES.fields())
						.fetchOneInto(Subsamples.class);

					keys.add(newSS.getSubsampleId());
				}

				// Blank any reported_variety_id at this time
				Integer reportedVarietyID = null;
				context.update(OUTBREAKS)
					.set(OUTBREAKS.REPORTED_VARIETY_ID, reportedVarietyID)
					.where(OUTBREAKS.OUTBREAK_ID.eq(outbreakID))
					.execute();
			}

			// Let's now get a list of all subsamples for this outbreak that are
			// in the database, and remove any that weren't posted because that
			// means the user has deleted them in the UI
			List<Subsamples> allSubsamples = context.selectFrom(SUBSAMPLES)
				.where(SUBSAMPLES.OUTBREAK_ID.eq(outbreakID))
				.fetchInto(Subsamples.class);

			// Loop over this list and DELETE the missing ones
			for (Subsamples dbSS: allSubsamples)
			{
				// Match against the POSTED list
				boolean found = false;
				for (Subsamples ss: subsamples)
					if (dbSS.getSubsampleId().intValue() == ss.getSubsampleId().intValue())
						found = true;

				if (found == false)
				{
					context.deleteFrom(SUBSAMPLES)
						.where(SUBSAMPLES.SUBSAMPLE_ID.eq(dbSS.getSubsampleId()))
						.execute();
				}
			}

			return Response.ok(keys).build();
		}
	}
}