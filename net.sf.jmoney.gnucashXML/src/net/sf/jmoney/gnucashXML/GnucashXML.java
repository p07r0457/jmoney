/*
 * Created on 10 juil. 2004
 */

package net.sf.jmoney.gnucashXML;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.filechooser.FileFilter;

import net.sf.jmoney.Constants;

import net.sf.jmoney.model2.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.dialogs.EventLoopProgressMonitor;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;

// all for xml
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import oracle.xml.parser.v2.*;

import java.lang.reflect.InvocationTargetException;
import java.net.*;

/** 
 * @author Faucheux
 */
public class GnucashXML implements FileFormat, IRunnableWithProgress {
	NumberFormat number = NumberFormat.getInstance(Locale.US);
	Calendar calendar = Calendar.getInstance();
	DateFormat swiftDateFormat = new SimpleDateFormat("yyMMdd");
	NumberFormat swiftNumberFormat = NumberFormat.getInstance(Locale.GERMANY);
	IWorkbenchWindow window;
	AccountChooser accountChooser;
	DateFormat gnucashDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	Hashtable accountsGUIDTable;  
	Session session;
	File file;

	/**
	 * Create a new GnucashXML Class
	 */
	public GnucashXML(IWorkbenchWindow window) {
		this.window = window;
		number.setMinimumFractionDigits(2);
		number.setMaximumFractionDigits(2);
	}

	/**
	 * FileFiter which the OpenBox offers to fiter the Gnucash Files
	 */
	public FileFilter fileFilter() {
		return null;
	}

	public void importFile(Session session, File file) {
	    ProgressMonitorJobsDialog progressDialog = new ProgressMonitorJobsDialog(window.getShell());
	    this.session = session;
	    this.file = file;
	    try {
	        progressDialog.run(true,false,this);
		} catch (InvocationTargetException e) {
		    Throwable e2 = e.getCause();
		    System.err.println(e.toString());
		    e2.printStackTrace(System.err);
		} catch (InterruptedException e) {
		    System.err.println(e.toString());
		}
	    EventLoopProgressMonitor monitor = new EventLoopProgressMonitor(new NullProgressMonitor());
	    // run(monitor);
	}

	/**
	 * import of the GnuCash File
	 * 
	 * The XML File has following structur:
	 * 
	 * <?xml version="1.0"?>
	 * <gnc-v2>
	 * <count-data type="account">63</count-data>
	 * <count-data type="transaction">1660</count-data>
	 * <account version="2.0.0">
	 *    <!-- Cf accounts -->
	 * </account>
	 * <account version="2.0.0">
	 * </account>
	 * <transaction version="2.0.0">
	 *    <!-- Cf transaction -->
	 * </transaction>
	 * <transaction version="2.0.0">
	 *    <!-- Cf transaction -->
	 * </transaction>
	 * </gnc-v2>
	 */
	public void run (IProgressMonitor monitor) {
		DOMParser parser = new DOMParser();
		accountsGUIDTable = new Hashtable(); 		// hash beetween GUID of the accounts (hash) and their names (value).

		try {

		    // Set various parser options; validation off,
			// warnings shown, error stream set to stderr.
			parser.setErrorStream(System.err);
			parser.setValidationMode(DOMParser.NONVALIDATING);
			parser.showWarnings(true);

			// set the DTD
			monitor.beginTask("Reading the file...", 0);   
			URL urlDTD = this.getClass().getResource("ressources/gnucash.dtd");
			parser.parseDTD(urlDTD, "gnc-v2");
			parser.setDoctype(parser.getDoctype());
			
			// parse the document
			monitor.beginTask("Parsing the document...", 1);
			URL url = file.toURL();
			parser.parse(url);

			// Obtain the document
			XMLDocument doc = parser.getDocument();

			// Create the accounts
			monitor.beginTask("Creating the accounts...", 2);
			createAccounts(doc);

			// Create the transactions
			monitor.beginTask("Importing the transactions...", 3);
			createTransactions(doc);

		} catch (MalformedURLException e) {
			System.err.println(e.toString());
		} catch (IOException e) {
			System.err.println(e.toString());
		} catch (SAXException e) {
			System.err.println(e.toString());

		} catch (LessThanTwoSplitsException e) {
			System.err.println(e.toString());
		} catch (MoreThanTwoSplitsException e) {
			System.err.println(e.toString());
		} catch (ParseException e) {
			System.err.println(e.toString());

		} catch (Error e) {
		    System.err.println(e.getStackTrace());
		    throw e;
		} finally {
		    monitor.done();
		}
	}

