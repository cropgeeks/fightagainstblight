package jhi.fab;

import java.util.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.*;

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

	public static void email(String address, boolean includeAdmins, String subject, String htmlMessage, byte[] image)
	{
		try
		{
			String emailServer = System.getenv("EMAIL_HOST");
			String emailPort = System.getenv("EMAIL_PORT");
			String emailAdmin = System.getenv("FAB_ADMIN");
			String emailAlias = System.getenv("FAB_ALIAS");

			Properties props = new Properties();
			props.put("mail.smtp.host", emailServer);
			props.put("mail.smtp.port", emailPort);
			props.put("mail.smtp.auth", "false");

			Session session = Session.getInstance(props);

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(emailAdmin, emailAlias));
			message.setSubject(subject);
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(address));
			if (includeAdmins)
				message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(emailAdmin));

			// Create a MimeMultipart
			MimeMultipart multipart = new MimeMultipart("related");

			// Create the HTML part
			MimeBodyPart htmlPart = new MimeBodyPart();
			htmlMessage = "<p><img src='cid:headerID'/></p>" + htmlMessage;
			htmlPart.setContent(htmlMessage, "text/html");
			multipart.addBodyPart(htmlPart);

			// Add the header image
			MimeBodyPart headerPart = new MimeBodyPart();
			// TODO: Not ideal but works for now
			DataSource fds = new FileDataSource("webapps/fab/WEB-INF/fab-logo.png");
			headerPart.setDataHandler(new DataHandler(fds));
			headerPart.setHeader("Content-ID", "<headerID>");
			multipart.addBodyPart(headerPart);

			// Add any addtional image
			if (image != null)
			{
				MimeBodyPart imagePart = new MimeBodyPart();
				DataSource bds = new ByteArrayDataSource(image, "image/png");
				imagePart.setDataHandler(new DataHandler(bds));
				imagePart.setHeader("Content-ID", "<imageID>");
				multipart.addBodyPart(imagePart);
			}

			// Set the complete message parts
			message.setContent(multipart);

			// Send the message
			Transport.send(message);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}