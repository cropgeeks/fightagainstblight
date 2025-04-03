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
import static jhi.fab.codegen.tables.SsrGenotypes.SSR_GENOTYPES;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.Varieties.VARIETIES;
import jhi.fab.codegen.tables.pojos.*;

@Path("/subsamples")
public class SubsamplesResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader,
								 @QueryParam("outbreak") Integer outbreakID)
		throws SQLException
	{
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);

			// First up, query the outbreak itself so we can see who "owns" it
			Outbreaks outbreak = context.select()
				.from(OUTBREAKS)
				.where(OUTBREAKS.OUTBREAK_ID.eq(outbreakID))
				.fetchOneInto(Outbreaks.class);
			int ownerID = outbreak.getUserId();

			var query =context.select()
				.from(SUBSAMPLES)
				.join(VARIETIES).on(SUBSAMPLES.VARIETY_ID.eq(VARIETIES.VARIETY_ID))
				.join(SSR_GENOTYPES).on(SUBSAMPLES.GENOTYPE_ID.eq(SSR_GENOTYPES.GENOTYPE_ID))
				.where(SUBSAMPLES.OUTBREAK_ID.eq(outbreakID));

			List<SubsamplesDTO> results = query.fetch()
				.stream()
				.map(record -> {
					return mapDTO(record, user, ownerID);
				})
				.collect(Collectors.toList());

			System.out.println("Returning " + results.size() + " results");

			return Response.ok(results).build();
		}
	}

	private SubsamplesDTO mapDTO(org.jooq.Record record, User user, int ownerID)
	{
		SubsamplesDTO dto = new SubsamplesDTO();

		dto.setSubsampleId(record.get(SUBSAMPLES.SUBSAMPLE_ID));
		dto.setSubsampleCode(record.get(SUBSAMPLES.SUBSAMPLE_CODE));
		dto.setOutbreakId(record.get(SUBSAMPLES.OUTBREAK_ID));
		dto.setVarietyId(record.get(SUBSAMPLES.VARIETY_ID));
		dto.setVarietyName(record.get(VARIETIES.VARIETY_NAME));
		dto.setMaterial(record.get(SUBSAMPLES.MATERIAL));
		dto.setDategenotyped(record.get(SUBSAMPLES.DATEGENOTYPED));

		// Fields that require either the owner of the associated outbreak or an admin
		if (user.getUserID() == ownerID || user.isAdmin())
		{
			dto.setGenotypeId(record.get(SUBSAMPLES.GENOTYPE_ID));
			dto.setGenotypeName(record.get(SSR_GENOTYPES.GENOTYPE_NAME));
			dto.setComments(record.get(SUBSAMPLES.COMMENTS));
		}

		return dto;
	}
}