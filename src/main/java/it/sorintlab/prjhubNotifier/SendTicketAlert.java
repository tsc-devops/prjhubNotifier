package it.sorintlab.prjhubNotifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class SendTicketAlert {
	
final static Logger logger = Logger.getLogger(SendTicketAlert.class);
	
	private String userMail="";
	private String passwordMail="";
	private String userMailFrom="";
	private String userMailTo="";
	private String mail_smtp_host;
	private String mail_smtp_socketFactory_port;
	private String mail_smtp_socketFactory_class;
	private String mail_smtp_auth;
	private String mail_smtp_port;
	private boolean validConfigProperties=false;

	public SendTicketAlert(){
		logger.info("costruzione oggetto per inviare le mail");
		boolean validConfig=readConfigProperties();
		this.setValidConfigProperties(validConfig);
	}
	
	public void send() {
		
		if(this.getValidConfigProperties()==true){

			Properties props = new Properties();
			props.put("mail.smtp.host", this.getMail_smtp_host());
			props.put("mail.smtp.socketFactory.port", this.getMail_smtp_socketFactory_port());
			props.put("mail.smtp.socketFactory.class", this.getMail_smtp_socketFactory_class());
			props.put("mail.smtp.auth", this.getMail_smtp_auth());
			props.put("mail.smtp.port", this.getMail_smtp_port());

			Authenticator auth = new Authenticator() {
				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(userMail, passwordMail);
				}
			};

			Session session = Session.getDefaultInstance(props, auth);

			try {
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(this.userMailFrom));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(this.getUserMailTo()));
				message.setSubject("Hey Ho! New OrientDB ticket on PrjHub!");
				message.setText("Dear DevOps,"
						+ "\n\n New OrientDB ticket on PrjHub, go and check it! \n\n http://www.prjhub.com/#/issues?q=is:open%20client:%22_my%22&page=1 - PrjHub - My Clients Issues");

				Transport.send(message);
				logger.info("Email inviata");
			} catch (MessagingException e) {
				logger.error("Errore nel settaggio delle impostazioni del messaggio da inviare ",  e);
			}
		}
		else{
			logger.error("Mail non inviata in quanto ci sono stati degli errori nel caricamento del file configMail.properties");
		}
	}

	// lettura del file delle propriet�
	private boolean readConfigProperties(){

		logger.info("CARICAMENTO PROPRIETA' FILE CONFIGMAIL.PROPERTIES");

		boolean validProperties=true;

		Properties prop = new Properties();
		InputStream input = null;

		String filename = "configMail.properties";
		//System.out.println(this.getClass().getResource("").getPath());
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		input = classloader.getResourceAsStream(filename);
		
		
		//ClassLoader classLoader = getClass().getClassLoader();
		//File file = new File(classLoader.getResource(filename).getFile());
		
		
		//input = this.getClass().getResourceAsStream(filename);


		try {
			prop.load(input);
		} catch (Exception e1) {
			logger.error("Sorry, unable to find " + filename);
			return false;
		}

		// l'input � valido, il file � stato caricato correttamente

		// get the property value 
		this.setUserMail(prop.getProperty("userMail"));
		this.setPasswordMail(prop.getProperty("passwordMail"));
		this.setUserMailFrom(prop.getProperty("userMailFrom"));
		this.setUserMailTo(prop.getProperty("userMailTo"));
		this.setMail_smtp_host(prop.getProperty("mail.smtp.host"));
		this.setMail_smtp_socketFactory_port(prop.getProperty("mail.smtp.socketFactory.port"));
		this.setMail_smtp_socketFactory_class(prop.getProperty("mail.smtp.socketFactory.class"));
		this.setMail_smtp_auth(prop.getProperty("mail.smtp.auth"));
		this.setMail_smtp_port(prop.getProperty("mail.smtp.port"));

		if(this.getUserMail()==null){
			logger.error("userMail = NULL");
			validProperties=false;
		}
		if(this.getPasswordMail()==null){
			logger.error("passwordMail = NULL");
			validProperties=false;
		}
		if(this.getUserMailFrom()==null){
			logger.error("userMailFrom = NULL");
			validProperties=false;
		}
		if(this.getUserMailTo()==null){
			logger.error("userMailTo = NULL");
			validProperties=false;
		}
		if(this.getMail_smtp_host()==null){
			logger.error("mail.smtp.host = NULL");
			validProperties=false;
		}
		if(this.getMail_smtp_socketFactory_port()==null){
			logger.error("mail.smtp.socketFactory.port = NULL");
			validProperties=false;
		}
		if(this.getMail_smtp_socketFactory_class()==null){
			logger.error("mail.smtp.socketFactory.class = NULL");
			validProperties=false;
		}
		if(this.getMail_smtp_auth()==null){
			logger.error("mail.smtp.auth = NULL");
			validProperties=false;
		}
		if(this.getMail_smtp_port()==null){
			logger.error("mail.smtp.port = NULL");
			validProperties=false;
		}


		logger.info("Login = " + this.getUserMail());
		logger.info("Password = " + this.getPasswordMail());
		logger.info("UserMailFrom = " + this.getUserMailFrom());
		logger.info("UserMailTo = " + this.getUserMailTo());
		logger.info("Mail_smtp_host = " + this.getMail_smtp_host());
		logger.info("Mail_smtp_socketFactory_port = " + this.getMail_smtp_socketFactory_port());
		logger.info("Mail_smtp_socketFactory_class = " + this.getMail_smtp_socketFactory_class());
		logger.info("Mail_smtp_auth = " + this.getMail_smtp_auth());
		logger.info("Mail_smtp_port = " + this.getMail_smtp_port());

		try {
			input.close();
		} catch (IOException e) {
			logger.error("Exception ", e);
		}

		if(validProperties==false){
			return false;
		}
		else{
			return true;
		}
	}
	
	public String getUserMail() {
		return userMail;
	}

	public void setUserMail(String userMail) {
		this.userMail = userMail;
	}

	public String getPasswordMail() {
		return passwordMail;
	}

	public void setPasswordMail(String passwordMail) {
		this.passwordMail = passwordMail;
	}

	public String getMail_smtp_host() {
		return mail_smtp_host;
	}

	public void setMail_smtp_host(String mail_smtp_host) {
		this.mail_smtp_host = mail_smtp_host;
	}

	public String getMail_smtp_socketFactory_port() {
		return mail_smtp_socketFactory_port;
	}

	public void setMail_smtp_socketFactory_port(String mail_smtp_socketFactory_port) {
		this.mail_smtp_socketFactory_port = mail_smtp_socketFactory_port;
	}

	public String getMail_smtp_socketFactory_class() {
		return mail_smtp_socketFactory_class;
	}

	public void setMail_smtp_socketFactory_class(
			String mail_smtp_socketFactory_class) {
		this.mail_smtp_socketFactory_class = mail_smtp_socketFactory_class;
	}

	public String getMail_smtp_auth() {
		return mail_smtp_auth;
	}

	public void setMail_smtp_auth(String mail_smtp_auth) {
		this.mail_smtp_auth = mail_smtp_auth;
	}

	public String getMail_smtp_port() {
		return mail_smtp_port;
	}

	public void setMail_smtp_port(String mail_smtp_port) {
		this.mail_smtp_port = mail_smtp_port;
	}

	public String getUserMailFrom() {
		return userMailFrom;
	}

	public void setUserMailFrom(String userMailFrom) {
		this.userMailFrom = userMailFrom;
	}

	public String getUserMailTo() {
		return userMailTo;
	}

	public void setUserMailTo(String userMailTo) {
		this.userMailTo = userMailTo;
	}

	public boolean isValidConfigProperties() {
		return validConfigProperties;
	}

	public void setValidConfigProperties(boolean validConfigProperties) {
		this.validConfigProperties = validConfigProperties;
	}
	
	public boolean getValidConfigProperties() {
		return this.validConfigProperties;
	}
	
	
}