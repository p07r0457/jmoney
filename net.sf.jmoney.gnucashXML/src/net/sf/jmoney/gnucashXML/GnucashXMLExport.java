package net.sf.jmoney.gnucashXML;

//all for xml
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.crimson.tree.XmlDocument;
import org.apache.crimson.tree.XmlDocumentBuilder;
import org.apache.xalan.templates.OutputProperties;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.sun.corba.se.internal.javax.rmi.CORBA.Util;

/**
* @author Faucheux
*/
public class GnucashXMLExport {

  private Session   session;
  private Document  doc;
  private Element	  bookElement;
	DateFormat gnucashDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
  /**
   * 
   */
  public GnucashXMLExport(Session session) {
      this.session = session;
  }

	/**
	 * export of the GnuCash File
	 * For the structure of the XML-File, @see GnucashXML
	 */
  public void export (String toFile) {
      
      // Prepare the tools
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder parser = null;
      DOMImplementation impl = null;
      try {
          parser = factory.newDocumentBuilder();
          impl   = parser.getDOMImplementation();
      } catch (ParserConfigurationException e) {
          e.printStackTrace();
      }
      
      // Create the document
      Element	  documentRoot;
      doc = impl.createDocument(null, "gnc-v2", null);
      documentRoot = doc.getDocumentElement();

      // Collect the information
      LinkedList accountList = new LinkedList ();
      Iterator itList = session.getAllAccounts().iterator();
      while (itList.hasNext()) 
          accountList.add ((Account) itList.next());
      LinkedList transactionList = new LinkedList ();
      Iterator itTransaction = session.getTransactionIterator();
      while (itTransaction.hasNext()) 
          transactionList.add ((Transaction) itTransaction.next());
      
      // Create the header
      Element e1 = doc.createElement("gnc:count-data");
      documentRoot.appendChild(e1);
      
      bookElement = doc.createElement("gnc:book");
      documentRoot.appendChild(bookElement);

      e1 = doc.createElement("book:id");
      bookElement.appendChild(e1);

      e1 = doc.createElement("book:slots");
      bookElement.appendChild(e1);

      
      // Resume the content
      Integer numberAccounts = new Integer(accountList.size());
      Integer numberTransaction = new Integer (transactionList.size());
      Element element;
      // Number of accounts
      element = doc.createElement("gnc:count-data");
      element.setAttribute("type","account");
      element.appendChild(doc.createTextNode(numberAccounts.toString()));
      bookElement.appendChild(element);
      // Number of transactions
      element = doc.createElement("gnc:count-data");
      element.setAttribute("type","transaction");
      element.appendChild(doc.createTextNode(numberTransaction.toString()));
      bookElement.appendChild(element);

      // add each account
      for (int i=0; i<accountList.size(); i++)
          exportAccount((Account) accountList.get(i));
      
      // add each transaction
      for (int i=0; i<transactionList.size(); i++)
          exportTransaction((Transaction) transactionList.get(i));
      
      // Prepare the output of the result
      Transformer transformer = null;
      try {
          transformer = (TransformerFactory.newInstance()).newTransformer();
      } catch (Throwable t) { 
          t.printStackTrace(); 
      }
      
      //
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputProperties.S_KEY_INDENT_AMOUNT, "2"); 
      