	/**
	 * Export the session in a GnuCash-XML-File
	 * (NOT IMPLEMENTED!) TODO Faucheux
	 * @author=Olivier Faucheux
	 */
	public void exportAccount(Session session, Account account, File file) {
    	throw new RuntimeException("exportAccount for GnucashXML not implemented !");
	}

	/**
	 * Create the accounts. Each XML-Account has following structure:
	 *   <account version="2.0.0">
	 *     <name>Sorties culturelles</name>
	 *     <id type="guid">3ce87b30bc7b5fc69b7ccba0ddab4d72</id>
	 *     <type>EXPENSE</type>
	 *     <currency>
	 *       <space>ISO4217</space>
	 *       <id>EUR</id>
	 *     </currency>
	 *     <currency-scu>100</currency-scu>
	 *     <parent type="guid">210104bdd4a8a79fd297ea233e1966c9</parent>
	 *   </account>
	 * 
	 * 
	 * @param doc = handle to the file to read it has to have the form:
	 *   <?xml version="1.0"?>
	 *   <gnc-v2>
	 *   <something></something>
	 *   <account version="2.0.0">
	 *    <!-- Cf accounts -->
	 *   </account>
	 *   <account version="2.0.0">
	 *    <!-- Cf accounts -->
	 *   </account>
	 *   <something></something>
	 *   </gnc-v2>
	 * @author Olivier Faucheux
	 * 
	 * TODO: Faucheux
	 *  - treats the type (EXPENSE, BANK, CASH, CURRENCY, ...)
	 *  - treats the currency (for the time, the standard currency is always used)
	 *  - when (or if ever) jmoney accepts it, treats the parent.  
	 */
	private void createAccounts(XMLDocument doc) {
		Node node; // First child of <gnc-v2>
		Hashtable childParent = new Hashtable();

		// For each account of the file
		for (node = doc.getFirstChild().getNextSibling().getFirstChild().getNextSibling().getFirstChild();
			node != null;
			node = (Element) node.getNextSibling()) {

			// System.out.println("Node: " + node.getNodeName());
			if (node.getNodeName().compareToIgnoreCase("gnc:Account") == 0) {
				String accountName = null;
				String accountGUID = null;
				String parentGUID = null;
				String parentName = null;

				NodeList childNodes = node.getChildNodes();
				for (int j = 0; j < childNodes.getLength(); j++) {
					// System.out.println("  Subnode : " + childNodes.item(j).getNodeName() + ": " + childNodes.item(j).getFirstChild().getNodeValue());
					if (childNodes
						.item(j)
						.getNodeName()
						.equalsIgnoreCase("act:name")) {
						accountName =
							childNodes.item(j).getFirstChild().getNodeValue();
					} else if (
						childNodes.item(j).getNodeName().equalsIgnoreCase(
							"act:id")) {
						accountGUID =
							childNodes.item(j).getFirstChild().getNodeValue();
					} else if (
							childNodes.item(j).getNodeName().equalsIgnoreCase(
							"act:parent")) {
						parentGUID =
							childNodes.item(j).getFirstChild().getNodeValue();
					}
				}

				// Create the account
				if (accountName != null) {
					System.out.println("I'm creating the account >" + accountName + "< with guid >" + accountGUID + "<");
					MutableCapitalAccountImpl account = (MutableCapitalAccountImpl) session.createNewCapitalAccount();
					account.setName(accountName);
					accountsGUIDTable.put(accountGUID, account);
					if (parentGUID != null)
						childParent.put(accountGUID, parentGUID);
					account.commit();
				} else {
					System.err.println(
						"Error while importing: Account without any name found !");
				}
			}

		}

		// Now link childs and parents
		// System.out.println("Liaisons Parents-Childs");
		Enumeration e = childParent.keys();
		while (e.hasMoreElements()) {
			String childGUID = (String) e.nextElement();
			String parentGUID = (String) childParent.get(childGUID);
			//System.out.println("childGUID:" + childGUID);
			//System.out.println("parentGUID:" + parentGUID);
			MutableCapitalAccount child = (MutableCapitalAccount) getAccountFromGUID(childGUID);
			CapitalAccount parent = getAccountFromGUID(parentGUID);
			child.setParent(parent);
			// System.out.println("Level of >" + child.getName() + "<:" + child.getLevel());
		}

	}


