/*
 * Created on 10 juil. 2004
 */

package net.sf.jmoney.gnucashXML;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.BankAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.dialogs.EventLoopProgressMonitor;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
			JMoneyPlugin.log(e);
		} catch (InterruptedException e) {
			JMoneyPlugin.log(e);
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
			URL urlDTD = this.getClass().getResource("resources/gnucash.dtd");
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

			// Commit the changes to the datastore
			session.registerUndoableChange(GnucashXMLPlugin.getResourceString("importDescription"));
		} catch (MalformedURLException e) {
			JMoneyPlugin.log(e);
		} catch (IOException e) {
			JMoneyPlugin.log(e);
		} catch (SAXException e) {
			JMoneyPlugin.log(e);
		} catch (LessThanTwoSplitsException e) {
			JMoneyPlugin.log(e);
		} catch (MoreThanTwoSplitsException e) {
			JMoneyPlugin.log(e);
		} catch (ParseException e) {
			JMoneyPlugin.log(e);
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
		final Hashtable childParent = new Hashtable();
		List accountToRecreate = new LinkedList();

		// For each account of the file
		for (node = doc.getFirstChild().getNextSibling().getFirstChild().getNextSibling().getFirstChild();
			node != null;
			node = (Element) node.getNextSibling()) {

			if (GnucashXMLPlugin.DEBUG) System.out.println("Node: " + node.getNodeName());
			if (node.getNodeName().compareToIgnoreCase("gnc:Account") == 0) {
				String accountName = null;
				String accountGUID = null;
				String parentGUID = null;
				String parentName = null;

				NodeList childNodes = node.getChildNodes();
				for (int j = 0; j < childNodes.getLength(); j++) {
					// if (GnucashXMLPlugin.DEBUG) System.out.println("  Subnode : " + childNodes.item(j).getNodeName() + ": " + childNodes.item(j).getFirstChild().getNodeValue());
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
					if (GnucashXMLPlugin.DEBUG) System.out.println("I'm creating the account >" + accountName + "< with guid >" + accountGUID + "<");
					CapitalAccount account = (CapitalAccount)session.createAccount(BankAccountInfo.getPropertySet());;
					account.setName(accountName);
					accountsGUIDTable.put(accountGUID, account);
					if (parentGUID != null) {
					    childParent.put(accountGUID, parentGUID);
					    accountToRecreate.add(accountGUID);
					}


				} else {
					JMoneyPlugin.log(new RuntimeException("Error while importing: Account without any name found!"));
				}
			}

		}

		// Now link childs and parents. We have to recreate the parents before the children.
		// Therefore, first sort the list
		Collections.sort(accountToRecreate, new Comparator () { 
		    public int compare(Object a, Object b) {
		        
		        String GUIDA = (String) a;
		        String GUIDB = (String) b;
		        String GUIDParentOfA = (String) childParent.get(GUIDA);
		        String GUIDParentOfB = (String) childParent.get(GUIDB);
		        
		        // case we compare two root-accounts
		        if (GUIDParentOfA == null & GUIDParentOfB == null) return 0;
		        
		        // case A is root account
		        if (GUIDParentOfA == null) return compare(GUIDA, GUIDParentOfB);
		        
		        // case B is root account
		        if (GUIDParentOfB == null) return compare(GUIDParentOfA, GUIDB);

		        // case neither A nor B are root accounts
		        if (GUIDA.equals(GUIDParentOfB)) return -1;
		        if (GUIDB.equals(GUIDParentOfA)) return 1;
		        
		        return compare (GUIDParentOfA, GUIDB);
		    }
		});
		
		// Now recreate some accounts.
		Iterator e = accountToRecreate.iterator();
		while (e.hasNext()) {
			String childGUID = (String) e.next();
			String parentGUID = (String) childParent.get(childGUID);
			String childName;
			// if (GnucashXMLPlugin.DEBUG) System.out.println("childGUID:" + childGUID);
			// if (GnucashXMLPlugin.DEBUG) System.out.println("parentGUID:" + parentGUID);
			CapitalAccount child = (CapitalAccount) getAccountFromGUID(childGUID);
			CapitalAccount parent = (CapitalAccount) getAccountFromGUID(parentGUID);
			
			session.deleteAccount(child);
			CapitalAccount newChild = (CapitalAccount) parent.createSubAccount(BankAccountInfo.getPropertySet());
			accountsGUIDTable.remove(childGUID);
			accountsGUIDTable.put(childGUID, newChild);
			
			newChild.setName(child.getName());
			if (GnucashXMLPlugin.DEBUG) System.out.println("Child: " + newChild + ", Parent: " + parent);
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

			if (GnucashXMLPlugin.DEBUG) System.out.println("Node: " + transactionElement.getNodeName());

			if (transactionElement
				.getNodeName()
				.equalsIgnoreCase("gnc:transaction")) {

				// if (GnucashXMLPlugin.DEBUG) System.out.println("New Transaction");
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
	private void treatSimpleTransaction(Element propertyElement, Transaction t)
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

		    Transaction t = session.createTransaction();

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

				// if (GnucashXMLPlugin.DEBUG) System.out.println("New property : >" + propertyElementName + "<" + " Value >" + propertyElementValue + "<");

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
						    treatSplittedTransaction(propertyElement, t);
						} else {
						    treatSplittedTransaction(propertyElement, t);
						}

				}
				
			} // Treatement of properties

	}
	
	
	
	
    public void exportAccount(Session session, CapitalAccount account, File file) {
    	throw new RuntimeException("exportAccount for GnucashXML not implemented !");
    };
    
    private Account getAccountFromGUID(String GUID) {
        // if (GnucashXMLPlugin.DEBUG) System.out.println("Looking for an account with the GUID " + GUID);
        return (Account) accountsGUIDTable.get(GUID);
    }

    
    
	/**
	 * Add a splitted transaction.
	 * A splitted transaction is a transaction with more than two entries 
	 * 
	 * @param transactionElement
	 * @throws ParseException
	 * @author Olivier Faucheux
	 */
	private void treatSplittedTransaction(Element propertyElement, Transaction t)
		throws ParseException {

		String accountName = null;
		String accountGUID = null;
		String transactionDescription = null;
		XMLElement transactionNode;
		NodeList entriesNodes = null;
		
		transactionNode = (XMLElement) propertyElement.getParentNode();
		
		try {
		transactionDescription = 
		    transactionNode
			.getElementsByTagName("description")
			.item(0)
			.getFirstChild()
			.getNodeValue();
		} catch (NullPointerException e) { /* No description */ }
		
		
		entriesNodes = propertyElement.getElementsByTagName("split");
		for (int i = 0; i<entriesNodes.getLength(); i++) {

		    Element accountElement = (Element)entriesNodes.item(i);
			accountGUID =
				accountElement
					.getElementsByTagName("account")
					.item(0)
					.getFirstChild()
					.getNodeValue();

			String value = accountElement
				.getElementsByTagName("value")
				.item(0)
				.getFirstChild()
				.getNodeValue();

			if (getLong(value) != 0) {
			    // Yes, I found entries with an amount = 0 and as accountGUID "0000000000000000000"
			    // I have to protect against it --  Faucheux
				Account account = getAccountFromGUID(accountGUID);
				Entry e = t.createEntry();
				e.setAmount(getLong(value));
				e.setAccount(account);
				e.setDescription(transactionDescription);
				
			    // if (GnucashXMLPlugin.DEBUG) System.out.println("Added amount: " + getLong(value) + " for " + account.toString() + " for >" + transactionDescription + "<" );
			}

		}
		
		// TODO: Faucheux to check
	}


}