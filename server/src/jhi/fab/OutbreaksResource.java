package jhi.fab;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.enums.*;
import jhi.fab.dto.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Severities.SEVERITIES;
import static jhi.fab.codegen.tables.Sources.SOURCES;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;

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
//			List<Outbreaks> list = context.selectFrom(OUTBREAKS)
//				.fetchInto(Outbreaks.class);

			var query = context.selectDistinct(OUTBREAKS.fields())
				.select(SEVERITIES.fields())
				.select(SOURCES.fields())
				.from(OUTBREAKS)
				.leftJoin(SUBSAMPLES).on(SUBSAMPLES.OUTBREAK_ID.eq(OUTBREAKS.OUTBREAK_ID))
				.leftJoin(SEVERITIES).on(OUTBREAKS.SEVERITY_ID.eq(SEVERITIES.SEVERITY_ID))
				.leftJoin(SOURCES).on(OUTBREAKS.SOURCE_ID.eq(SOURCES.SOURCE_ID));

			if (year != null)
				query.where(DSL.year(OUTBREAKS.DATE_SUBMITTED).eq(year));
			if (status != null)
				query.where(OUTBREAKS.STATUS.eq(OutbreaksStatus.lookupLiteral(status)));
			if (source != null)
				query.where(OUTBREAKS.SOURCE_ID.eq(source));
			if (severity != null)
				query.where(OUTBREAKS.SEVERITY_ID.eq(severity));
			if (variety != null)
				query.where(SUBSAMPLES.VARIETY_ID.eq(variety));

			query.groupBy(OUTBREAKS.OUTBREAK_ID);

			List<OutbreaksDTO> results = query.fetch()
				.stream()
				.map(record -> {
					return mapDTO(record, user);
				})
				.collect(Collectors.toList());

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
//			Outbreaks outbreak = context.selectFrom(OUTBREAKS)
//				.where(OUTBREAKS.OUTBREAK_ID.eq(id))
//				.fetchOneInto(Outbreaks.class);

			org.jooq.Record record = context.select()
				.from(OUTBREAKS)
				.leftJoin(SEVERITIES).on(OUTBREAKS.SEVERITY_ID.eq(SEVERITIES.SEVERITY_ID))
				.leftJoin(SOURCES).on(OUTBREAKS.SOURCE_ID.eq(SOURCES.SOURCE_ID))
				.where(OUTBREAKS.OUTBREAK_ID.eq(id))
				.fetchOne();

			if (record != null)
				return Response.ok(mapDTO(record, user)).build();

			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response postOutbreaks(@HeaderParam("Authorization") String authHeader,
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

			context.insertInto(OUTBREAKS)
				.set(OUTBREAKS.OUTBREAK_CODE, "FAB-NEW")
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

			// TODO: Work out outbreak code as an increment from the last entry
			// TODO: Create viewLat/Long
			// TODO: Create associated subsamples entries at the same time?

			// TODO: Probably want to wrap the outbreak ID up for return so the
			// UI can then jump to the new page for it
			return Response.ok().build();
		}
	}

	OutbreaksDTO mapDTO(org.jooq.Record record, User user)
	{
		OutbreaksDTO dto = new OutbreaksDTO();
		dto.setOutbreakId(record.get(OUTBREAKS.OUTBREAK_ID));
		dto.setOutbreakCode(record.get(OUTBREAKS.OUTBREAK_CODE));
		dto.setViewLongitude(record.get(OUTBREAKS.VIEW_LATITUDE));
		dto.setViewLatitude(record.get(OUTBREAKS.VIEW_LONGITUDE));
		dto.setDateSubmitted(record.get(OUTBREAKS.DATE_SUBMITTED));
		dto.setDateReceived(record.get(OUTBREAKS.DATE_RECEIVED));
		dto.setSeverityId(record.get(SEVERITIES.SEVERITY_ID));
		dto.setSeverityName(record.get(SEVERITIES.SEVERITY_NAME));
		dto.setSourceId(record.get(SOURCES.SOURCE_ID));
		dto.setSourceName(record.get(SOURCES.SOURCE_NAME));
		dto.setSeverityOther(record.get(OUTBREAKS.SEVERITY_OTHER));
		dto.setSourceOther(record.get(OUTBREAKS.SOURCE_OTHER));
		dto.setComments(record.get(OUTBREAKS.COMMENTS));
		dto.setAdditionalInfo(record.get(OUTBREAKS.ADDITIONAL_INFO));
		dto.setStatus(record.get(OUTBREAKS.STATUS));

		// Fields that require either the owner of this record or an admin
		if (user.getUserID() == record.get(OUTBREAKS.USER_ID) || user.isAdmin())
		{
			dto.setUserId(record.get(OUTBREAKS.USER_ID));
			dto.setRealLatitude(record.get(OUTBREAKS.REAL_LATITUDE));
			dto.setRealLongitude(record.get(OUTBREAKS.REAL_LONGITUDE));

			// Overwrite the "view" coordinates with the real position
			dto.setViewLatitude(record.get(OUTBREAKS.REAL_LATITUDE));
			dto.setViewLongitude(record.get(OUTBREAKS.REAL_LONGITUDE));
		}

		return dto;
	}
}