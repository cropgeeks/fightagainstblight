package jhi.fab;

import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

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

	public static void email(String address, String htmlMessage)
		throws Exception
    {
        String emailServer = System.getenv("EMAIL_HOST");
		String emailPort = System.getenv("EMAIL_PORT");
        String emailAddress = System.getenv("EMAIL_ADDRESS");
        String emailAlias = System.getenv("EMAIL_ALIAS");

		Properties props = new Properties();
		props.put("mail.smtp.host", emailServer);
		props.put("mail.smtp.port", emailPort);
		props.put("mail.smtp.auth", "false");

		Session session = Session.getInstance(props);

		Message message = new MimeMessage(session);
		message.setContent(htmlMessage, "text/html; charset=utf-8");
		message.setFrom(new InternetAddress(emailAddress, emailAlias));

		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));
		message.setSubject("Login to FightAgainstBlight");

		Transport.send(message);
	}
}