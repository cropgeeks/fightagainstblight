package jhi.fab;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/")
@Path("/")
@WebListener
public class FAB extends ResourceConfig implements ServletContextListener
{
	public FAB()
	{
		packages("jhi.fab");
	}

	@GET
	public String getInformation(@Context ServletContext context)
		throws Exception
	{
		return "Fight Against Blight API - " + new java.util.Date();
	}

	@Override
	public void contextInitialized(ServletContextEvent sce)
	{
		DatabaseUtils.init(sce.getServletContext());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		DatabaseUtils.close();
	}
}