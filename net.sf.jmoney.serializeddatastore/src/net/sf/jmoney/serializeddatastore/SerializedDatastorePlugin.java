package net.sf.jmoney.serializeddatastore;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.sf.jmoney.model2.*;
import net.sf.jmoney.JMoneyPlugin;

//SAX classes.
import org.xml.sax.helpers.*;
//JAXP 1.1
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;

/** @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
/**
 * The main plugin class to be used in the desktop.
 */
public class SerializedDatastorePlugin extends AbstractUIPlugin {
	//The shared instance.
	private static SerializedDatastorePlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public SerializedDatastorePlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle   = ResourceBundle.getBundle("net.sf.jmoney.serializeddatastore.Language");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static SerializedDatastorePlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	
	public static String getResourceString(String key) {
		ResourceBundle bundle = SerializedDatastorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	/**
	 * Called when the default startup session is to be created.
	 *
	 * Normally sessions are created in response to menu action items
	 * added by the datastore plug-in.  However, when the user exits
	 * from the framework, information about the current session is
	 * saved and the same datastore is re-loaded when the framework
	 * is restarted.
	 */
	// TODO remove this?
	public void openDefaultSession(Properties defaultSessionProperties, IWorkbenchWindow window) {
		String sessionFileName = defaultSessionProperties.getProperty("file");
		File sessionFile = new File(sessionFileName);
		SessionImpl session = readSession(sessionFile, window);
		JMoneyPlugin.getDefault().setSession(session);
	}
	
	/**
	 * Read session from file.
	 */
	public SessionImpl readSession(File sessionFile, IWorkbenchWindow window) {
		SessionImpl result;
		
		String title = JMoneyPlugin.getResourceString("Dialog.Wait.Title");
		String message = SerializedDatastorePlugin.getResourceString("MainFrame.OpeningFile")
		+ " "
		+ sessionFile;
		
		ProgressBar progressBar = new ProgressBar(
				window.getShell(), SWT.HORIZONTAL);
		
		// Show the progress bar at 0 in a range of 0 to 100.
		// TODO Update the progress bar as the input file is read.
		// This can be done by writing a wrapper around the input stream class.
		// This wrapper updates the progress bar as data is read from the input
		// stream.
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setSelection(0);
		
		/*        
		 MessageDialog waitDialog =
		 new MessageDialog(
		 window.getShell(), 
		 title, 
		 null, // accept the default window icon
		 message, 
		 MessageDialog.INFORMATION, 
		 new String[] { IDialogConstants.OK_LABEL }, 0);
		 waitDialog.open();
		 */					
		
		try {
			result = readSessionQuietly(sessionFile);
			//          waitDialog.close();
			progressBar.dispose();
		} catch (Exception ex) {
			//  		waitDialog.close();
			progressBar.dispose();
			fileReadError(sessionFile, window);
			result = null;
		}
		
		return result;
	}
	
	public SessionImpl readSessionQuietly(File sessionFile) throws FileNotFoundException, IOException, CoreException {
		SessionImpl result;
		
		FileInputStream fin = new FileInputStream(sessionFile);
		// Patch to aid with testing.  If the extension is 'xml'
		// then assume the xml is not compressed.
		GZIPInputStream gin = null;
		BufferedInputStream bin;
		if (sessionFile.getName().endsWith(".xml")) {
			bin = new BufferedInputStream(fin);
		} else {
			gin = new GZIPInputStream(fin);
			bin = new BufferedInputStream(gin);
		}

		// First attempt to read the XML as though it is in the
		// current format.
		
		Object newSession;
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			idToCurrencyMap = new HashMap();
			idToAccountMap = new HashMap();
			currentSAXEventProcessor = null;
			
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			SAXParser saxParser = factory.newSAXParser();
			HandlerForObject handler = new HandlerForObject();
			saxParser.parse(bin, handler); 
			newSession = handler.getSession();
		} 
		catch (ParserConfigurationException e) {
			throw new RuntimeException("Serious XML parser configuration error");
		} 
		catch (SAXException se) {
			// This exception will be throw if the file is old format (0.4.5 or prior).
			// Read as an old format file.
			
			// First close and re-open the file.
			bin.close();
			if (gin != null) gin.close();
			fin.close();
			
			fin = new FileInputStream(sessionFile);
			// Patch to aid with testing.  If the extension is 'xml'
			// then assume the xml is not compressed.
			if (sessionFile.getName().endsWith(".xml")) {
				bin = new BufferedInputStream(fin);
			} else {
				gin = new GZIPInputStream(fin);
				bin = new BufferedInputStream(gin);
			}

			// The XMLDecoder must use the same classpath that was used to load this class.
			// The classpath set in this thread is the system class path, and if that
			// is used then XMLDecoder will not be able to find the classes specified
			// in the XML.  We must therefore temporarily replace the classpath.
			ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.getDescriptor().getPluginClassLoader());
			XMLDecoder dec = new XMLDecoder(bin);
			newSession = dec.readObject();
			dec.close();
			Thread.currentThread().setContextClassLoader(originalClassLoader);            
		}
		catch (IOException ioe) { 
			throw new RuntimeException("IO internal exception error");
		} finally {
			bin.close();
			if (gin != null) gin.close();
			fin.close();
		}
		
		if (newSession instanceof net.sf.jmoney.model2.Session) {
			result = (SessionImpl)newSession;
			setRedundantReferences(result);
		} else if  (newSession instanceof net.sf.jmoney.model.Session) {
			SessionImpl newSessionNewFormat = new SessionImpl();
			convertModelOneFormat((net.sf.jmoney.model.Session)newSession, newSessionNewFormat);
			result = newSessionNewFormat;
		} else {
			throw new CoreException(
					new Status(Status.ERROR, "net.sf.jmoney.serializeddatastore", Status.OK,
							"session object deserialized, but the object was not a session!",
							null));	
		}
		
		result.setFileA(sessionFile);
		
