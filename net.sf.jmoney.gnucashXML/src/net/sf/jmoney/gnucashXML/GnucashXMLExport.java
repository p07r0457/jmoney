
package net.sf.jmoney.gnucashXML;

//all for xml
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
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
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * @author Faucheux
 */
public class GnucashXMLExport {

    private Session   session;
    private Document  doc;
    private Element	  documentRoot;
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
    public void export () {
        
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
        doc = impl.createDocument(null, "gnc-v2", null);
        documentRoot = doc.getDocumentElement();

        // Collect the information
        LinkedList accountList = new LinkedList ();
        Iterator itList = session.getAccountIterator();
        while (itList.hasNext()) 
            accountList.add ((Account) itList.next());
        LinkedList transactionList = new LinkedList ();
        Iterator itTransaction = session.getTransactionIterator();
        while (itTransaction.hasNext()) 
            transactionList.add ((Transaction) itTransaction.next());
        
        // Resume the content
        Integer numberAccounts = new Integer(accountList.size());
        Integer numberTransaction = new Integer (transactionList.size());
        Element element;
        // Number of accounts
        element = doc.createElement("count-data");
        element.setAttribute("type","account");
        element.appendChild(doc.createTextNode(numberAccounts.toString()));
        documentRoot.appendChild(element);
        // Number of transactions
        element = doc.createElement("count-data");
        element.setAttribute("type","transactions");
        element.appendChild(doc.createTextNode(numberTransaction.toString()));
        documentRoot.appendChild(element);

        // add the accounts
        for (int i=0; i<accountList.size(); i++)
            exportAccount((Account) accountList.get(i));
        
        // add the transactions
        
        // Print the result
        Transformer transformer = null;
        try {
            transformer = (TransformerFactory.newInstance()).newTransformer();
        } catch (Throwable t) { 
            t.printStackTrace(); 
        }
        
        Source input = new DOMSource(doc);
        Result output = new StreamResult(System.out);
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

        // create the entry for the account
        Element e = doc.createElement("account");
        e.setAttribute("version","2.0.0");
        
        // give the information of the account
        Element e2, e3;
        
        e2 = doc.createElement("name");
        e2.appendChild(doc.createTextNode(account.getName()));
        e.appendChild(e2);
        
        String guid = Integer.toHexString(account.hashCode());
        e2 = doc.createElement("id");
        e2.setAttribute("type", "guid");
        e2.appendChild(doc.createTextNode(guid));
        e.appendChild(e2);
        
        e2 = doc.createElement("type");
        e2.appendChild(doc.createTextNode("EXPENSE")); // TODO Faucheux - Change this
        e.appendChild(e2);
        
        e2 = doc.createElement("currency");
        e3 = doc.createElement("space");
        e3.appendChild(doc.createTextNode("ISO4217"));
        e2.appendChild(e3);
        e3 = doc.createElement("id");
        e3.appendChild(doc.createTextNode("EUR"));     // TODO Faucheux - Change this
        e2.appendChild(e3);
        e.appendChild(e2);

        if (account.getParent() != null ) {
            e2 = doc.createElement("parent");
            e2.setAttribute("type", "guid");
            e2.appendChild(doc.createTextNode(Integer.toHexString(account.getParent().hashCode()))); 
            e.appendChild(e2);
        }
       
        // add the account to the XML file
        documentRoot.appendChild(e);
    }
}