  /**
   * Add all the transactions of the XML-File.
   * A transaction looks as following:
   * 
   * <transaction version="2.0.0">
   *   <id type="guid">66e591ba1b00dab33628d58390973e33</id>
   *   <date-posted>
   *     <date>2003-10-31 000000 +0000</date>
   *   </date-posted>
   *   <date-entered>
   *     <date>2003-11-03 070741 +0000</date>
   *   </date-entered>
   *   <description>Geldkarte</description>
   *   <splits>
   *     <split>
   *       <id type="guid">73fd69691319ea2872565aad65e26cde</id>
   *       <reconciled-state>n</reconciled-state>
   *       <value>-2600/100</value>
   *       <quantity>-2600/100</quantity>
   *       <account type="guid">c192cbb8d5980c690c0d44c188fede4b</account>
   *     </split>
   *     <split>
   *       <id type="guid">396f463aaea2482a4c80da8b1eb2bcfa</id>
   *       <reconciled-state>n</reconciled-state>
   *       <value>2600/100</value>
   *       <quantity>2600/100</quantity>
   *       <account type="guid">00a629b2ed01633286b2c9782a17757c</account>
   *     </split>
   *   </splits>
   * </transaction>
   *
   * TODO Faucheux:
   *  - can we store the "date-entered" in jmoney too?
   *  - when we have two "splits", it's a simple double Entry. When more, it's a splitted one. 
   *    For the time, only "simple double" Entries works.
   *  
   * @param doc
   * @throws MoreThanTwoSplitsException
   * @throws LessThanTwoSplitsException
   * @throws ParseException
   */
	private void createTransactions(XMLDocument doc)
		throws
			MoreThanTwoSplitsException,
			LessThanTwoSplitsException,
			ParseException {

		Entry e;
		Node transactionElement; /* Currently treated Transaction node */
		/* Currently treated property for the transaction */
		Element propertyElement;

		// For each Transaction of the XML file

		for (transactionElement =doc.getFirstChild().getNextSibling().getFirstChild().getNextSibling().getFirstChild();
			transactionElement != null;
			transactionElement =
				(Element) transactionElement.getNextSibling()) {

			// System.out.println("Node: " + transactionElement.getNodeName());

			if (transactionElement
				.getNodeName()
				.equalsIgnoreCase("gnc:transaction")) {

				// System.out.println("New Transaction");
				treatTransaction(transactionElement);
				
			}
		}
	}

	private long getLong(String s) {
		int posDivision = s.indexOf("/");
		long l1 = Long.parseLong(s.substring(0, posDivision));
		long l2 = Long.parseLong(s.substring(posDivision + 1));

		// TODO: Faucheux - understand why return (l1/l2) is not the good one;
		return l1;
		

	}

