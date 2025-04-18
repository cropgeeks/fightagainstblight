package jhi.fab;

import java.awt.image.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.imageio.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import org.jooq.*;
import org.jooq.impl.*;

import com.google.zxing.*;
import com.google.zxing.client.j2se.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.*;

import jhi.fab.codegen.tables.pojos.*;
import static jhi.fab.codegen.tables.UserSessions.USER_SESSIONS;
import static jhi.fab.codegen.tables.Users.USERS;

@Path("/users")
public class UserSessionsResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsers(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		// You must be at least logged in use this method
		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();
		// And you must be an admin
		if (user.isAdmin() == false)
			return Response.status(Response.Status.FORBIDDEN).build();

		return Response.ok(getAllUsers()).build();
	}

	List<Users> getAllUsers()
		throws SQLException
	{
		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			List<Users> results = context.selectFrom(USERS)
				.fetchInto(Users.class);

			return results;
		}
	}

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOutbreaks(@HeaderParam("Authorization") String authHeader)
		throws SQLException
	{
		User user = new User(authHeader);

		if (user.isOK() == false)
			return Response.status(Response.Status.UNAUTHORIZED).build();

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			Users dbUser = context.selectFrom(USERS)
				.where(USERS.USER_ID.eq(user.getUserID()))
				.fetchOneInto(Users.class);

			if (dbUser != null)
				return Response.ok(dbUser).build();
			else
				return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@POST
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(String email)
		throws Exception, SQLException
	{
		System.out.println("Searching for: " + email);

		try (Connection conn = DatabaseUtils.getConnection())
		{
			DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
			Users user = context.selectFrom(USERS)
				.where(USERS.EMAIL.eq(email))
				.fetchOneInto(Users.class);

			// If the user exists, create and push a new token for them
			if (user != null)
			{
				UUID uuid = UUID.randomUUID();
				System.out.println("Created " + uuid + " for " + email);

				context.insertInto(USER_SESSIONS)
					.set(USER_SESSIONS.USER_ID, user.getUserId())
					.set(USER_SESSIONS.TOKEN, uuid.toString())
					.set(USER_SESSIONS.CREATED_ON, LocalDateTime.now())
					.execute();

				// Now email...
				String host = System.getenv("FAB_URL");

				String link = host + "/#/?token=" + uuid.toString();
				String message = "<p>Hi " + user.getUserName() + ",</p>"
					+ "<p>You're receiving this message as part of the login "
					+ "procedure to the James Hutton Institute's Fight Against "
					+ "Blight service. If you didn't request this, please "
					+ "contact us at <a href='mailto:fab@hutton.ac.uk'>"
					+ "fab@hutton.ac.uk</a>.</p>"
					+ "<p>You can login by following this link:<br>"
					+ "<a href='" + link + "'>" + link + "</a></p>"
					+ "<p>You can also scan this QR code to login on other devices:</p>"
					+ "<p><img src='cid:imageID'/></p>"
					+ "<p>Thanks for using Fight Against Blight and helping with "
					+ "research into blight populations around the UK.</p>";

				byte[] qrCode = generateQRCodeBase64(link);
				FAB.email(user.getEmail(), false, "Login to Fight Against Blight", message, qrCode);
			}
			else
				System.out.println("User not found");

			// We'll always return OK, even when the user isn't found as the UI
			// should just be informing the user that *if* their address was
			// found, they'll be receiving an email
			return Response.ok().build();
		}
	}

	private byte[] generateQRCodeBase64(String link)
		throws Exception
	{
		// Gernerate a QR code
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(link, BarcodeFormat.QR_CODE, 200, 200);
		BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

		// Convert it to base64 for embedding in the email
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(qrImage, "png", baos);

		byte[] imageBytes = baos.toByteArray();
		baos.close();

		return imageBytes;
	}

	// Start a timer that will check the DB for expired tokens
	void startTokenTimer()
	{
		new Timer().schedule(new TimerTask() {
			public void run()
			{
				try (Connection conn = DatabaseUtils.getConnection())
				{
					// Anything older than this timestamp is getting removed
					Instant instant = Instant.ofEpochMilli(
						System.currentTimeMillis()
						- Long.parseLong(System.getenv("FAB_TOKEN_EXPIRE")));

					LocalDateTime dateTime = LocalDateTime.ofInstant(instant,
						ZoneId.systemDefault());

					DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
					context.deleteFrom(USER_SESSIONS)
						.where(USER_SESSIONS.CREATED_ON.lessThan(dateTime))
						.execute();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		// Run now (0), and then every hour
		}, 0, (60*60*1000));
	}
}