		return result;
	}
	
	private class HandlerForObject extends DefaultHandler {
		
		/**
		 * The top level session object.
		 */
		private SessionImpl session;
		
		HandlerForObject() {
			
		}
		
		SessionImpl getSession() {
			return session;
		}
		
		/**
		 * Receive notification of the start of an element.
		 *
		 * <p>See if there is a setter for this element name.  If there is
		 * then set the setter.  Otherwise set the setter to null to indicate
		 * that any character data should be ignored.
		 * </p>
		 * @param name The element type name.
		 * @param attributes The specified or defaulted attributes.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#startElement
		 */
		public void startElement(String uri, String localName,
				String qName, Attributes attributes)
		throws SAXException {
			if (currentSAXEventProcessor == null) {
				if (!localName.equals("session")) {
					throw new SAXException(
					"only element 'session' is allowed at top level");
				}
				
				session = new SessionImpl();
				currentSAXEventProcessor = new ObjectProcessor(null, session, "net.sf.jmoney.session");
			} else {
				currentSAXEventProcessor.startElement(uri, localName, attributes);
			}
		}
		
		
		/**
		 * Receive notification of the end of an element.
		 *
		 * <p>Set the property accessor back to null.
		 * </p>
		 * @param name The element type name.
		 * @param attributes The specified or defaulted attributes.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#endElement
		 */
		public void endElement(String uri, String localName, String qName)
		throws SAXException {
			currentSAXEventProcessor = currentSAXEventProcessor.endElement();
		}
		
		
		/**
		 * Receive notification of character data inside an element.
		 *
		 * <p>If a setter method is set then the character data is passed
		 * to the setter.  Otherwise the character data is dropped.
		 * </p>
		 * @param ch The characters.
		 * @param start The start position in the character array.
		 * @param length The number of characters to use from the
		 *               character array.
		 * @exception org.xml.sax.SAXException Any SAX exception, possibly
		 *            wrapping another exception.
		 * @see org.xml.sax.ContentHandler#characters
		 */
		public void characters(char ch[], int start, int length)
		throws SAXException {
			if (currentSAXEventProcessor == null) {
				throw new SAXException("data outside top element is illegal");
			}
			
			currentSAXEventProcessor.characters(ch, start, length);
		}
	}
	
	abstract private class SAXEventProcessor {
		protected SAXEventProcessor parent;
		
		/**
		 * Creates a new SAXEventProcessor object.
		 *
		 * @param parent The event processor that was in effect.  This newly
		 *        created event processor will take over and will process the
		 *        contents of an element.  When the end tag for the element is
		 *        found then this original event processor must be restored as
		 *        the active event processor.
		 */
		SAXEventProcessor(SAXEventProcessor parent) {
			this.parent = parent;
		}
		
		/**
		 * DOCUMENT ME!
		 *
		 * @param name DOCUMENT ME!
		 * @param atts DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public abstract void startElement(String uri, String localName, Attributes atts)	throws SAXException;
		
		/**
		 * DOCUMENT ME!
		 *
		 * @param ch DOCUMENT ME!
		 * @param start DOCUMENT ME!
		 * @param length DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public void characters(char ch[], int start, int length)
		throws SAXException {
			/* ignore data by default */
		}
		
		/**
		 * DOCUMENT ME!
		 *
		 * @param name DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public SAXEventProcessor endElement()
		throws SAXException {
			return parent;
		}

		/**
		 * This method is called each time the next processor down
		 * the stack processed an 'end element' and returned control
		 * so this processor.
		 * <P>
		 * The processor below will pass the object that it had
		 * read in.  This object might be a scalar property value
		 * or an object that itself has properties. 
		 */
		public void elementCompleted(Object value) {
			// The default processing assumes that no inner
			// element processors will ever pass values back.
			// If inner elements may pass up values then this
			// method must be overridden.
			throw new RuntimeException("Value passed back from inner element but no value expected.");
		}
	}

	/**
	 * An event processor that takes over processing
	 * while we are inside an object.  The processor looks for
	 * elements representing the properties of the object.
	 */
	private class ObjectProcessor extends SAXEventProcessor {
		// This is a bit of a kludge.  We know that all of our
		// own objects are implemented using the
		// ExtendableObjectHelperImpl helper class,
		// so this gives us access to the methods in this class.
		// In particular, this class supports the setPropertyValue
		// method even though this method is not exposed through
		// the interfaces for the non-mutable objects.
		ExtendableObjectHelperImpl extendableObject;
		
		PropertySet propertySet;
		
		/**
		 * If we have processed the start of an element representing
		 * a property but have not yet processed the end of the element
		 * then this field is the property accessor.  Otherwise this
		 * field is null.
		 * One or other but not both of the following can be non-null.
		 */
		PropertyAccessor propertyAccessor = null;
		String listType = null;
		String propertyName = null;
		
		/**
		 *
		 * @param parent The event processor that was in effect.  This newly
		 *        created event processor will take over and will process the
		 *        contents of an element.  When the end tag for the element is
		 *        found then this original event processor must be restored as
		 *        the active event processor.
		 */
		ObjectProcessor(SAXEventProcessor parent, ExtendableObjectHelperImpl object, String propertySetId) {
			super(parent);
			
			this.extendableObject = object;
			
			try {
				this.propertySet = PropertySet.getPropertySet(propertySetId);
			} catch (PropertySetNotFoundException e) {
				throw new RuntimeException("internal error");
			}
		}
		
		/**
		 * DOCUMENT ME!
		 *
		 * @param name DOCUMENT ME!
		 * @param atts DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public void startElement(String uri, String localName, Attributes atts)
		throws SAXException {
			
			// We must set up either:
			// - propertyAccessor
			// - listType and propertyName
		
			// All elements are expected to be in a namespace beginning
			// "http://jmoney.sf.net".  If the element is a property in an
			// extension property set then the id of the extension property
			// set will be appended.
			
			propertyName = localName;
			String namespace = null;
			if (uri.length() == 20) {
				namespace = null;
			} else {
				namespace = uri.substring(21);
			}
			listType = null;
			
			try {
				// Find the property accessor for this property.
				// The property may be in the property set for the
				// object or in the property set for any base objects,
				// or may be in extensions of this or any base object.
				// If no namespace is specified then we search only
				// this and the base property sets.
				
				if (namespace == null) {
					// Search this property set and base property sets,
					// but exclude extensions.  
					propertyAccessor = propertySet.getPropertyAccessorGivenLocalNameAndExcludingExtensions(propertyName);
				} else {
					// Get the property based on the fully qualified name.
					// TODO this is not very efficient.  We combine the propertySetId
					// and the local name, but the first thing the following method
					// does is to split them apart again!
					propertyAccessor = PropertySet.getPropertyAccessor(namespace + "." + propertyName);
				}
			} catch (PropertyNotFoundException e) {
				// See if this is in fact a list, as lists are not
				// currently registered.
			
				// Must set to null if a list.
				propertyAccessor = null;
				
				if (propertySet.getId().equals("net.sf.jmoney.session")
						&& propertyName.equals("commodity")) {
					listType = "net.sf.jmoney.commodity";
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.session")
						&& propertyName.equals("account")) {
					listType = "net.sf.jmoney.account";
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.session")
						&& propertyName.equals("transaction")) {
					listType = "net.sf.jmoney.transaction";
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.capitalAccount")
						&& propertyName.equals("subAccount")) {
					listType = "net.sf.jmoney.capitalAccount";
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.categoryAccount")
						&& propertyName.equals("subAccount")) {
					listType = "net.sf.jmoney.categoryAccount";
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.transaction")
						&& propertyName.equals("entry")) {
					listType = "net.sf.jmoney.entry";
				}
				
				if (listType == null) {
					// The property no longer exists.
					// TODO: Log this.  When changing the properties,
					// one is supposed to provide upgrader properties
					// for all obsoleted properties.
					// We drop the value.
					// Ignore content
					currentSAXEventProcessor = new IgnoreElementProcessor(this, null);
					return;
				}
			}
			
			Class propertyClass;
			if (propertyAccessor != null) {
				propertyClass = propertyAccessor.getValueClass();
			} else {
				try {
					propertyClass = PropertySet.getPropertySet(listType).getInterfaceClass();
				} catch (PropertySetNotFoundException e) {
					throw new RuntimeException("internal error");
				}
			}
			
			// See if the 'idref' attribute is specified.
			String idref = atts.getValue("idref");
			if (idref != null) {
				Object value;
				
				if (propertyClass == Currency.class) {
					value = idToCurrencyMap.get(idref);
				} else if (propertyClass == Account.class) {
					value = idToAccountMap.get(idref);
				} else {
					throw new RuntimeException("bad XML file");
				}

				// Process this element.
				// Although we already have all the data we need
				// from the start element, we still need a processor
				// to process it.
				// Ideally we should create another processor which
				// gives errors if there is any additional data.

				// We pass the value to the processor so that it can pass the value
				// back to us!  (That is the design - it is up to the inner processor
				// to supply the value.  It just so happens in the case of an idref
				// that we know the value before we even create the inner processor
				// to process the content).
				
				currentSAXEventProcessor = new IgnoreElementProcessor(this, value);
			} else {
				if (!propertyClass.isPrimitive() && propertyClass != String.class && propertyClass != Date.class) {
					String propertySetId = atts.getValue("propertySet");
					if (propertySetId == null) {
						// TODO: following call is overkill, because
						// the class should be an exact match, not derived
						// or anything.
						propertySetId = PropertySet.getPropertySet(propertyClass).getId();
					}
					
					ExtendableObjectHelperImpl propertyValueObject = null;
					
					if (propertySetId.equals("net.sf.jmoney.session")) {
						propertyValueObject = new SessionImpl();
					} else if (propertySetId.equals("net.sf.jmoney.currency")) {
						propertyValueObject = new CurrencyImpl();
						idToCurrencyMap.put(atts.getValue("id"), propertyValueObject);
					} else if (propertySetId.equals("net.sf.jmoney.capitalAccount")) {
						propertyValueObject = new CapitalAccountImpl();
						idToAccountMap.put(atts.getValue("id"), propertyValueObject);
					} else if (propertySetId.equals("net.sf.jmoney.categoryAccount")) {
						propertyValueObject = new IncomeExpenseAccountImpl();
						idToAccountMap.put(atts.getValue("id"), propertyValueObject);
					} else if (propertySetId.equals("net.sf.jmoney.transaction")) {
						propertyValueObject = new TransaxionImpl();
					} else if (propertySetId.equals("net.sf.jmoney.entry")) {
						propertyValueObject = new EntryImpl();
					} 
					
					currentSAXEventProcessor = new ObjectProcessor(this, propertyValueObject, propertySetId);
				} else {
					// Property class is primative or Date
					currentSAXEventProcessor = new PropertyProcessor(this, propertyClass);
				}
			}
		}
			
		/**
		 * DOCUMENT ME!
		 *
		 * @param ch DOCUMENT ME!
		 * @param start DOCUMENT ME!
		 * @param length DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public void characters(char ch[], int start, int length) {
			for (int i = start; i < start + length; i++ ) {
				if (ch[i] != ' ' && ch[i] != '\n' && ch[i] != '\t') {
					throw new RuntimeException("unexpected character data found.");
				}
			}
		}

		public SAXEventProcessor endElement()
		throws SAXException {
			// Pass the value back up to the outer element processor.
			if (parent != null) {
				parent.elementCompleted(extendableObject);
			}
			
			return parent;
		}

		/**
		 * The inner element processor has returned a value to us.
		 * We now set the value into the apppropriate property or
		 * add it to the appropriate list.
		 */
		public void elementCompleted(Object value) {
			// Now we have the value of this property.
			// If it is null, something is wrong.
			if (value == null) {
				throw new RuntimeException("null value");
			}
			
			// Set the value in our object.  If the property
			// is a list property then the object is added to
			// the list.
			if (propertyAccessor != null) {
				extendableObject.setPropertyValue(propertyAccessor, value);
			} else {
				// Must be an element in an array.
				if (propertySet.getId().equals("net.sf.jmoney.session")
						&& propertyName.equals("commodity")) {
					((SessionImpl)extendableObject).addCommodity((Commodity)value);
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.session")
						&& propertyName.equals("account")) {
					((SessionImpl)extendableObject).addAccount((Account)value);
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.session")
						&& propertyName.equals("transaction")) {
					((SessionImpl)extendableObject).addTransaxion((Transaxion)value);
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.capitalAccount")
						&& propertyName.equals("subAccount")) {
					((CapitalAccountImpl)extendableObject).addSubAccount((CapitalAccount)value);
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.categoryAccount")
						&& propertyName.equals("subAccount")) {
					((IncomeExpenseAccountImpl)extendableObject).addSubAccount((IncomeExpenseAccount)value);
				} else
				
				if (propertySet.getId().equals("net.sf.jmoney.transaction")
						&& propertyName.equals("entry")) {
					((TransaxionImpl)extendableObject).addEntry((Entry)value);
				}
				
			}
		}
	}

	/**
	 * An event processor that takes over processing
	 * while we are inside a scalar property.  
	 * The processor looks for character content of the element
	 * giving the value of the property.
	 */
	private class PropertyProcessor extends SAXEventProcessor {
		/**
		 * Class of the property we are expecting.
		 * This may be a primative or Date.
		 */
		Class propertyClass;
		
		/**
		 * Value of the property to be returned to the outer
		 * processor.
		 */
		Object value = null;
		
		/**
		 *
		 * @param parent The event processor that was in effect.  This newly
		 *        created event processor will take over and will process the
		 *        contents of an element.  When the end tag for the element is
		 *        found then this original event processor must be restored as
		 *        the active event processor.
		 */
		PropertyProcessor(SAXEventProcessor parent, Class propertyClass) {
			super(parent);
			this.propertyClass = propertyClass;
		}
		
		/**
		 * DOCUMENT ME!
		 *
		 * @param name DOCUMENT ME!
		 * @param atts DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public void startElement(String uri, String localName, Attributes atts)
		throws SAXException {
			throw new SAXException("element not expected inside scalar property");
		}
			
		/**
		 * DOCUMENT ME!
		 *
		 * @param ch DOCUMENT ME!
		 * @param start DOCUMENT ME!
		 * @param length DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public void characters(char ch[], int start, int length)
		throws SAXException {
			// TODO: change this.  Find a constructor from string.
			if (propertyClass.equals(int.class)) {
				String s = new String(ch, start, length);
				value = new Integer(s);
			} else if (propertyClass.equals(long.class) || propertyClass.equals(Long.class)) {
				String s = new String(ch, start, length);
				value = new Long(s);
			} else if (propertyClass.equals(String.class)) {
				value = new String(ch, start, length);
			} else if (propertyClass.equals(char.class)) {
				value = new Character(ch[start]);
			} else if (propertyClass.equals(Date.class)) {
//				String s = new String(ch, start, length);
//				String numbers[] = s.split("\.");
				
				// above function does not work, easier to do it ourselves than
				// to find the doc. for the above function.
				int i = start;
				while (ch[i] != '.') i++;
				int year = new Integer(new String(ch, start, i-start)).intValue();
				int newStart = i + 1;
				i = newStart;
				while (ch[i] != '.') i++;
				int month = new Integer(new String(ch, newStart, i-newStart)).intValue();
				int day = new Integer(new String(ch, i+1, (start+length)-(i+1))).intValue();
				
//				int year = new Integer(numbers[0]).intValue();
//				int month = new Integer(numbers[1]).intValue();
//				int day = new Integer(numbers[2]).intValue();

				value = new Date(year - 1900, month, day);
			} else {
				throw new RuntimeException("unsupported type");
			}
		}

		public SAXEventProcessor endElement()
		throws SAXException {
			// Pass the value back up to the outer element processor.
			parent.elementCompleted(value);
			
			return parent;
		}
	}


	/**
	 * Process events that occur within any element for which we are not
	 * interested in the contents.
	 * <P>
	 * This class does double duty.  It processes both elements with an idref
	 * and elements for which we know nothing about the object.  In the former
	 * case, a non-null value is passed to the constructor which is passed back
	 * as the value of this element.  In the latter case a null value is passed.
	 */
	private class IgnoreElementProcessor extends SAXEventProcessor {
		private Object value;
		
		/**
		 * Creates a new IgnoreElementProcessor object.
		 *
		 * @param parent DOCUMENT ME!
		 * @param elementName DOCUMENT ME!
		 */
		IgnoreElementProcessor(SAXEventProcessor parent, Object value) {
			super(parent);
			this.value = value;
		}
		
		/**
		 * Process elements that occur within an element for which we are
		 * ignoring content.
		 *
		 * @param name The name of the element found inside the element.
		 * @param atts A map object that contains the names and values of all
		 *        the attributes for the element.
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public void startElement(String uri, String localName, Attributes atts)
		throws SAXException {
			// If we are ignoring an element, also ignore elements inside it.
			if (value != null) {
				throw new RuntimeException("Cannot have content inside an element with an idref");
			}
			currentSAXEventProcessor = new IgnoreElementProcessor(this, null);
		}
		
		/**
		 * DOCUMENT ME!
		 *
		 * @param name DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 *
		 * @throws SAXException DOCUMENT ME!
		 */
		public SAXEventProcessor endElement()
		throws SAXException {
			if (value != null) {
				parent.elementCompleted(value);
			}
			
			return parent;
		}
	}
	
	boolean requestSave(SessionImpl session, IWorkbenchWindow window) {
		String title = SerializedDatastorePlugin.getResourceString("MainFrame.saveOldSessionTitle");
		String question =
			SerializedDatastorePlugin.getResourceString("MainFrame.saveOldSessionQuestion");
		MessageDialog dialog = new MessageDialog(
				window.getShell(),
				title,
				null,	// accept the default window icon
				question, 
				MessageDialog.QUESTION, 
				new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 
				2); 	// CANCEL is the default
		
		int answer = dialog.open();
		switch (answer) {
		case 0: // YES
			saveSession(window);
			return true;
		case 1: // NO
			return true;
		case 2: // CANCEL
			return false;
		default:
			throw new RuntimeException("bad switch value");
		}
	}
	
	/**
	 * Saves the session in the selected file.
	 */
	private void saveSession(IWorkbenchWindow window) {
		SessionImpl session = (SessionImpl)JMoneyPlugin.getDefault().getSession();
		if (session.getFile() == null) {
			File sessionFile = obtainFileName(window);
			if (sessionFile != null) {
				writeSession(session, sessionFile, window);
			}
		} else {
			// TODO: this is a bit funny, as file is changed but not changed.
			// It works, though.
			writeSession(session, session.getFile(), window);
		}
	}
	
	private boolean dontOverwrite(File file, IWorkbenchWindow window) {
		if (file.exists()) {
			String question = SerializedDatastorePlugin.getResourceString("MainFrame.OverwriteExistingFile")
			+ " "
			+ file.getPath()
			+ "?";
			String title = SerializedDatastorePlugin.getResourceString("MainFrame.FileExists");
			
			boolean answer = MessageDialog.openQuestion(
					window.getShell(),
					title,
					question);
			return !answer;
		} else {
			return false;
		}
	}
	
	/**
	 * Obtain the file name if a file is not already associated with this session.
	 *
	 * @return true if a file name was obtained from the user,
	 *      false if no file name was obtained.
	 */
	public File obtainFileName(IWorkbenchWindow window) {
		FileDialog dialog = new FileDialog(window.getShell());
		dialog.setFilterExtensions(new String[] { "*.jmx", "*.xml" });
		dialog.setFilterNames(new String[] { "jmoney files", "uncompressed xml" });
		String fileName = dialog.open();
		//    String file = dialog.getFilterPath()+"\\"+dialog.getFileName();
		
		if (fileName != null) {
			File file = new File(fileName);
			if (dontOverwrite(file, window))
				return null;
			
			return file;
		}
		return null;
	}
	
	/**
	 * Saves the old session.
	 * Returns false if canceled by user or the save fails.
	 */
	public boolean saveOldSession(IWorkbenchWindow window) {
		ISessionManagement previousSession = JMoneyPlugin.getDefault().getSession();
		if (previousSession == null) {
			return true;
		} else {
			return previousSession.canClose(window);
		}
	}

	// Used for writing
	private Map namespaceMap;  // PropertySet to String (namespace prefix)
	private int accountId;
	private Map accountIdMap;
	
	// Used for reading
	private Map idToCurrencyMap;
	private Map idToAccountMap;
	
	/**
	 * Current event processor.  A stack of event processors
	 * is maintained as the XML is parsed.  Each event
	 * processor has a reference to the previous (next
	 * outer) event processor.
	 */
	public SAXEventProcessor currentSAXEventProcessor;
	

	/**
	 * Write session to file.
	 */
	public void writeSession(SessionImpl session, File sessionFile, IWorkbenchWindow window)  {
		// If there is any modified data in the controls in any of the
		// views, then commit these to the database now.
		// TODO: How do we do this?  Should framework call first?
		//  bookkeepingWindow.commitRemainingUserChanges();
		
		/* TODO: get the wait dialog working
		 MessageDialog waitDialog =
		 new MessageDialog(window.getShell(), Constants.LANGUAGE.getString("Dialog.Wait.Title"), 
		 null, // accept the default window icon
		 SerializerConstants.LANGUAGE.getString("MainFrame.SavingFile") + " " + sessionFile,
		 MessageDialog.INFORMATION, new String[] { }, 0);
		 waitDialog.open();
		 */
		
		try {
			FileOutputStream fout = new FileOutputStream(sessionFile);
			// Patch to aid with testing.  If the extension is 'xml'
			// then do not compress.
			BufferedOutputStream bout;
			if (sessionFile.getName().endsWith(".xml")) {
				bout = new BufferedOutputStream(fout);
			} else {
				GZIPOutputStream gout = new GZIPOutputStream(fout);
				bout = new BufferedOutputStream(gout);
			}
			
			if (false) {
				// The XMLEncoder must use the same classpath that was used to load this class.
				// The classpath set in this thread is the system class path, and if that
				// is used then XMLEncoder will not be able to find the classes specified
				// in the XML.  We must therefore temporarily replace the classpath.
				ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(this.getDescriptor().getPluginClassLoader());
				XMLEncoder enc = new XMLEncoder(bout);
				enc.writeObject(session);
				enc.close();
				Thread.currentThread().setContextClassLoader(originalClassLoader);            
			} else {
				// The new way.
				
				namespaceMap = new HashMap();
				accountId = 1;
				accountIdMap = new HashMap();
				
				// At some point we must decide which of the two
				// implementations to use.  Both work, as long as
				// no characters requiring entitization occur in the data.
				// (<, >, & etc.)  It really depends on what the
				// performance difference is.
				
//				writeObjectFast(bout, session, "session", "net.sf.jmoney.session");

				
				try {
//					FileWriter out = new FileWriter(sessionFile);
					StreamResult streamResult = new StreamResult(bout);
					SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
//					SAX2.0 ContentHandler.
					TransformerHandler hd = tf.newTransformerHandler();
					Transformer serializer = hd.getTransformer();
					serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
//					serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
					serializer.setOutputProperty(OutputKeys.INDENT,"no");
					hd.setResult(streamResult);
					hd.startDocument();
					writeObjectSafe(hd, session, "session", "net.sf.jmoney.session");
					hd.endDocument();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransformerConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			bout.close();
			fout.close();
			
			session.setModifiedA(false);
			session.setFileA(sessionFile);
			
			//       waitDialog.close();
		} catch (IOException ex) {
			//       waitDialog.close();
			fileWriteError(sessionFile, window);
		}
	}
	
/* SAMPLE CODE FOR XML WRITING
//	PrintWriter from a Servlet
	PrintWriter out = response.getWriter();
	StreamResult streamResult = new StreamResult(out);
	SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
//	SAX2.0 ContentHandler.
	TransformerHandler hd = tf.newTransformerHandler();
	Transformer serializer = hd.getTransformer();
	serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
	serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
	serializer.setOutputProperty(OutputKeys.INDENT,"yes");
	hd.setResult(streamResult);
	hd.startDocument();
	AttributesImpl atts = new AttributesImpl();
//	USERS tag.
	hd.startElement("","","USERS",atts);
//	USER tags.
	String[] id = {"PWD122","MX787","A4Q45"};
	String[] type = {"customer","manager","employee"};
	String[] desc = {"Tim@Home","Jack&Moud","John D'oé"};
	for (int i=0;i<id.length;i++)
	{
	atts.clear();
	atts.addAttribute("","","ID","CDATA",id[i]);
	atts.addAttribute("","","TYPE","CDATA",type[i]);
	hd.startElement("","","USER",atts);
	hd.characters(desc[i].toCharArray(),0,desc[i].length());
	hd.endElement("","","USER");
	}
	hd.endElement("","","USERS");
	hd.endDocument();
*/

	void writeObjectSafe(TransformerHandler hd, IExtendableObject object, String elementName, String typedPropertySetId) throws SAXException {
		// Find the property set information for this object.
		PropertySet propertySet = PropertySet.getPropertySet(object.getClass());

		AttributesImpl atts = new AttributesImpl();
		
		// Generate and declare the namespace prefixes.
		// All extension property sets have namespace prefixes.
		// Properties in base and derived property sets must be
		// unique within each object, so are all put in the
		// default namespace.
		atts.clear();
		if (typedPropertySetId.equals("net.sf.jmoney.session")) {
			atts.addAttribute("", "", "xmlns", "CDATA", "http://jmoney.sf.net");

			int suffix = 1;
			for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
				PropertySet extensionPropertySet = (PropertySet)iter.next();
				
				if (extensionPropertySet.isExtension()) {
					// Put into our map.
					String namespacePrefix = "ns" + new Integer(suffix++).toString();
					namespaceMap.put(extensionPropertySet, namespacePrefix);
					
					atts.addAttribute("", "", "xmlns:" + namespacePrefix, "CDATA", "http://jmoney.sf.net/" + extensionPropertySet.getId());
				}
			}
		}
		
		String id = null;
		if (object instanceof Currency) {
			id = ((Currency)object).getCode();
		} else if (object instanceof Account) {
			id = "account" + new Integer(accountId++).toString();
			accountIdMap.put(object, id);
		}

		if (id != null) {
			atts.addAttribute("", "", "id", "CDATA", id);
		}

		if (!propertySet.getId().equals(typedPropertySetId)) {
			atts.addAttribute("", "", "propertySet", "CDATA", propertySet.getId());
		}

		hd.startElement("", "", elementName, atts);
		
		// Write all properties that have both getters and setters.
		
		// If the property is an object then we find the PropertyAccessor
		// object for it and see if this object 'owns' the object.
		// If it does then we output that object as a nested XML element. 
		
		// If we find the pattern for a list (add... method,
		// remove... method, and a get...Iterator method)
		// then we again look for the PropertyAccessor object
		// and see if this object 'owns' the list.
		// If it does then we output the list of objects
		// as nested XML elements.
		
		// For derived property sets, information must be in
		// the XML that allows the derived property set to be
		// determined.  This is done by outputting the
		// actual final property set id.  The property set id
		// is specified as an attribute.
		
		// When an object is not owned, an id is specified.
		// These are specified as 'id' and 'idref' attributes
		// in the normal way.
		
		// Write the list properties.
		// This is done before the properties because then, as it happens, we get
		// no problems due to the single pass.
		// TODO: we cannot rely on this mechanism to ensure all idref's are written
		// before they are used.
		if (propertySet.getId().equals("net.sf.jmoney.session")) {
			Session session = (Session)object;
			
			for (Iterator iter = session.getCommodityIterator(); iter.hasNext(); ) {
				Commodity commodity = (Commodity)iter.next();
				writeObjectSafe(hd, commodity, "commodity", "net.sf.jmoney.commodity");
			}

			// Write the accounts
			for (Iterator iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				Account account = (Account)iter.next();
				writeObjectSafe(hd, account, "account", "net.sf.jmoney.account");
				
			}
			for (Iterator iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
				Account account = (Account)iter.next();
				writeObjectSafe(hd, account, "account", "net.sf.jmoney.account");
			}
			
			for (Iterator iter = session.getTransaxionIterator(); iter.hasNext(); ) {
				Transaxion transaction = (Transaxion)iter.next();
				writeObjectSafe(hd, transaction, "transaction", "net.sf.jmoney.transaction");
			}
		} else if (propertySet.getId().equals("net.sf.jmoney.capitalAccount")) {
			Account account = (Account)object;
			
			for (Iterator iter = account.getSubAccountIterator(); iter.hasNext(); ) {
				Account subAccount = (Account)iter.next();
				writeObjectSafe(hd, subAccount, "subAccount", "net.sf.jmoney.capitalAccount");
			}
		} else if (propertySet.getId().equals("net.sf.jmoney.categoryAccount")) {
			Account account = (Account)object;
			
			for (Iterator iter = account.getSubAccountIterator(); iter.hasNext(); ) {
				Account subAccount = (Account)iter.next();
				writeObjectSafe(hd, subAccount, "subAccount", "net.sf.jmoney.incomeExpenseAccount");
			}
		} else if (propertySet.getId().equals("net.sf.jmoney.transaction")) {
			Transaxion transaction = (Transaxion)object;
			
			for (Iterator iter = transaction.getEntriesIterator(); iter.hasNext(); ) {
				Entry entry = (Entry)iter.next();
				writeObjectSafe(hd, entry, "entry", "net.sf.jmoney.entry");
			}
			
		}

		for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			PropertySet propertySet2 = propertyAccessor.getPropertySet(); 
			if (!propertySet2.isExtension()
					|| object.getExtension(propertySet2) != null) {
				String name = propertyAccessor.getLocalName();
				Object value = object.getPropertyValue(propertyAccessor);
				// No element means null value.
				if (value != null) {
					
					atts.clear();

					String idref = null;
					if (propertyAccessor.getValueClass() == Currency.class) {
						idref = ((Currency)value).getCode();
					} else if (value instanceof Account) {
						idref = (String)accountIdMap.get(value);
					}
					if (idref != null) {
						atts.addAttribute("", "", "idref", "CDATA", idref);
					}

					String qName;
					if (propertySet2.isExtension()) {
						String namespacePrefix = (String)namespaceMap.get(propertySet2); 
						qName = namespacePrefix + ":" + name;
					} else {
						qName = name;
					}
					hd.startElement("", "", qName, atts);

					if (idref == null) {
						String text;
						if (value instanceof Date) {
							Date date = (Date)value;
							text = new Integer(date.getYear() + 1900).toString() + "."
							+ new Integer(date.getMonth()).toString() + "."
							+ new Integer(date.getDay()).toString();
						} else {
							text = value.toString();
						}
						hd.characters(text.toCharArray(), 0, text.length());
					}

					hd.endElement("", "", qName);
				}
			}
		}
		
		hd.endElement("", "", elementName);
	}
	
	void writeObjectFast(BufferedOutputStream bout, IExtendableObject object, String elementName, String typedPropertySetId) throws IOException {
		// Find the property set information for this object.
		PropertySet propertySet = PropertySet.getPropertySet(object.getClass());

		bout.write(new String("<").getBytes());
		bout.write(elementName.getBytes());
		
		// Generate and declare the namespace prefixes.
		// All extension property sets have namespace prefixes.
		// Properties in base and derived property sets must be
		// unique within each object, so are all put in the
		// default namespace.
		if (typedPropertySetId.equals("net.sf.jmoney.session")) {
			bout.write(new String(" xmlns=\"http://jmoney.sf.net\"").getBytes());

			int suffix = 1;
			for (Iterator iter = PropertySet.getPropertySetIterator(); iter.hasNext(); ) {
				PropertySet extensionPropertySet = (PropertySet)iter.next();
				
				if (extensionPropertySet.isExtension()) {
					// Put into our map.
					String namespacePrefix = "ns" + new Integer(suffix++).toString();
					namespaceMap.put(extensionPropertySet, namespacePrefix);
					
					bout.write(new String(" xmlns:").getBytes());
					bout.write(namespacePrefix.getBytes());
					bout.write(new String("=\"http://jmoney.sf.net/").getBytes());
					bout.write(extensionPropertySet.getId().getBytes());
					bout.write(new String("\"").getBytes());
				}
			}
		}
		
		String id = null;
		if (object instanceof Currency) {
			id = ((Currency)object).getCode();
		} else if (object instanceof Account) {
			id = "account" + new Integer(accountId++).toString();
			accountIdMap.put(object, id);
		}
		if (id != null) {
			bout.write(new String(" id=\"").getBytes());
			bout.write(id.getBytes());
			bout.write(new String("\"").getBytes());
		}

		if (!propertySet.getId().equals(typedPropertySetId)) {
			bout.write(new String(" propertySet=\"").getBytes());
			bout.write(propertySet.getId().getBytes());
			bout.write(new String("\"").getBytes());
		}
		bout.write(new String(">\n").getBytes());
		
		// Write all properties that have both getters and setters.
		
		// If the property is an object then we find the PropertyAccessor
		// object for it and see if this object 'owns' the object.
		// If it does then we output that object as a nested XML element. 
		
		// If we find the pattern for a list (add... method,
		// remove... method, and a get...Iterator method)
		// then we again look for the PropertyAccessor object
		// and see if this object 'owns' the list.
		// If it does then we output the list of objects
		// as nested XML elements.
		
		// For derived property sets, information must be in
		// the XML that allows the derived property set to be
		// determined.  This is done by outputting the
		// actual final property set id.  The property set id
		// is specified as an attribute.
		
		// When an object is not owned, an id is specified.
		// These are specified as 'id' and 'idref' attributes
		// in the normal way.
		
		// Write the list properties.
		// This is done before the properties because then, as it happens, we get
		// no problems due to the single pass.
		// TODO: we cannot rely on this mechanism to ensure all idref's are written
		// before they are used.
		if (propertySet.getId().equals("net.sf.jmoney.session")) {
			Session session = (Session)object;
			
			for (Iterator iter = session.getCommodityIterator(); iter.hasNext(); ) {
				Commodity commodity = (Commodity)iter.next();
				writeObjectFast(bout, commodity, "commodity", "net.sf.jmoney.commodity");
			}

			// Write the accounts
			for (Iterator iter = session.getCapitalAccountIterator(); iter.hasNext(); ) {
				Account account = (Account)iter.next();
				writeObjectFast(bout, account, "account", "net.sf.jmoney.account");
				
			}
			for (Iterator iter = session.getIncomeExpenseAccountIterator(); iter.hasNext(); ) {
				Account account = (Account)iter.next();
				writeObjectFast(bout, account, "account", "net.sf.jmoney.account");
			}
			
			for (Iterator iter = session.getTransaxionIterator(); iter.hasNext(); ) {
				Transaxion transaction = (Transaxion)iter.next();
				writeObjectFast(bout, transaction, "transaction", "net.sf.jmoney.transaction");
			}
		} else if (propertySet.getId().equals("net.sf.jmoney.capitalAccount")) {
			Account account = (Account)object;
			
			for (Iterator iter = account.getSubAccountIterator(); iter.hasNext(); ) {
				Account subAccount = (Account)iter.next();
				writeObjectFast(bout, subAccount, "subAccount", "net.sf.jmoney.capitalAccount");
			}
		} else if (propertySet.getId().equals("net.sf.jmoney.categoryAccount")) {
			Account account = (Account)object;
			
			for (Iterator iter = account.getSubAccountIterator(); iter.hasNext(); ) {
				Account subAccount = (Account)iter.next();
				writeObjectFast(bout, subAccount, "subAccount", "net.sf.jmoney.incomeExpenseAccount");
			}
		} else if (propertySet.getId().equals("net.sf.jmoney.transaction")) {
			Transaxion transaction = (Transaxion)object;
			
			for (Iterator iter = transaction.getEntriesIterator(); iter.hasNext(); ) {
				Entry entry = (Entry)iter.next();
				writeObjectFast(bout, entry, "entry", "net.sf.jmoney.entry");
			}
			
		}

		for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			PropertySet propertySet2 = propertyAccessor.getPropertySet(); 
			if (!propertySet2.isExtension()
					|| object.getExtension(propertySet2) != null) {
				String name = propertyAccessor.getLocalName();
				Object value = object.getPropertyValue(propertyAccessor);
				// No element means null value.
				if (value != null) {
					
					bout.write(new String("<").getBytes());
					if (propertySet2.isExtension()) {
						String namespacePrefix = (String)namespaceMap.get(propertySet2); 
						bout.write((namespacePrefix).getBytes());
						bout.write(new String(":").getBytes());
					}	
					bout.write(name.getBytes());

					String idref = null;
					if (propertyAccessor.getValueClass() == Currency.class) {
						idref = ((Currency)value).getCode();
					} else if (value instanceof Account) {
						idref = (String)accountIdMap.get(value);
					}
					if (idref != null) {
						bout.write(new String(" idref=\"").getBytes());
						bout.write(idref.toString().getBytes());
						bout.write(new String("\"/>\n").getBytes());
					} else {
						bout.write(new String(">").getBytes());
						if (value instanceof Date) {
							Date date = (Date)value;
//							bout.write(new Integer(date.get(Calendar.YEAR) + 1900).toString().getBytes());
							bout.write(new Integer(date.getYear() + 1900).toString().getBytes());
							bout.write(new String(".").getBytes());
							bout.write(new Integer(date.getMonth()).toString().getBytes());
							bout.write(new String(".").getBytes());
							bout.write(new Integer(date.getDay()).toString().getBytes());
						} else {
							bout.write(value.toString().getBytes());
						}
						bout.write(new String("</").getBytes());
						if (propertySet2.isExtension()) {
							String namespacePrefix = (String)namespaceMap.get(propertySet2); 
							bout.write((namespacePrefix).getBytes());
							bout.write(new String(":").getBytes());
						}	
						bout.write(name.getBytes());
						bout.write(new String(">\n").getBytes());
					}
				}
			}
		}
		
		bout.write(new String("</").getBytes());
		bout.write(elementName.getBytes());
		bout.write(new String(">\n").getBytes());
	}
	
	/**
	 * This method is used when reading a session.
	 */
	public void fileReadError(File file, IWorkbenchWindow window) {
		String message = SerializedDatastorePlugin.getResourceString("MainFrame.CouldNotReadFile")
		+ " "
		+ file.getPath();
		String title = SerializedDatastorePlugin.getResourceString("MainFrame.FileError");
		
		MessageDialog.openError(
				window.getShell(),
				title,
				message);
	}
	
	/**
	 * This method is used when writing a session.
	 */
	public void fileWriteError(File file, IWorkbenchWindow window) {
		String message = SerializedDatastorePlugin.getResourceString("MainFrame.CouldNotWriteFile")
		+ " "
		+ file.getPath();
		String title = SerializedDatastorePlugin.getResourceString("MainFrame.FileError");
		
		MessageDialog.openError(
				window.getShell(),
				title,
				message);
	}
	
	/**
	 * Not everything is serialized to disk.  Redundant information is
	 * not saved.  This method sets up these pointers and should be
	 * called after a session is de-serialized.
	 *
	 * The following need to be set up:
	 * <UL>
	 * <LI>The list of entries in each account</LI>
	 * <LI>The back-pointers from sub-categories to their parents</LI>
	 * <LI>The back-pointers from entries to the transaction</LI>
	 * </UL>
	 */
	private void setRedundantReferences(SessionImpl session) {
		
		// Add the back-references from sub-categories to the parent category
		setParentAccountReferences(session.getIncomeExpenseAccountIterator(), null);
		setParentAccountReferences(session.getCapitalAccountIterator(), null);
		
		// Add the back-references from entries to the transactions and also
		// add each entry in an account to the list for that account.
		for (Iterator iter = session.getTransaxionIterator(); iter.hasNext(); ) {
			Transaxion transaction = (Transaxion)iter.next();
			
			for (Iterator entryIter = transaction.getEntriesIterator(); entryIter.hasNext(); ) {
				EntryImpl entry = (EntryImpl)entryIter.next();
				
				entry.setTransaxion(transaction);
				
				if (entry.getAccount() instanceof CapitalAccountImpl) {
					CapitalAccountImpl account = (CapitalAccountImpl) entry.getAccount();
					account.addEntry(entry);
				}
			}
		}
	}
	
	
	private void setParentAccountReferences(Iterator iter, Account parent) {
		while (iter.hasNext()) {
			AbstractAccountImpl category = (AbstractAccountImpl) iter.next();
			category.setParent(parent);
			setParentAccountReferences(category.getSubAccountIterator(), category);
		}
	}
	
	/**
	 * Converts an old format session (net.sf.jmoney.model.Session) to
	 * the latest format session (net.sf.jmoney.model2.Session).
	 * The current model is implemented in the net.sf.jmoney.model2
	 * package.   The net.sf.jmoney.model package implements an older
	 * model that is now obsolete.  This method allows persistent
	 * serializations of the old model to be converted to the new model
	 * to ensure backwards compatibility.
	 */
	private void convertModelOneFormat(net.sf.jmoney.model.Session oldFormatSession, Session newSession) {
		Map accountMap = new Hashtable();
		
		// Add the income and expense accounts
		net.sf.jmoney.model.CategoryNode root = oldFormatSession.getCategories().getRootNode();
		for (Enumeration e = root.children(); e.hasMoreElements();) {
			net.sf.jmoney.model.CategoryNode node = (net.sf.jmoney.model.CategoryNode) e.nextElement();
			Object obj = node.getUserObject();
			if (obj instanceof net.sf.jmoney.model.SimpleCategory) {
				net.sf.jmoney.model.SimpleCategory oldCategory = (net.sf.jmoney.model.SimpleCategory) obj;
				
				MutableIncomeExpenseAccount newMutableCategory = newSession.createNewIncomeExpenseAccount();
				newMutableCategory.setName(oldCategory.getCategoryName());
				IncomeExpenseAccount newCategory = newMutableCategory.commit();
				
				accountMap.put(oldCategory, newCategory);
				
				for (Enumeration e2 = oldCategory.getCategoryNode().children(); e2.hasMoreElements();) {
					net.sf.jmoney.model.CategoryNode subNode = (net.sf.jmoney.model.CategoryNode) e2.nextElement();
					Object obj2 = subNode.getUserObject();
					if (obj2 instanceof net.sf.jmoney.model.SimpleCategory) {
						net.sf.jmoney.model.SimpleCategory oldSubCategory = (net.sf.jmoney.model.SimpleCategory) obj2;
						
						MutableIncomeExpenseAccount newMutableSubCategory = newCategory.createNewSubAccount(newSession);
						newMutableSubCategory.setName(oldSubCategory.getCategoryName());
						IncomeExpenseAccount newSubCategory = newMutableSubCategory.commit();
						
						accountMap.put(oldSubCategory, newSubCategory);
					}
				}
			}
		}
		
		// Add the capital  accounts
		Vector oldAccounts = oldFormatSession.getAccounts();
		for (Iterator iter = oldAccounts.iterator(); iter.hasNext(); ) {
			net.sf.jmoney.model.Account oldAccount = (net.sf.jmoney.model.Account)iter.next();
			
			MutableCapitalAccount newMutableAccount = newSession.createNewCapitalAccount();
			newMutableAccount.setName(oldAccount.getName());
			newMutableAccount.setAbbreviation(oldAccount.getAbbrevation());
			newMutableAccount.setAccountNumber(oldAccount.getAccountNumber());
			newMutableAccount.setBank(oldAccount.getBank());
			newMutableAccount.setComment(oldAccount.getComment());
			newMutableAccount.setCurrency(newSession.getCurrencyForCode(oldAccount.getCurrencyCode()));
			newMutableAccount.setMinBalance(oldAccount.getMinBalance());
			newMutableAccount.setStartBalance(oldAccount.getStartBalance());
			CapitalAccount newAccount = newMutableAccount.commit();
			
			accountMap.put(oldAccount, newAccount);
		}
		
		// Add the transactions and entries
		
		// We must be very careful here.  Consider a split entry that
		// contain a double entry within it.  The other account in the
		// double entry does not see the split entry.  There is simply no
		// way of getting to the split entry from the other account.
		// If we create a new format transaction for the old format
		// double entry then we are in trouble because we would need to
		// find and amend the transaction later when we find the split
		// entry.
		
		// When we find a split entry, we create the entire transaction
		// at that time.  We know that the other half of any double entries
		// in the split entry cannot also be in a split entry, because
		// this could not have happened under the old model.
		// When we find a double entry (that is not part of a split entry)
		// we do not create the transaction because we do not know if the
		// other half of the entry is in a split entry.  We add the
		// double entry to the set of double entries previously found.
		// However, if the other half of this entry is in the set then
		// we know neither half of the double entry is in a split entry,
		// so we create the transaction at that time.
		
		// Here is the set of double entries that have been found but
		// not yet processed.
		Set doubleEntriesPreviouslyFound = new HashSet();
		
		// See if the plug-in for the reconciliation state is present.
		// If it is then we can copy the reconciliation state into
		// the extension for this plug-in.
		// This is an example of a plug-in that does not depend on another
		// plug-in but will use it if it is there.
		PropertyAccessor statusProperty = null;
		try {
			PropertySet reconciliationProperties = PropertySet.getPropertySet("net.sf.jmoney.reconciliation.entryProperties");
			if (reconciliationProperties.isExtensionClassKnown()) {
				statusProperty = reconciliationProperties.getProperty("status");
			}
		} catch (PropertySetNotFoundException e) {
			// If the property set is not found then this means
			// the reconciliation plug-in is not installed.
			// We simply drop the reconciliation field in such
			// circumstances.
			// TODO It would be better if we saved the data
			// in case the user installs the plug-in later.
			// To do this, we must create a general purpose
			// property class that is able to store any property
			// given to it.
			// Alternatively, we could not do the above but
			// recommend creating an extension property and then
			// create a propagator to get the value into the
			// reconciliation plug-in.  This would be better
			// if this process could be made more efficient.
			statusProperty = null;
		} catch (PropertyNotFoundException e) {
			// If the property is not found then this means
			// the reconciliation plug-in has been updated to
			// a later version and the 'status' property is not
			// longer supported in the later version.
			// The reconciliation plug-in should provide a 'status'
			// property with a setter only so that upgrades are
			// possible.  However, plug-ins do not have to support
			// unlimited past versions (or should they)?
			// We simply drop the reconciliation field in such
			// circumstances.
			statusProperty = null;
		}
		
		for (Iterator iter = oldAccounts.iterator(); iter.hasNext(); ) {
			net.sf.jmoney.model.Account oldAccount = (net.sf.jmoney.model.Account)iter.next();
			CapitalAccount newAccount = (CapitalAccount)accountMap.get(oldAccount);
			
			for (Iterator entryIter = oldAccount.getEntries().iterator(); entryIter.hasNext(); ) {
				net.sf.jmoney.model.Entry oldEntry = (net.sf.jmoney.model.Entry)entryIter.next();
				
				if (oldEntry instanceof net.sf.jmoney.model.DoubleEntry) {
					net.sf.jmoney.model.DoubleEntry de = (net.sf.jmoney.model.DoubleEntry)oldEntry;
					// Only add this transaction if we have already come across the
					// other half of this entry and so we know the other half is not
					// part of a split entry.
					if (doubleEntriesPreviouslyFound.contains(de.getOther())) {
						MutableTransaxion trans = newSession.createNewTransaxion();
						trans.setDate(de.getDate());
						Entry entry1 = trans.createEntry();
						Entry entry2 = trans.createEntry();
						entry1.setAmount(de.getAmount());
						entry2.setAmount(-de.getAmount());
						entry1.setAccount((Account)accountMap.get(de.getOther().getCategory()));
						entry2.setAccount((Account)accountMap.get(de.getCategory()));
						
						copyEntryProperties(de, entry1, statusProperty);
						copyEntryProperties(de.getOther(), entry2, statusProperty);
						
						trans.commit();
					} else {
						doubleEntriesPreviouslyFound.add(de);
					}
				} else if (oldEntry instanceof net.sf.jmoney.model.SplittedEntry) {
					net.sf.jmoney.model.SplittedEntry se = (net.sf.jmoney.model.SplittedEntry)oldEntry;
					
					MutableTransaxion trans = newSession.createNewTransaxion();
					trans.setDate(oldEntry.getDate());
					
					// Add the entry for the account that was holding the split entry.
					Entry subEntry = trans.createEntry();
					subEntry.setAmount(oldEntry.getAmount());
					subEntry.setAccount(newAccount);
					
					copyEntryProperties(oldEntry, subEntry, statusProperty);
					
					// Add an entry for each old entry in the split.
					for (Iterator subEntryIter = se.getEntries().iterator(); subEntryIter.hasNext(); ) {
						net.sf.jmoney.model.Entry oldSubEntry = (net.sf.jmoney.model.Entry)subEntryIter.next();
						
						subEntry = trans.createEntry();
						subEntry.setAmount(oldSubEntry.getAmount());
						subEntry.setAccount((Account)accountMap.get(oldSubEntry.getCategory()));
						copyEntryProperties(oldSubEntry, subEntry, statusProperty);
					}
					
					trans.commit();
				} else {
					MutableTransaxion trans = newSession.createNewTransaxion();
					trans.setDate(oldEntry.getDate());
					Entry entry1 = trans.createEntry();
					Entry entry2 = trans.createEntry();
					entry1.setAmount(oldEntry.getAmount());
					entry2.setAmount(-oldEntry.getAmount());
					entry1.setAccount(newAccount);
					entry2.setAccount((Account)accountMap.get(oldEntry.getCategory()));
					
					// Put the check, memo, valuta, and status into the account entry only.
					// Assume the creation and description apply to both account and
					// category.
					copyEntryProperties(oldEntry, entry1, statusProperty);
					
					entry2.setCreation(oldEntry.getCreation());
					entry2.setDescription(oldEntry.getDescription());
					
					trans.commit();
				}
			}
		}
	}
	
	private void copyEntryProperties(net.sf.jmoney.model.Entry oldEntry, Entry entry, PropertyAccessor statusProperty) {
		entry.setCheck(oldEntry.getCheck());
		entry.setCreation(oldEntry.getCreation());
		entry.setDescription(oldEntry.getDescription());
		entry.setMemo(oldEntry.getMemo());
		entry.setValuta(oldEntry.getValuta());
		if (statusProperty != null && oldEntry.getStatus() != 0) {
			entry.setIntegerPropertyValue(statusProperty, oldEntry.getStatus());
		}
	}
	
}
