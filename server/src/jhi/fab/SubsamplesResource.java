package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.Path;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.ViewSubsamples.VIEW_SUBSAMPLES;

@Path("/subsamples")
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

				if (user.isAdmin() == false)
					for (ViewSubsamples ss: results)
					{
						ss.setMyceliaPellet(null);
						ss.setCultureSlope(null);
						ss.setMatingType(null);
					}

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
					System.out.println("Updating: " + ss.getSubsampleId());

					context.update(SUBSAMPLES)
						.set(SUBSAMPLES.SUBSAMPLE_CODE, ss.getSubsampleCode())
						.set(SUBSAMPLES.VARIETY_ID, ss.getVarietyId())
						.set(SUBSAMPLES.MATERIAL, ss.getMaterial())
						.set(SUBSAMPLES.GENOTYPE_ID, ss.getGenotypeId())
						.set(SUBSAMPLES.DATE_GENOTYPED, ss.getDateGenotyped())
						.set(SUBSAMPLES.ADMIN_COMMENTS, ss.getAdminComments())
						.set(SUBSAMPLES.MYCELIA_PELLET, ss.getMyceliaPellet())
						.set(SUBSAMPLES.CULTURE_SLOPE, ss.getCultureSlope())
						.set(SUBSAMPLES.MATING_TYPE, ss.getMatingType())
						.where(SUBSAMPLES.SUBSAMPLE_ID.eq(ss.getSubsampleId()))
						.execute();

					keys.add(ss.getSubsampleId());
				}
				// Otherwise, create a new one
				else
				{
					System.out.println("Adding new record: " + ss.getSubsampleCode());

					Subsamples newSS = context.insertInto(SUBSAMPLES)
						.set(SUBSAMPLES.SUBSAMPLE_CODE, ss.getSubsampleCode())
						.set(SUBSAMPLES.OUTBREAK_ID, outbreakID)
						.set(SUBSAMPLES.VARIETY_ID, ss.getVarietyId())
						.set(SUBSAMPLES.MATERIAL, ss.getMaterial())
						.set(SUBSAMPLES.GENOTYPE_ID, ss.getGenotypeId())
						.set(SUBSAMPLES.DATE_GENOTYPED, ss.getDateGenotyped())
						.set(SUBSAMPLES.ADMIN_COMMENTS, ss.getAdminComments())
						.set(SUBSAMPLES.MYCELIA_PELLET, ss.getMyceliaPellet())
						.set(SUBSAMPLES.CULTURE_SLOPE, ss.getCultureSlope())
						.set(SUBSAMPLES.MATING_TYPE, ss.getMatingType())
						.returning(SUBSAMPLES.fields())
						.fetchOneInto(Subsamples.class);

					keys.add(newSS.getSubsampleId());
				}
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
				for (Integer key: keys)
					if (dbSS.getSubsampleId().intValue() == key)
							found = true;

				if (found == false)
				{
					context.deleteFrom(SUBSAMPLES)
						.where(SUBSAMPLES.SUBSAMPLE_ID.eq(dbSS.getSubsampleId()))
						.execute();
				}
			}

			// And finally, blank any outbreaks.reported_variety_id at this time
			Integer reportedVarietyID = null;
			context.update(OUTBREAKS)
				.set(OUTBREAKS.REPORTED_VARIETY_ID, reportedVarietyID)
				.where(OUTBREAKS.OUTBREAK_ID.eq(outbreakID))
				.execute();

			return Response.ok(keys).build();
		}
	}

	@GET
	@Path("/materials")
	@Produces(MediaType.APPLICATION_JSON)
	public Response varieties(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			List<String> list = context.selectDistinct(SUBSAMPLES.MATERIAL)
				.from(SUBSAMPLES)
				.fetchInto(String.class);

			// Sort alphabetically
			Collections.sort(list);

			// Move the unknowns/others to the top of the list
			for (int i = 0; i < list.size(); i++)
			{
				String material = list.get(i);

				if (material.equalsIgnoreCase("unknown"))
				{
					list.remove(material);
					list.add(0, material);
				}
			}

			return Response.ok(list).build();
		}
	}
}