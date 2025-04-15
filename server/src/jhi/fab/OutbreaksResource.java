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
import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.ViewOutbreaks.VIEW_OUTBREAKS;

@Path("/outbreaks")
public class OutbreaksResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader,
		@QueryParam("year") Integer year,
		@QueryParam("outbreakCode") String outbreakCode,
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
				// Something something give me any outbreak where any of its
				// subsamples have this variety OR the reported_variety matches
				query.where(DSL.exists(DSL.selectOne().from(SUBSAMPLES)
					.where(SUBSAMPLES.OUTBREAK_ID.eq(VIEW_OUTBREAKS.OUTBREAK_ID)).and(SUBSAMPLES.VARIETY_ID.eq(variety)))
					.or(VIEW_OUTBREAKS.REPORTED_VARIETY_ID.eq(variety)));
			}
			if (year != null)
				query.where(DSL.year(VIEW_OUTBREAKS.DATE_SUBMITTED).eq(year));
			if (outbreakCode != null)
				query.where(VIEW_OUTBREAKS.OUTBREAK_CODE.eq(outbreakCode));
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
	public synchronized Response postOutbreaks(@HeaderParam("Authorization") String authHeader, Outbreaks newOutbreak)
		throws SQLException, Exception
	{
		User user = new User(authHeader);

		// Must be logged in to create a new outbreak
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		if (newOutbreak == null)
			return Response.status(Response.Status.BAD_REQUEST).build();

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

			Outbreaks outbreak = context.insertInto(OUTBREAKS)
				.set(OUTBREAKS.OUTBREAK_CODE, code)
				.set(OUTBREAKS.USER_ID, user.getUserID())
				.set(OUTBREAKS.POSTCODE, newOutbreak.getPostcode())
				.set(OUTBREAKS.REAL_LATITUDE, newOutbreak.getRealLatitude())
				.set(OUTBREAKS.REAL_LONGITUDE, newOutbreak.getRealLongitude())
				.set(OUTBREAKS.VIEW_LATITUDE, newOutbreak.getViewLatitude())
				.set(OUTBREAKS.VIEW_LONGITUDE, newOutbreak.getViewLongitude())
				.set(OUTBREAKS.DATE_SUBMITTED, LocalDate.now())
				.set(OUTBREAKS.STATUS, OutbreaksStatus.lookupLiteral("pending"))
				.set(OUTBREAKS.SOURCE_ID, newOutbreak.getSourceId())
				.set(OUTBREAKS.SOURCE_OTHER, newOutbreak.getSourceOther())
				.set(OUTBREAKS.SEVERITY_ID, newOutbreak.getSeverityId())
				.set(OUTBREAKS.SEVERITY_OTHER, newOutbreak.getSeverityOther())
				.set(OUTBREAKS.USER_COMMENTS, newOutbreak.getUserComments())
				.returning(OUTBREAKS.fields())
				.fetchOneInto(Outbreaks.class);

			// TODO: Format a proper email
			// Now email...
			String host = System.getenv("FAB_URL");
			String message = "<p>A new outbreak has just been reported. You can view it here:</p>"
				+ "<p>&nbsp;&nbsp;&nbsp;&nbsp;<a href='" + host + "#/outbreak/" + outbreak.getOutbreakId() + "'></a></p>";

			FAB.emailAdmins("FlightAgainstBlight: New Outbreak Reported", message);

			return Response.ok(outbreak).build();
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

	@POST
	@Path("/{id}/subsamples")
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response postSubsamples(@HeaderParam("Authorization") String authHeader, List<Subsamples> subsamples)
		throws SQLException
	{
		return SubsamplesResource.postSubsamples(authHeader, subsamples);
	}

	private void map(ViewOutbreaks outbreak, User user)
	{
		if (user.isAdmin())
			return;

		// Owner doesn't get to see...
		outbreak.setIsAdmin(null);
		outbreak.setAdminComments(null);

		// And a normal user also doesn't get to see...
		if (outbreak.getUserId() != user.getUserID())
		{
			outbreak.setUserId(null);
			outbreak.setRealLatitude(outbreak.getViewLatitude());
			outbreak.setRealLongitude(outbreak.getViewLongitude());
			outbreak.setUserEmail(null);
			outbreak.setUserName(null);
			outbreak.setUserComments(null);

			// Hide the last part of the postcode
			if (outbreak.getPostcode() != null)
				outbreak.setPostcode(outbreak.getPostcode()
					.substring(0, outbreak.getPostcode().length()-3));
		}
	}
}