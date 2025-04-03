package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import java.util.stream.*;
import jhi.fab.codegen.enums.*;

import org.jooq.*;
import org.jooq.impl.*;

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
				.join(SUBSAMPLES).on(SUBSAMPLES.OUTBREAK_ID.eq(OUTBREAKS.OUTBREAK_ID))
				.join(SEVERITIES).on(OUTBREAKS.SEVERITY_ID.eq(SEVERITIES.SEVERITY_ID))
				.join(SOURCES).on(OUTBREAKS.SOURCE_ID.eq(SOURCES.SOURCE_ID));

			if (year != null)
				query.where(DSL.year(OUTBREAKS.DATESUBMITTED).eq(year));
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
				.innerJoin(SEVERITIES).on(OUTBREAKS.SEVERITY_ID.eq(SEVERITIES.SEVERITY_ID))
				.innerJoin(SOURCES).on(SOURCES.SOURCE_ID.eq(SOURCES.SOURCE_ID))
				.where(OUTBREAKS.OUTBREAK_ID.eq(id))
				.fetchOne();

			if (record != null)
				return Response.ok(mapDTO(record, user)).build();

			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	OutbreaksDTO mapDTO(org.jooq.Record record, User user)
	{
		OutbreaksDTO dto = new OutbreaksDTO();
		dto.setOutbreakId(record.get(OUTBREAKS.OUTBREAK_ID));
		dto.setOutbreakCode(record.get(OUTBREAKS.OUTBREAK_CODE));
		dto.setViewlongitude(record.get(OUTBREAKS.VIEWLATITUDE));
		dto.setViewlatitude(record.get(OUTBREAKS.VIEWLONGITUDE));
		dto.setDatesubmitted(record.get(OUTBREAKS.DATESUBMITTED));
		dto.setDatereceived(record.get(OUTBREAKS.DATERECEIVED));
		dto.setSeverityId(record.get(SEVERITIES.SEVERITY_ID));
		dto.setSeverityName(record.get(SEVERITIES.SEVERITY_NAME));
		dto.setSourceId(record.get(SOURCES.SOURCE_ID));
		dto.setSourceName(record.get(SOURCES.SOURCE_NAME));
		dto.setSeverityother(record.get(OUTBREAKS.SEVERITYOTHER));
		dto.setSourceother(record.get(OUTBREAKS.SOURCEOTHER));
		dto.setComments(record.get(OUTBREAKS.COMMENTS));
		dto.setAdditionalinfo(record.get(OUTBREAKS.ADDITIONALINFO));
		dto.setStatus(record.get(OUTBREAKS.STATUS));

		// Fields that require either the owner of this record or an admin
		if (user.getUserID() == record.get(OUTBREAKS.USER_ID) || user.isAdmin())
		{
			dto.setUserId(record.get(OUTBREAKS.USER_ID));
			dto.setReallatitude(record.get(OUTBREAKS.REALLATITUDE));
			dto.setReallongitude(record.get(OUTBREAKS.REALLONGITUDE));

			// Overwrite the "view" coordinates with the real position
			dto.setViewlatitude(record.get(OUTBREAKS.REALLATITUDE));
			dto.setViewlongitude(record.get(OUTBREAKS.REALLONGITUDE));
		}

		return dto;
	}
}