      Source input = new DOMSource(doc);
      Result output = new StreamResult(new File(toFile));
      try {
          transformer.transform(input, output);
      } catch (TransformerException e) {
          e.printStackTrace();
      }


  }
  /*
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
  
  
  /**
   * 
   * @author Faucheux
   */
  void exportAccount (Account account) {
      System.out.println("Export account " + account.getFullAccountName());

      // create the entry for the account
      Element e = doc.createElement("gnc:account");
      e.setAttribute("version","2.0.0");
      
      // give the information of the account
      Element e2, e3;
      
      e2 = doc.createElement("act:name");
      e2.appendChild(doc.createTextNode(account.getName()));
      e.appendChild(e2);
      
      String guid = Integer.toHexString(account.hashCode());
      e2 = doc.createElement("act:id");
      e2.setAttribute("type", "guid");
      e2.appendChild(doc.createTextNode(guid));
      e.appendChild(e2);
      
      e2 = doc.createElement("act:type");
      e2.appendChild(doc.createTextNode("EXPENSE")); // TODO Faucheux - Change this
      e.appendChild(e2);
      
      e2 = doc.createElement("act:currency");
      e3 = doc.createElement("cmdty:space");
      e3.appendChild(doc.createTextNode("cmdty:ISO4217"));
      e2.appendChild(e3);
      e3 = doc.createElement("cmdty:id");
      e3.appendChild(doc.createTextNode("EUR"));     // TODO Faucheux - Change this
      e2.appendChild(e3);
      e.appendChild(e2);

      if (account.getParent() != null ) {
          e2 = doc.createElement("act:parent");
          e2.setAttribute("type", "guid");
          e2.appendChild(doc.createTextNode(Integer.toHexString(account.getParent().hashCode()))); 
          e.appendChild(e2);
      }
     
      // add the account to the XML file
      bookElement.appendChild(e);
  }
  
  /**
   * Add the transaction to the document
   * @author Faucheux
   */
  void exportTransaction (Transaction transaction) {
      System.out.println("Export transaction " + transaction.hashCode());

      // create the entry for the transaction
      Element e = doc.createElement("gnc:transaction");
      e.setAttribute("version","2.0.0");
      
      // give the information of the account
      Element e1, e2, e3;
      
      String guid = Integer.toHexString(transaction.hashCode());
      e2 = doc.createElement("trn:id");
      e2.setAttribute("type", "guid");
      e2.appendChild(doc.createTextNode(guid));
      e.appendChild(e2);
      
      e2 = doc.createElement("trn:date-posted");
      addDate(e2, transaction.getDate());
      e.appendChild(e2);
      
      e2 = doc.createElement("trn:date-entered");
      addDate(e2, (new Date(/* Now */)));  // TODO: add the property;
      e.appendChild(e2);

      e2 = doc.createElement("trn:description");
      e2.appendChild(doc.createTextNode(getDescription(transaction)));
      e.appendChild(e2);

      e2 = doc.createElement("trn:splits");
      Iterator entryIt = transaction.getEntryIterator();
      while (entryIt.hasNext()) {
          Entry entry = (Entry) entryIt.next();
          exportEntry (e2, entry);
      }
      e.appendChild(e2);

      // add the account to the XML file
      bookElement.appendChild(e);
  }

  
  /**
   * Add the information of the entry to the "splits" XML-Element of the transaction. 
   * @param splitsElement
   * @param entry
   * @author Olivier Faucheux
   */
  void exportEntry (Element splitsElement, Entry entry) {
      System.out.println("Export entry " + entry.hashCode());

      Element entryElement;
      Element e;

      // create the element of the entry and add it the the transaction
      entryElement = doc.createElement("trn:split");
      splitsElement.appendChild(entryElement);

      // full the properties of this event
      
      String guid = Integer.toHexString(entry.hashCode());
      e = doc.createElement("split:id");
      e.setAttribute("type", "guid");
      e.appendChild(doc.createTextNode(guid));
      entryElement.appendChild(e);

      e = doc.createElement("split:reconciled-state");
      e.appendChild(doc.createTextNode("n"));					// TODO - Faucheux: No idea what it is.
      entryElement.appendChild(e);
      
      e = doc.createElement("split:value");
      String s = entry.getAmount() + "/" + entry.getCommodity().getScaleFactor();
      e.appendChild(doc.createTextNode(s));				
      entryElement.appendChild(e);
      
      e = doc.createElement("split:quantity");
      s = entry.getAmount() + "/" + entry.getCommodity().getScaleFactor();
      e.appendChild(doc.createTextNode(s));				
      entryElement.appendChild(e);
      
      e = doc.createElement("split:account");
      guid = Integer.toHexString(entry.getAccount().hashCode());
      e.setAttribute("type", "guid");
      e.appendChild(doc.createTextNode(guid));
      entryElement.appendChild(e);
      
  }

  
  private void addDate (Element e, Date date) {
      Element d = doc.createElement("ts:date");
      d.appendChild(doc.createTextNode(gnucashDateFormat.format(date)));
      e.appendChild(d);
  }
  
  /**
   * Look the description of each Entry of the transaction to determine
   * which description the transaction has to have.
   * @param t the transaction
   * @return a description
   * @author Olivier Faucheux
   */
  private String getDescription (Transaction t) {
      String s = null;
      Iterator it = t.getEntryIterator();
      while (it.hasNext()) {
          Entry e = (Entry) it.next();
          if (s == null)
              s = e.getDescription();
          else if (e.getDescription() != s) 
              s = new String ("Splitted!");
      }

      if (s==null) s = new String ("No Entry");
      
      return s;
  }
}

