package jhi.fab;

import java.sql.*;
import java.util.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.Varieties.VARIETIES;

@Path("/varieties")
public class VarietiesResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response varieties(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			List<Varieties> list = context.selectFrom(VARIETIES)
				.fetchInto(Varieties.class);

			// Sort alphabetically
			Collections.sort(list, java.util.Comparator.comparing(Varieties::getVarietyName));

			// Move the unknowns/others to the top of the list
			for (int i = 0; i < list.size(); i++)
			{
				Varieties v = list.get(i);

				if (v.getVarietyName().equalsIgnoreCase("unknown/other"))
				{
					list.remove(v);
					list.add(0, v);
				}
			}

			return Response.ok(list).build();
		}
	}
}