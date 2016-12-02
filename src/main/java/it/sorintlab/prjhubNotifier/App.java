package it.sorintlab.prjhubNotifier;

import org.apache.log4j.Logger;


public class App {
	
	final static Logger logger = Logger.getLogger(App.class);
	
	public static void main(String[] args) {
		
		// il file del log viene scritto in append
		logger.info("Info : " + "Applicazione avviata");
		
		// esecuzione della login e del controllo delle nuove issues
		PrjHub_Parser login = new PrjHub_Parser();
		login.doInBackground();
				
		//SendTicketAlert sendTicketAlert=new SendTicketAlert();
		//sendTicketAlert.send();
		
	}
	
}