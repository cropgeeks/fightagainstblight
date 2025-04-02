package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import java.util.stream.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.dto.*;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Severities.SEVERITIES;
import static jhi.fab.codegen.tables.Sources.SOURCES;
import static jhi.fab.codegen.tables.Varieties.VARIETIES;

@Path("/outbreaks")
public class OutbreaksResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader)
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

			List<OutbreaksDTO> results = context.select()
				.from(OUTBREAKS)
				.innerJoin(VARIETIES).on(OUTBREAKS.VARIETY_ID.eq(VARIETIES.VARIETY_ID))
				.innerJoin(SEVERITIES).on(OUTBREAKS.SEVERITY_ID.eq(SEVERITIES.SEVERITY_ID))
//				.innerJoin(SOURCES).on(SOURCES.SOURCE_ID.eq(SOURCES.SOURCE_ID))
				.fetch()
				.stream()
				.map(record -> {
					return mapDTO(record);
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
				.innerJoin(VARIETIES).on(OUTBREAKS.VARIETY_ID.eq(VARIETIES.VARIETY_ID))
				.innerJoin(SEVERITIES).on(OUTBREAKS.SEVERITY_ID.eq(SEVERITIES.SEVERITY_ID))
//				.innerJoin(SOURCES).on(SOURCES.SOURCE_ID.eq(SOURCES.SOURCE_ID))
				.where(OUTBREAKS.OUTBREAK_ID.eq(id))
				.fetchOne();

			if (record != null)
				return Response.ok(mapDTO(record)).build();

			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	OutbreaksDTO mapDTO(org.jooq.Record record)
	{
		OutbreaksDTO dto = new OutbreaksDTO();
		dto.setOutbreakId(record.get(OUTBREAKS.OUTBREAK_ID));
		dto.setOutbreakCode(record.get(OUTBREAKS.OUTBREAK_CODE));
////	dto.setUserId(record.get(OUTBREAKS.USER_ID));
		dto.setLatitude(record.get(OUTBREAKS.LATITUDE));
		dto.setLongitude(record.get(OUTBREAKS.LONGITUDE));
		dto.setDatesubmitted(record.get(OUTBREAKS.DATESUBMITTED));
		dto.setDatereceived(record.get(OUTBREAKS.DATERECEIVED));
		dto.setVarietyId(record.get(VARIETIES.VARIETY_ID));
		dto.setVarietyName(record.get(VARIETIES.VARIETY_NAME));
		dto.setSeverityId(record.get(SEVERITIES.SEVERITY_ID));
		dto.setSeverityName(record.get(SEVERITIES.SEVERITY_NAME));
//		dto.setSourceId(record.get(SOURCES.SOURCE_ID));
//		dto.setSourceName(record.get(SOURCES.SOURCE_NAME));
		dto.setSeverityother(record.get(OUTBREAKS.SEVERITYOTHER));
		dto.setSourceother(record.get(OUTBREAKS.SOURCEOTHER));
		dto.setComments(record.get(OUTBREAKS.COMMENTS));
		dto.setAdditionalinfo(record.get(OUTBREAKS.ADDITIONALINFO));
		dto.setStatus(record.get(OUTBREAKS.STATUS));

		return dto;
	}
}