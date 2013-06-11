package com.jerusalemu.jufiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.googlecode.objectify.ObjectifyService;

@SuppressWarnings("serial")
public class Email_CatcherServlet extends HttpServlet {
	
	String toEmail;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		 toEmail = req.getRequestURI().split("/")[3]; 
//		ObjectifyService.register(File.class);
		Properties props = new Properties(); 
        Session session = Session.getDefaultInstance(props, null); 
        try {
			MimeMessage message = new MimeMessage(session, req.getInputStream());
			processMessage(message);
//			File f = new File(message.getSubject());
//			ofy().save().entity(f).now();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	private boolean processMessage(MimeMessage message) {
		String date = getMessageDate(message);
		String from = "unknown";

		try {
			from = message.getFrom()[0].toString();
			Object content = message.getContent();
			if (message.getContentType().startsWith("text/plain")) {
//				processMail(from, date, (String) content);
				System.err.println(content.toString());
				return true;
			} else if (content instanceof Multipart) {
				Multipart mp = (Multipart) content;
				for (int i = 0; i < mp.getCount(); i++) {
					if (handlePart(from, date, mp.getBodyPart(i))) {
						return true;
					}
				}
				return false;
			} else {
				System.err.println("Unable to process message content - unknown content type");
			}
		} catch (IOException e) {
			System.err.println("Exception handling incoming email " + e);
		} catch (MessagingException e) {
			System.err.println("Exception handling incoming email " + e);
		} catch (Exception e) {
			System.err.println("Exception handling incoming email " + e);
		}

		return false;
	}
	
	
	private boolean handlePart(String from, String date, BodyPart part)
			throws MessagingException, IOException {
		if (part.getContentType().startsWith("text/plain")
				|| part.getContentType().startsWith("text/html")) {
//			processMail(from, date, (String) part.getContent());
			System.err.println(part.getContent().toString());
			return true;
		} else {
			if (part.getContent() instanceof Multipart) {
				Multipart mp = (Multipart) part.getContent();
//				System.err.println("Handling a multipart sub-message with " + mp.getCount() + " sub-parts");
				for (int i = 0; i < mp.getCount(); i++) {
					handlePart(from, date, mp.getBodyPart(i));
				}
			} else {
				System.err.println("new attachment " + part.getContent().toString());
				savePdf (from, part);
			}
			return false;
		}
	}
	
	private void savePdf (String from, BodyPart part) throws MessagingException {
		// Get a file service
		  FileService fileService = FileServiceFactory.getFileService();
		  
		  String contentType = part.getContentType().substring(0, part.getContentType().indexOf(";"));
		  String filename = part.getContentType().split("\"")[1];
//		  System.err.println("debug: " + contentType + "  " + filename);

		  // Create a new Blob file with mime-type "application/pdf"
		  AppEngineFile file = null;
		try {
			file = fileService.createNewBlobFile(contentType, filename);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		  String path = file.getFullPath();

		  // Write more to the file in a separate request:
		  file = new AppEngineFile(path);

		  // This time lock because we intend to finalize
		  boolean lock = true;
		  FileWriteChannel writeChannel;
		try {
			writeChannel = fileService.openWriteChannel(file, lock);
			IOUtils.copy(part.getInputStream(), Channels.newOutputStream(writeChannel));
			writeChannel.closeFinally();
		} catch (IOException | MessagingException e) {
			e.printStackTrace();
		}

		System.err.println("creating file: " + fileService.getBlobKey(file).getKeyString());
		System.err.println("from: " + from);
		
		try {
			postToCore18 (fileService.getBlobKey(file).getKeyString(), from);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private String getMessageDate(Message message) {
		Date when = null;
		try {
			when = message.getReceivedDate();
			if (when == null) {
				when = message.getSentDate();
			}
			if (when == null) {
				return null;
			}
		} catch (MessagingException e) {
			System.err.println("Cannot get message date: " + e);
			e.printStackTrace();
			return null;
		}

		DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		return format.format(when);
	}
	
	private void postToCore18 (String key, String from) throws IOException {
		String url = "http://www.core18.org/callbacks/gotfile.php";
		String charset = "UTF-8";
		String query = String.format("key=%s&from=%s&to=%s", 
		     URLEncoder.encode(key, charset), 
		     URLEncoder.encode(from, charset),
		     URLEncoder.encode(toEmail, charset));
		URLConnection connection = new URL(url).openConnection();
		connection.setDoOutput(true); // Triggers POST.
		connection.setRequestProperty("Accept-Charset", charset);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
		OutputStream output = null;
		try {
		     output = connection.getOutputStream();
		     output.write(query.getBytes(charset));
		} finally {
		     if (output != null) try { output.close(); } catch (IOException logOrIgnore) {}
		}
		InputStream response = connection.getInputStream();
	}
}
