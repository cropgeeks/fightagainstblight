package jhi.fab;

import java.sql.*;
import java.util.*;
import java.util.stream.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.dto.*;
import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.SsrGenotypes.SSR_GENOTYPES;
import static jhi.fab.codegen.tables.Outbreaks.OUTBREAKS;
import static jhi.fab.codegen.tables.Subsamples.SUBSAMPLES;
import static jhi.fab.codegen.tables.Varieties.VARIETIES;

public class SubsamplesResource
{
	@Produces(MediaType.APPLICATION_JSON)
	public static Response getSubsamples(String authHeader, Integer outbreakID)
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
			Outbreaks outbreak = context.select()
				.from(OUTBREAKS)
				.where(OUTBREAKS.OUTBREAK_ID.eq(outbreakID))
				.fetchOneInto(Outbreaks.class);
			int ownerID = outbreak.getUserId();

			// If you're not an admin, or not the owner, then it's forbidden
			if (user.isAdmin() == false || user.getUserID() != ownerID)
				return Response.status(Response.Status.FORBIDDEN).build();

			var query =context.select()
				.from(SUBSAMPLES)
				.leftJoin(VARIETIES).on(SUBSAMPLES.VARIETY_ID.eq(VARIETIES.VARIETY_ID))
				.leftJoin(SSR_GENOTYPES).on(SUBSAMPLES.GENOTYPE_ID.eq(SSR_GENOTYPES.GENOTYPE_ID))
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

	private static SubsamplesDTO mapDTO(org.jooq.Record record, User user, int ownerID)
	{
		SubsamplesDTO dto = new SubsamplesDTO();

		dto.setSubsampleId(record.get(SUBSAMPLES.SUBSAMPLE_ID));
		dto.setSubsampleCode(record.get(SUBSAMPLES.SUBSAMPLE_CODE));
		dto.setOutbreakId(record.get(SUBSAMPLES.OUTBREAK_ID));
		dto.setVarietyId(record.get(SUBSAMPLES.VARIETY_ID));
		dto.setVarietyName(record.get(VARIETIES.VARIETY_NAME));
		dto.setMaterial(record.get(SUBSAMPLES.MATERIAL));
		dto.setDateGenotyped(record.get(SUBSAMPLES.DATE_GENOTYPED));

		// Fields that require either the owner of the associated outbreak or an admin
		if (user.getUserID() == ownerID || user.isAdmin())
		{
			dto.setGenotypeId(record.get(SUBSAMPLES.GENOTYPE_ID));
			dto.setGenotypeName(record.get(SSR_GENOTYPES.GENOTYPE_NAME));
			dto.setUserComments(record.get(SUBSAMPLES.USER_COMMENTS));
		}

		return dto;
	}
}