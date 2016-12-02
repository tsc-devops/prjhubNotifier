package it.sorintlab.prjhubNotifier;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class PrjHub_Parser {
	
	final static Logger logger = Logger.getLogger(PrjHub_Parser.class);

	private int counter = 1;
	private HtmlPage prj_issues = null;
	private int total_issues=0;
	private int old_total_issues = 0;
	private final WebClient webClient = new WebClient(BrowserVersion.CHROME);
	private SendTicketAlert sendTicketAlert=null;
	
	private String loginGithub="";
	private String passwordGithub="";
	
	private String pagePrjhubLogin="";
	private String pageTotalIssuesPrjhub="";

	public void doParse() {
		
		logger.info("METODO doParse");
		
		// serve per commentare i log di htmlunit
		Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		
		try {
			webClient.getOptions().setJavaScriptEnabled(true);
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			
			logger.info("STARTED... OPENING PRJHUB AND SEARCHING FOR TOTAL ISSUES (TAKES A WHILE, APROX. 30 seconds)");
			HtmlPage html_page = null;
			int PAGE_RETRY = 10;
			try {
				html_page = webClient.getPage(this.getPagePrjhubLogin());
				Thread.sleep(4000);
			} catch (Exception e) {
				logger.info("ECCEZIONE, NON RIESCO A COLLEGARMI ALLA PAGINA DI PRJHUB");
			}
			for (int i = 0; !html_page.asXml().contains("Signup") && i < PAGE_RETRY; i++) {
				try {
					logger.info("*** SEARCHING FOR LOGIN BUTTON. ATTEMPT N.: +++ " + i + " +++ ***");
					Thread.sleep(1000 * (i + 1));
					html_page = webClient.getPage(this.getPagePrjhubLogin());
				} catch (Exception e) {
					logger.info("ECCEZIONE, NON RIESCO A TROVARE IL PULSANTE LOGIN NELLA PAGINA DI PRJHUB");
				}
			}
			if (html_page.asXml().contains("api/v1/github/login")) {
				logger.info("*** FOUND GITHUB LOGIN BUTTON ***");
				final HtmlAnchor anchor = html_page.getAnchorByHref("api/v1/github/login");
				final HtmlPage page2 = anchor.click();
				Thread.sleep(3000);
				
				// REDIRECTED TO GITHUB LOGIN PAGE; STARTING LOGIN PHASE
				final HtmlForm form = (HtmlForm) page2.querySelector("form");
				final HtmlSubmitInput button = (HtmlSubmitInput) form.getInputsByValue("Sign in").get(0);
				final HtmlTextInput textField = form.getInputByName("login");
				textField.setValueAttribute(this.getLoginGithub());
				final HtmlPasswordInput textField2 = form.getInputByName("password");
				textField2.setValueAttribute(this.getPasswordGithub());
				HtmlPage git_clicked = button.click();
				Thread.sleep(3000);
				if (git_clicked.asXml().contains("Authorize application")) {
					logger.info("*** AUTH BLOCK DETECTED ***");
					final HtmlForm auth_form = (HtmlForm) git_clicked
							.getFirstByXPath("//form[@action='/login/oauth/authorize']");
					
					final HtmlButton auth_button = (HtmlButton) auth_form
							.getFirstByXPath("//*[@id='js-pjax-container']/div[1]/div/div[2]/div/div[1]/form/p/button");
					auth_button.click();
					Thread.sleep(3000);
					parsePrj(false);
				} else {
					logger.info("*** NOT LOGGED IN; SO DONE IT, AND NO AUTH BLOCK ***");
					parsePrj(false);
				}
			} else {
				logger.info("*** ALREADY LOGGED IN ***");
				parsePrj(false);
			}
		} catch (Exception e) {
			logger.info("*** CATCHED EXCEPTION. ERROR IN PAGE PARSING ***");
			doParse();
		} finally {
			this.webClient.close();
		}
	}

	public void parsePrj(boolean refresh) {
		logger.info("METODO parsePrj");
		try {
			// sono loggato
			if(refresh==false){
				prj_issues = webClient.getPage(this.getPageTotalIssuesPrjhub());
			}
			else{
				prj_issues.refresh(); // il refresh con 2 pagine non va
			}
			Thread.sleep(300000); // aspetto 8 secondi   5minuti=300 secondi
			String issuesAsXmlOpen = prj_issues.asXml();
			if (issuesAsXmlOpen.contains("Total issues")) {
				// leggiamo il testo della pagina e ricaviamo il numero totale delle issues
				String[] split=issuesAsXmlOpen.split("\\r?\\n");  // splitto sugli a capo
				for(int i=0;i<split.length;i++){
					String s=split[i];
					if(s.contains("Total issues")){
						String [] numbers=s.split(" ");
						//System.out.println(numbers);
						logger.info("length " + numbers.length);
						// alcune volte non c'� il numero
						
						if(numbers.length==14){     // c� il numero
							old_total_issues=Integer.parseInt(numbers[numbers.length-1]);
							logger.info("*** TOTAL ISSUES: " + old_total_issues);
							break;
						}
						else{   // bisogna rileggere la pagina
							logger.info("ERRORE NON C'e'IL NUMERO DELLE ISSUE APERTE");
							
							// cancelliamo i cookie e la cache
							URL url=new URL(this.getPagePrjhubLogin());
							webClient.getCookies(url).clear();
							webClient.getCache().clear();
							doParse();
						}
					}
				}	
				done();
			} 
			else {
				webClient.close();
				doParse();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	protected void doInBackground() {
		logger.info("METODO doinbackgroud");
		
		sendTicketAlert=new SendTicketAlert();
		if(sendTicketAlert.getValidConfigProperties()==true){   //il file di configurazione delle mail � settato correttamente
			boolean validConfig=readConfigProperties();
			if(validConfig==true){ //il file di configurazione di PrjJub � settato correttamente
				doParse();
			}
			else{
				logger.error("Ci sono degli errori nel caricamento del file configPrjHub.properties");
			}
		}else{
			logger.error("Ci sono degli errori nel caricamento del file configMail.properties");
		}
	}

	protected void done() {
		logger.info("METODO done");
		if(counter!=1){
			if (total_issues>old_total_issues){
				int nuoviTicket=total_issues-old_total_issues;
				logger.info("sono stati aperti "+ nuoviTicket + "nuovi ticket");
				for(int i=0;i<nuoviTicket;i++){
					this.sendTicketAlert.send();
				}
			}
		}
		else{
			counter++;
		}
		logger.info("total = " + total_issues + "     old total = " + old_total_issues);
		old_total_issues = total_issues;
		parsePrj(true);
	}
	
	// lettura del file delle propriet�
	private boolean readConfigProperties(){
		
		logger.info("CARICAMENTO PROPRIETA' FILE CONFIGPRJHUB.PROPERTIES");
		
		boolean validProperties=true;
		
		Properties prop = new Properties();
		InputStream input = null;
		
		String filename = "configPrjhub.properties";
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		input = classloader.getResourceAsStream(filename);

		try {
			prop.load(input);
		} catch (Exception e1) {
			logger.error("Sorry, unable to find " + filename);
			return false;
		}

		// l'input e' valido, il file e' stato caricato correttamente

		// get the property value
		this.setLoginGithub(prop.getProperty("loginGithub"));
		this.setPasswordGithub(prop.getProperty("passwordGithub"));
		this.setPagePrjhubLogin(prop.getProperty("pagePrjhubLogin"));
		this.setPageTotalIssuesPrjhub(prop.getProperty("pageTotalIssuesPrjhub"));

		if(this.getLoginGithub()==null){
			logger.error("loginGithub = NULL");
			validProperties=false;
		}
		if(this.getPasswordGithub()==null){
			logger.error("passwordGithub = NULL");
			validProperties=false;
		}
		if(this.getPagePrjhubLogin()==null){
			logger.error("pagePrjhubLogin = NULL");
			validProperties=false;
		}
		if(this.getPageTotalIssuesPrjhub()==null){
			logger.error("pageTotalIssuesPrjhub = NULL");
			validProperties=false;
		}

		logger.info("login = " + this.getLoginGithub());
		logger.info("password = " + this.getPasswordGithub());
		logger.info("pagePrjhubLogin = " + this.getPagePrjhubLogin());
		logger.info("pageTotalIssuesPrjhub = " + this.getPageTotalIssuesPrjhub());
		
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

	public String getLoginGithub() {
		return loginGithub;
	}

	public void setLoginGithub(String loginGithub) {
		this.loginGithub = loginGithub;
	}

	public String getPasswordGithub() {
		return passwordGithub;
	}

	public void setPasswordGithub(String passwordGithub) {
		this.passwordGithub = passwordGithub;
	}

	public String getPagePrjhubLogin() {
		return pagePrjhubLogin;
	}

	public void setPagePrjhubLogin(String pagePrjhubLogin) {
		this.pagePrjhubLogin = pagePrjhubLogin;
	}

	public String getPageTotalIssuesPrjhub() {
		return pageTotalIssuesPrjhub;
	}

	public void setPageTotalIssuesPrjhub(String pageTotalIssuesPrjhub) {
		this.pageTotalIssuesPrjhub = pageTotalIssuesPrjhub;
	}
	
}