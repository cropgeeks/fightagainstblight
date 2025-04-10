package jhi.fab;

import java.sql.*;
import java.text.*;
import java.time.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;
import static org.jooq.impl.DSL.field;

import jhi.fab.codegen.enums.*;
import jhi.fab.codegen.tables.pojos.ViewOutbreaks;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.Varieties.VARIETIES;
import static jhi.fab.codegen.tables.ViewOutbreaks.VIEW_OUTBREAKS;

@Path("/outbreaks")
public class OutbreaksResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader,
		@QueryParam("year") Integer year,
		@QueryParam("status") String status,
		@QueryParam("source") Integer source,
		@QueryParam("severity") Integer severity,
		@QueryParam("variety") Integer variety)
		throws SQLException
	{
		// You don't *need* a token for this call, but if we have one (and a
		// valid user) we'll use it to fill out a more detailed response
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			var query = context.selectFrom(VIEW_OUTBREAKS);

			if (variety != null) {
				// Something something give me any outbreak where any of its subsamples have this variety
				query.where(DSL.exists(DSL.selectOne().from(SUBSAMPLES).where(SUBSAMPLES.OUTBREAK_ID.eq(VIEW_OUTBREAKS.OUTBREAK_ID)).and(VARIETIES.VARIETY_ID.eq(variety))));
			}
			if (year != null)
				query.where(DSL.year(VIEW_OUTBREAKS.DATE_SUBMITTED).eq(year));
			if (status != null)
			{
				try { query.where(VIEW_OUTBREAKS.STATUS.eq(ViewOutbreaksStatus.lookupLiteral(status))); }
				catch (Exception e) { return Response.status(Response.Status.BAD_REQUEST).build(); }
			}
			if (source != null)
				query.where(VIEW_OUTBREAKS.SOURCE_ID.eq(source));
			if (severity != null)
				query.where(VIEW_OUTBREAKS.SEVERITY_ID.eq(severity));


			List<ViewOutbreaks> results = query.fetchInto(ViewOutbreaks.class);
			for (ViewOutbreaks outbreak: results)
				map(outbreak, user);

			System.out.println("Returning " + results.size() + " results");

			return Response.ok(results).build();
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
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			ViewOutbreaks result = context.selectFrom(VIEW_OUTBREAKS)
				.where(VIEW_OUTBREAKS.OUTBREAK_ID.eq(id))
				.fetchOneInto(ViewOutbreaks.class);

			if (result != null)
			{
				map(result, user);
				return Response.ok(result).build();
			}

			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("/{id}/subsamples")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSubsamples(@HeaderParam("Authorization") String authHeader, @PathParam("id") int id)
		throws SQLException
	{
		return SubsamplesResource.getSubsamples(authHeader, id);
	}

	@GET
	@Path("/years")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSubsamples(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			List<Integer> list = context.selectDistinct(field("EXTRACT(YEAR FROM date_submitted)"))
				.from(OUTBREAKS)
				.groupBy(field("EXTRACT(YEAR FROM date_submitted)"))
				.fetchInto(Integer.class);

			return Response.ok(list).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response postOutbreaks(@HeaderParam("Authorization") String authHeader,
		@QueryParam("latitude") Double latitude,
		@QueryParam("longitude") Double longitude,
		@QueryParam("source") Integer source,
		@QueryParam("sourceOther") String sourceOther,
		@QueryParam("severity") Integer severity,
		@QueryParam("severityOther") String severityOther,
		@QueryParam("comments") String comments,
		@QueryParam("additionalInfo") String additionalInfo,
		@QueryParam("variety") Integer variety)
		throws SQLException
	{
		User user = new User(authHeader);

		// Must be logged in to create a new outbreak
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();

		System.out.println("UserID is " + user.getUserID());

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			// Work out what the next ID code will be
			int[] nextID = {0};
			context.selectFrom(OUTBREAKS)
				.forEach(outbreakRecord -> {
					int code = Integer.parseInt(outbreakRecord.getOutbreakCode().substring(7));
					nextID[0] = Math.max(code, nextID[0]);
				});

			// And format (FABYY_0000ID) where YY is 25 in the case of 2025
			String code = "FAB"
				+ ("" + LocalDate.now().getYear()).substring(2) + "_"
				+ new DecimalFormat("000000").format(nextID[0]+1);


			context.insertInto(OUTBREAKS)
				.set(OUTBREAKS.OUTBREAK_CODE, code)
				.set(OUTBREAKS.USER_ID, user.getUserID())
				.set(OUTBREAKS.REAL_LATITUDE, latitude)
				.set(OUTBREAKS.REAL_LONGITUDE, longitude)
				.set(OUTBREAKS.DATE_SUBMITTED, LocalDate.now())
				.set(OUTBREAKS.STATUS, OutbreaksStatus.lookupLiteral("pending"))
				.set(OUTBREAKS.SOURCE_ID, source)
				.set(OUTBREAKS.SOURCE_OTHER, sourceOther)
				.set(OUTBREAKS.SEVERITY_ID, severity)
				.set(OUTBREAKS.SEVERITY_OTHER, severityOther)
				.set(OUTBREAKS.COMMENTS, comments)
				.set(OUTBREAKS.ADDITIONAL_INFO, additionalInfo)
				.execute();

			// TODO: Create viewLat/Long
			// TODO: Create associated subsamples entries at the same time?

			// TODO: Probably want to wrap the outbreak ID up for return so the
			// UI can then jump to the new page for it
			return Response.ok().build();
		}
	}

	private void map(ViewOutbreaks outbreak, User user)
	{
		if (user.isAdmin())
			return;

		outbreak.setIsAdmin(null);

		if (outbreak.getUserId() != user.getUserID())
		{
			outbreak.setUserId(null);
			outbreak.setRealLatitude(outbreak.getViewLatitude());
			outbreak.setRealLongitude(outbreak.getViewLongitude());
			outbreak.setUserEmail(null);
			outbreak.setUserName(null);
		}
	}
}