	/**
	 * Add a simple transaction. Simple transaction means here "double" one, but not splitted
	 * 
	 * @param transactionElement
	 * @throws ParseException
	 * @author Olivier Faucheux
	 */
	private void treatSimpleTransaction(Element propertyElement, MutableTransaction t)
		throws ParseException {

		String firstAccountName = null;
		String firstAccountGUID = null;
		String secondAccountName = null;
		String secondAccountGUID = null;
		String description = null; 
		XMLElement transactionNode;
		
		
		transactionNode = (XMLElement) propertyElement.getParentNode();
		
		try {
		description = 
		    transactionNode
			.getElementsByTagName("description")
			.item(0)
			.getFirstChild()
			.getNodeValue();
		} catch (NullPointerException e) { /* No description */ }
		
		Element firstAccoutElement =
			(Element) propertyElement.getElementsByTagName(
				"split").item(
				0);
		firstAccountGUID =
			firstAccoutElement
				.getElementsByTagName("account")
				.item(0)
				.getFirstChild()
				.getNodeValue();
		Account firstAccount = getAccountFromGUID(firstAccountGUID);
		if (firstAccount instanceof MutableAccount) firstAccount = ((MutableAccount) firstAccount).getRealAccount();
		Element secondAccoutElement =
			(Element) propertyElement.getElementsByTagName(
				"split").item(
				1);
		secondAccountGUID =
			secondAccoutElement
				.getElementsByTagName("account")
				.item(0)
				.getFirstChild()
				.getNodeValue();
		Account secondAccount = getAccountFromGUID(secondAccountGUID);
		if (secondAccount instanceof MutableAccount) secondAccount = ((MutableAccount) secondAccount).getRealAccount();

		String Value =
			firstAccoutElement
				.getElementsByTagName("value")
				.item(0)
				.getFirstChild()
				.getNodeValue();

		Entry e1 = t.createEntry();
		e1.setAmount(-getLong(Value));
		e1.setAccount(secondAccount);
		e1.setDescription(description);

		Entry e2 = t.createEntry();
		e2.setAmount(getLong(Value));
		e2.setAccount(firstAccount);
		e2.setDescription(description);

		// t.addEntry(e1);
		// t.addEntry(e2);
		
		// TODO: Faucheux to check
	}

	private void treatTransaction(Node transactionElement)
		throws ParseException {

		    MutableTransaction t = session.createNewTransaction();

			// For each property of the node
			for (Element propertyElement =
				(Element) transactionElement.getFirstChild();
				propertyElement != null;
				propertyElement = (Element) propertyElement.getNextSibling()) {
			    String transactionDescription = null;

				String propertyElementName = propertyElement.getNodeName();
				String propertyElementValue = 
					propertyElement.getFirstChild() == null
					? null
					: propertyElement.getFirstChild().getNodeValue();

				// System.out.println("New property : >" + propertyElementName + "<" + " Value >" + propertyElementValue + "<");

				if (propertyElementName
					.equalsIgnoreCase("trn:date-posted")) {
					t.setDate(
						gnucashDateFormat.parse(
							propertyElement
								.getFirstChild()
								.getFirstChild()
								.getNodeValue()));

				} else if (propertyElementName.equalsIgnoreCase("trn:description")) {
					transactionDescription = propertyElementValue;

				} else if (
						propertyElementName.equalsIgnoreCase("trn:splits")) {
						
						if (propertyElement.getElementsByTagName("split").getLength() < 2) {
							// TODO Faucheux
						} else if (propertyElement.getElementsByTagName("split").getLength() == 2) {
							treatSimpleTransaction(propertyElement, t);
						} else {
							// TODO Faucheux
						}

				}
				
			} // Treatement of properties
			
			t.commit();
	}
	
	
	
	
    public void exportAccount(Session session, CapitalAccount account, File file) {
    	throw new RuntimeException("exportAccount for GnucashXML not implemented !");
    };
    
    private CapitalAccount getAccountFromGUID(String GUID) {
        return (CapitalAccount) accountsGUIDTable.get(GUID);
    }

}