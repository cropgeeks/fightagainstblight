package jhi.fab;

import jakarta.servlet.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/")
@Path("/")
public class FAB extends ResourceConfig
{
	public FAB()
	{
		packages("jhi.fab");
	}

	@GET
	public String getTestObject(@Context ServletContext context)
		throws Exception
	{
		return "Fight Against Blight API - " + new java.util.Date();
	}
}