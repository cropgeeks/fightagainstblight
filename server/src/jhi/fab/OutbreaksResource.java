package jhi.fab;

import java.io.*;
import java.nio.charset.*;
import java.sql.*;
import java.text.*;
import java.time.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.Path;

import org.jooq.*;
import org.jooq.impl.*;
import static org.jooq.impl.DSL.field;

import jhi.fab.codegen.enums.*;
import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.ViewOutbreaks.VIEW_OUTBREAKS;
import static jhi.fab.codegen.tables.ViewSubsamples.VIEW_SUBSAMPLES;

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
		@QueryParam("variety") Integer variety,
		@QueryParam("userId") Integer userId,
		@QueryParam("outcode") String outcode)
		throws SQLException
	{
		// You don't *need* a token for this call, but if we have one (and a
		// valid user) we'll use it to fill out a more detailed response
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			var query = context.selectFrom(VIEW_OUTBREAKS);

			// We always want to filter by a year, even if the UI didn't provide
			if (year == null)
				year = LocalDate.now().getYear();
			query.where(DSL.year(VIEW_OUTBREAKS.DATE_SUBMITTED).eq(year));

			if (variety != null) {
				// Something something give me any outbreak where any of its
				// subsamples have this variety OR the reported_variety matches
				query.where(DSL.exists(DSL.selectOne().from(SUBSAMPLES)
					.where(SUBSAMPLES.OUTBREAK_ID.eq(VIEW_OUTBREAKS.OUTBREAK_ID)).and(SUBSAMPLES.VARIETY_ID.eq(variety)))
					.or(VIEW_OUTBREAKS.REPORTED_VARIETY_ID.eq(variety)));
			}

			if (outcode != null)
				query.where(VIEW_OUTBREAKS.OUTCODE.eq(outcode));
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
			if (userId != null)
				query.where(VIEW_OUTBREAKS.USER_ID.eq(userId));


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
		throws SQLException
	{
		User user = new User(authHeader);

		// Must be logged in to create a new outbreak
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		if (newOutbreak == null || newOutbreak.getUserId() == null)
			return Response.status(Response.Status.BAD_REQUEST).build();

		// Admins can override the userId but anyone else who posts a different
		// userId from their own isn't allowed
		if (user.isAdmin() == false && newOutbreak.getUserId() != user.getUserID())
			return Response.status(Response.Status.BAD_REQUEST).build();

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

			// Ensure we always have a value for isPublic
			if (newOutbreak.getIsPublic() == null)
				newOutbreak.setIsPublic(false);

			// And format (FABYY_0000ID) where YY is 25 in the case of 2025
			String code = "FAB"
				+ ("" + LocalDate.now().getYear()).substring(2) + "_"
				+ new DecimalFormat("000000").format(nextID[0]+1);

			Outbreaks outbreak = context.insertInto(OUTBREAKS)
				.set(OUTBREAKS.OUTBREAK_CODE, code)
				.set(OUTBREAKS.USER_ID, newOutbreak.getUserId())
				.set(OUTBREAKS.POSTCODE, newOutbreak.getPostcode())
				.set(OUTBREAKS.OUTCODE, newOutbreak.getOutcode())
				.set(OUTBREAKS.COUNTRY, newOutbreak.getCountry())
				.set(OUTBREAKS.ITL_NUTS, newOutbreak.getItlNuts())
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
				.set(OUTBREAKS.ADMIN_COMMENTS, newOutbreak.getAdminComments())
				.set(OUTBREAKS.IS_PUBLIC, newOutbreak.getIsPublic())
				.returning(OUTBREAKS.fields())
				.fetchOneInto(Outbreaks.class);

			emailOutbreak(outbreak.getOutbreakId());

			return Response.ok(outbreak).build();
		}
	}

	@PATCH
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public synchronized Response patchOutbreaks(@HeaderParam("Authorization") String authHeader, @PathParam("id") int id, Outbreaks outbreak)
		throws SQLException
	{
		User user = new User(authHeader);

		// Must be logged in to update an outbreak
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		// And an admin
		if (user.isAdmin() == false)
			return Response.status(Response.Status.FORBIDDEN).build();
		if (outbreak == null)
			return Response.status(Response.Status.BAD_REQUEST).build();

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			context.update(OUTBREAKS)
				.set(OUTBREAKS.USER_ID, user.getUserID())
				.set(OUTBREAKS.DATE_RECEIVED, outbreak.getDateReceived())
				.set(OUTBREAKS.STATUS, outbreak.getStatus())
				.set(OUTBREAKS.USER_COMMENTS, outbreak.getAdminComments())
				.where(OUTBREAKS.OUTBREAK_ID.eq(outbreak.getOutbreakId()))
				.execute();

			emailOutbreak(outbreak.getOutbreakId());

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
	public synchronized Response postSubsamples(@HeaderParam("Authorization") String authHeader, @PathParam("id") int id, List<Subsamples> subsamples)
		throws SQLException
	{
		return SubsamplesResource.postSubsamples(authHeader, id, subsamples);
	}

	@GET
	@Path("/{id}/notify")
	@Produces(MediaType.APPLICATION_JSON)
	public Response notify(@HeaderParam("Authorization") String authHeader, @PathParam("id") int id)
		throws SQLException
	{
		User user = new User(authHeader);

		// Must be logged in
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		// And an admin
		if (user.isAdmin() == false)
			return Response.status(Response.Status.FORBIDDEN).build();

		emailOutbreak(id);

		return Response.ok().build();
	}

	private void map(ViewOutbreaks outbreak, User user)
	{
		if (user.isAdmin())
			return;

		// Owner doesn't get to see...
		outbreak.setIsAdmin(null);
		outbreak.setAdminComments(null);

		// A normal user also doesn't get to see...
		if (outbreak.getUserId() != user.getUserID())
		{
			outbreak.setUserId(null);
			outbreak.setUserEmail(null);
			outbreak.setUserName(null);
			outbreak.setUserComments(null);

			// And these fields get less detailed
			if (outbreak.getIsPublic() == false)
			{
				outbreak.setRealLatitude(outbreak.getViewLatitude());
				outbreak.setRealLongitude(outbreak.getViewLongitude());
				outbreak.setPostcode(outbreak.getOutcode());
			}
		}
	}

	static void emailOutbreak(int obId)
		throws SQLException
	{
		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			ViewOutbreaks viewOB = context.selectFrom(VIEW_OUTBREAKS)
				.where(VIEW_OUTBREAKS.OUTBREAK_ID.eq(obId))
				.fetchOneInto(ViewOutbreaks.class);

			List<ViewSubsamples> subsamples = context.selectFrom(VIEW_SUBSAMPLES)
				.where(VIEW_SUBSAMPLES.OUTBREAK_ID.eq(obId))
				.fetchInto(ViewSubsamples.class);

			// Now email...
			String host = System.getenv("FAB_URL");
			String link = host + "/#/outbreak/" + obId;
			String message = "<p>An outbreak has been reported or updated:</p>"
				+ "<p><table>"
				+ "<tr><td><b>Reported: </b></td><td>" + viewOB.getDateSubmitted() + "</td><tr>"
				+ "<tr><td><b>Code: </b></td><td>" + viewOB.getOutbreakCode() + "</td><tr>";

			if (viewOB.getReportedVarietyId() != null)
				message += "<tr><td><b>Variety: </b></td><td>" + viewOB.getReportedVarietyName() + "</td><tr>";

			message += "<tr><td><b>User: </b></td><td>" + viewOB.getUserName() + "</td><tr>"
				+ "<tr><td><b>Postcode: </b></td><td>" + viewOB.getPostcode() + "</td><tr>"
				+ "<tr><td><b>Location: </b></td><td>" + viewOB.getItlNuts() + ", " + viewOB.getCountry() + "</td><tr>"
				+ "<tr><td><b>Latitude: </b></td><td>" + viewOB.getRealLatitude() + "</td><tr>"
				+ "<tr><td><b>Longitude: </b></td><td>" + viewOB.getRealLongitude() + "</td><tr>"
				+ "<tr><td><b>Severity: </b></td><td>" + viewOB.getSeverityName() + "</td><tr>"
				+ "<tr><td><b>Source: </b></td><td>" + viewOB.getSourceName() + "</td><tr>"
				+ "</table></p>";

			if (subsamples.isEmpty())
				message += "<p>No subsample information is available for this outbreak yet.</p>";
			else
			{
				message += "<p>Information on the subsamples is as follows:</p>"
					+ "<p><table><tr>"
					+ "<th align=left>Subsample</th>"
					+ "<th align=left>Variety</th>"
					+ "<th align=left>Material</th>"
					+ "<th align=left>Genotype</th>"
					+ "<th align=left>Date Genotyped</th>"
					+ "</tr>";

				for (ViewSubsamples ss: subsamples)
				{
					message += "<tr>"
						+ "<td>" + ss.getSubsampleCode() + "</td>"
						+ "<td>" + ss.getVarietyName() + "</td>"
						+ "<td>" + ss.getMaterial() + "</td>"
						+ "<td>" + ss.getGenotypeName() + "</td>"
						+ "<td>" + ss.getDateGenotyped() + "</td>"
						+ "</tr>";
				}

				message += "</table></p>";
			}

			message += "<p>You can view full details at: "
				+ "<a href='" + link + "'>" + link + "</a></p>"
				+ "<p>Thanks for using Fight Against Blight and helping with "
				+ "research into blight populations around the UK.</p>";

			FAB.email(viewOB.getUserEmail(), true, "Fight Against Blight: Outbreak Reported/Updated", message, null);
		}
	}

	@GET
	@Path("/csv")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCSV(@HeaderParam("Authorization") String authHeader,
		@QueryParam("year") Integer year,
		@QueryParam("outbreakCode") String outbreakCode,
		@QueryParam("status") String status,
		@QueryParam("source") Integer source,
		@QueryParam("severity") Integer severity,
		@QueryParam("variety") Integer variety,
		@QueryParam("userId") Integer userId,
		@QueryParam("outcode") String outcode)
		throws SQLException, Exception
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
		BufferedWriter out = new BufferedWriter(writer);

		out.write("testing");
		out.close();

		return Response.ok(baos.toByteArray())
			.header("Content-Disposition", "attachment; filename=\"EuroBlight.csv\"")
			.build();
	}
}