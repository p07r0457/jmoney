/*
 * Created on Feb 26, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jmoney.serializeddatastore.formats;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.dialogs.EventLoopProgressMonitor;
import org.eclipse.ui.internal.progress.ProgressMonitorJobsDialog;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.BankAccountInfo;
import net.sf.jmoney.fields.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertyNotFoundException;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.serializeddatastore.IFileDatastore;
import net.sf.jmoney.serializeddatastore.OldFormatJMoneyFileException;
import net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin;
import net.sf.jmoney.serializeddatastore.SessionManager;
import net.sf.jmoney.serializeddatastore.SimpleListManager;
import net.sf.jmoney.serializeddatastore.SimpleObjectKey;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class JMoneyXmlFormat implements IFileDatastore {

	/**
	 * Date format used for dates in this file format:
	 * yyyy.MM.dd
	 */
	private static SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateInstance();
	static {
		dateFormat.applyPattern("yyyy.MM.dd");
	}
	
	/**
	 * Read session from file.
	 * <P>
	 * The opened session is set as the current open JMoney session. 
	 * If no session can be opened then an appropriate message is
	 * displayed to the user and the previous session, if any, is
	 * left open.
	 */
	public void readSession(final File sessionFile, final SessionManager sessionManager, final IWorkbenchWindow window) {
		try {
			if (sessionFile.length() < 500000) {
				// If the file is smaller than 500K then it is
				// not worthwhile using a progress monitor.
				// The monitor would flash up so quickly that the
				// user could not read it.
				readSessionQuietly(sessionFile, sessionManager, null);
			} else {
				IRunnableWithProgress readSessionRunnable = new IRunnableWithProgress() {
					
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						// Set the number of work units in the monitor where
						// one work unit is reading 100 Kbytes.
						int workUnits = (int)(sessionFile.length()/100000);
						
						monitor.beginTask(
								SerializedDatastorePlugin.getResourceString("MainFrame.OpeningFile") + " " + sessionFile, 
								workUnits);   
						
						try {
							readSessionQuietly(sessionFile, sessionManager, monitor);
						} catch (Exception ex) {
							throw new InvocationTargetException(ex);
						} finally {
							monitor.done();
						}
					}
					
				};
				
				ProgressMonitorJobsDialog progressDialog = new ProgressMonitorJobsDialog(window.getShell());
				
				try {
					progressDialog.run(true, false, readSessionRunnable);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
				
				EventLoopProgressMonitor monitor = new EventLoopProgressMonitor(new NullProgressMonitor());
			}
		} catch (InterruptedException e) {
			// If the user inturrupted the read then no error message is displayed.
			// Currently this cannot happen because the cancel button is not
			// enabled in the progress dialog, but if the cancel button is enabled
			// then we do nothing here, leaving the previous session, if any, open.
		} catch (Throwable ex) {
			JMoneyPlugin.log(ex);
			fileReadError(sessionFile, window);
		}
	}
	
	/**
	 * This class extends FileInputStream and overrides the
	 * various read methods, counting the total number of
	 * bytes read and updating the progress monitor.
	 * <P>
	 * This stream is used as input to BufferedInputStream,
	 * either directly or through GZIPInputStream.  Of all
	 * the read methods, only read(byte b[], int off, int len)
	 * is used by BufferedInputStream, and read() is used
	 * occassionally by GZIPInputStream.  However, for completeness,
	 * all the read methods have been overridden to update
	 * the byte count.
	 * Other methods that may affect the progress, such as
	 * skip(n), do not appear to be called by the above
	 * consumers of the stream.
	 */
	private class FileInputStreamWithMonitor extends FileInputStream {

		private IProgressMonitor monitor;
		private long totalBytes = 0;
		private int previousTotalWork = 0;

		/**
		 * @param monitor The monitor to be updated.  This
		 * 			parameter must be non-null.  The monitor
		 * 			must have been initialized for an expected
		 * 			amount of total work units where one work unit
		 * 			is reading 100 KBytes of the input stream.
		 */
		FileInputStreamWithMonitor(File sessionFile, IProgressMonitor monitor) 
			throws FileNotFoundException {
			super(sessionFile);
			this.monitor = monitor;
		}
		
		/* This method reads a single byte at a time.
		 * GZIPInputStream uses this method occassionally, so we increment
		 * the count of bytes read just to stop errors creeping
		 * in.  However, we don't bother to update the monitor.
		 */
		public int read() throws IOException {
			totalBytes++;
			return super.read();
		}

	    public int read(byte b[]) throws IOException {
	    	int bytesRead = super.read(b);
			updateProgress(bytesRead);
			return bytesRead;
	    }

	    public int read(byte b[], int off, int len) throws IOException {
		int bytesRead = super.read(b, off, len);
		updateProgress(bytesRead);
		return bytesRead;
	    }

	    /**
	     * Update the progress monitor.  The number of bytes read from
	     * the input stream is passed to this method and used to measure
	     * the progress.
	     *
	     * @param bytesRead the number of bytes read from the input stream.
	     */
	    private void updateProgress(int bytesRead) {
	    	if (bytesRead > 0) {
	    		totalBytes += bytesRead;
	    		int newTotalWork = (int)(totalBytes/100000);
	    		if (newTotalWork > previousTotalWork) {
	    			monitor.worked(newTotalWork - previousTotalWork);
	    			previousTotalWork = newTotalWork;
	    		}
	    	}
	    }
	}
	
	/**
	 * Read a session from file, creating a session manager and a
	 * session.
	 *
	 * @param monitor Monitor into which this method will call
	 * 			the beginTask method and update the progress.
	 * 			This parameter may be null in which this method
	 * 			will read the session without feedback on the progress.
	 */
	public void readSessionQuietly(File sessionFile, SessionManager sessionManager, IProgressMonitor monitor) throws FileNotFoundException, IOException, CoreException {
		InputStream fin;
		if (monitor == null) {
			fin = new FileInputStream(sessionFile);
		} else {
			fin = new FileInputStreamWithMonitor(sessionFile, monitor);
		}
		
		// If the extension is 'xml' then no compression is used.
		// If the extension is 'jmx' then compression is used.
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
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
            try {
                idToCommodityMap = new HashMap();
                idToAccountMap = new HashMap();
                currentSAXEventProcessor = null;

                factory.setValidating(false);
                factory.setNamespaceAware(true);
                SAXParser saxParser = factory.newSAXParser();
                HandlerForObject handler = new HandlerForObject(sessionManager);
                saxParser.parse(bin, handler);
                Session newSession = handler.getSession();

                sessionManager.setSession(newSession);
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("Serious XML parser configuration error");
            } catch (SAXException e) {
                // Workaround: OldFormatJMoneyFileException seems to be thrown in
                // two different ways: Either embedded in a SAXException or
                // directly as an OldFormatJMoneyFileException.
                if (e.getException() instanceof OldFormatJMoneyFileException) {
                    throw (OldFormatJMoneyFileException) e.getException();
                } else {
                    throw new RuntimeException("Fatal SAX parser error");
                }
            }
        }
		catch (OldFormatJMoneyFileException se) {
			// This exception will be throw if the file is old format (0.4.5 or prior).
			// Try reading as an old format file.

			// First close and re-open the file.
			bin.close();
			if (gin != null) gin.close();
			fin.close();
			
			if (monitor == null) {
				fin = new FileInputStream(sessionFile);
			} else {
				fin = new FileInputStreamWithMonitor(sessionFile, monitor);
			}
			
			// If the extension is 'xml' then no compression is used.
			// If the extension is 'jmx' then compression is used.
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
			Thread.currentThread().setContextClassLoader(SerializedDatastorePlugin.getDefault().getDescriptor().getPluginClassLoader());
			XMLDecoder dec = new XMLDecoder(bin);
			Object newSession = dec.readObject();
			dec.close();
			Thread.currentThread().setContextClassLoader(originalClassLoader);            

			if  (!(newSession instanceof net.sf.jmoney.model.Session)) {
				throw new CoreException(
						new Status(Status.ERROR, "net.sf.jmoney.serializeddatastore", Status.OK,
								"session object deserialized, but the object was not a session!",
								null));	
			}
			
			SimpleObjectKey key = new SimpleObjectKey(sessionManager); 
			Session newSessionNewFormat = new Session(
				key,
				null,
				null,
				new SimpleListManager(sessionManager),
				new SimpleListManager(sessionManager),
				new SimpleListManager(sessionManager),
				null
			);
			key.setObject(newSessionNewFormat);
			sessionManager.setSession(newSessionNewFormat);
			
			convertModelOneFormat((net.sf.jmoney.model.Session)newSession, newSessionNewFormat);
		}
		catch (IOException ioe) { 
			throw new RuntimeException("IO internal exception error");
		} finally {
			bin.close();
			if (gin != null) gin.close();
			fin.close();
		}
	}
	
	private class HandlerForObject extends DefaultHandler {
		
		protected SessionManager sessionManager;
		/**
		 * The top level session object.
		 */
		private Session session;
		
		HandlerForObject(SessionManager sessionManager) {
			this.sessionManager = sessionManager;
		}
		
		Session getSession() {
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
					if (localName.equals("java")) {
						throw new OldFormatJMoneyFileException();
					} else {
						throw new SAXException("Unexpected top level element '" + localName + "' found.  The top level element must be either 'session' (new format file) or 'java' (old format file).");
					}
				}
				
				currentSAXEventProcessor = new ObjectProcessor(sessionManager, null, "net.sf.jmoney.session");
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
			SAXEventProcessor parent = currentSAXEventProcessor.endElement();
			
			if (parent == null) {
				// We are back at the top level.
				// Save this object because it is the session object.
				session = (Session)currentSAXEventProcessor.getValue();
			}
			
			currentSAXEventProcessor = parent;
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
		protected SessionManager sessionManager;
		
		/**
		 * Creates a new SAXEventProcessor object.
		 *
		 * @param parent The event processor that was in effect.  This newly
		 *        created event processor will take over and will process the
		 *        contents of an element.  When the end tag for the element is
		 *        found then this original event processor must be restored as
		 *        the active event processor.
		 */
		SAXEventProcessor(SessionManager sessionManager, SAXEventProcessor parent) {
			this.sessionManager = sessionManager;
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
		 * to this processor.
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

		/**
		 * @return
		 */
		// TODO: This method can be called instead of using elementCompleted.
		// elementCompleted can then be removed.
		public abstract Object getValue();
	}

	/**
	 * An event processor that takes over processing
	 * while we are inside an object.  The processor looks for
	 * elements representing the properties of the object.
	 */
	private class ObjectProcessor extends SAXEventProcessor {
		
		private PropertySet propertySet;
		
		/**
		 * If we have processed the start of an element representing
		 * a property but have not yet processed the end of the element
		 * then this field is the property accessor.  Otherwise this
		 * field is null.
		 */
		private PropertyAccessor propertyAccessor = null;
		
		/**
		 * Key to the object being parsed by this ObjectProcessor.
		 */
		SimpleObjectKey objectKey;
		
		/**
		 * The list of parameters to be passed to the constructor
		 * of this object.
		 */
		private Object[] constructorParameters;
		
		/**
		 * Map of extension PropertySet objects to arrays of
		 * constructor parameters that construct the extension
		 * objects. 
		 */
		private Map extensionMap;
		
		/**
		 * Saved id of objects that are Currency or Account objects.
		 * This id is saved so that when the object is later created, it can
		 * be added to a map.
		 */
		private Map map;
		private String id;

		private Object value;
		
		/**
		 *
		 * @param parent The event processor that was in effect.  This newly
		 *        created event processor will take over and will process the
		 *        contents of an element.  When the end tag for the element is
		 *        found then this original event processor must be restored as
		 *        the active event processor.
		 */
		ObjectProcessor(SessionManager sessionManager, ObjectProcessor parent, String propertySetId) {
			super(sessionManager, parent);
			
			objectKey = new SimpleObjectKey(sessionManager);

			try {
				this.propertySet = PropertySet.getPropertySet(propertySetId);
			} catch (PropertySetNotFoundException e) {
				throw new RuntimeException("internal error");
			}
			
			Collection constructorProperties = propertySet.getConstructorProperties();
			int numberOfParameters = constructorProperties.size();
			if (!propertySet.isExtension()) {
				numberOfParameters += 3;
			}
			constructorParameters = new Object[numberOfParameters];
			extensionMap = new HashMap();
			constructorParameters[0] = objectKey;
			constructorParameters[1] = extensionMap;
			if (parent == null) {
				constructorParameters[2] = null;
			} else {
				constructorParameters[2] = parent.objectKey;
			}
			
			// For all lists, set the Collection object to be a Vector.
			// For all other parameters, the value is set when the property
			// value is found.  We initialize to null here so a null value
			// will be passed to the constructor if no value is found.
			Object[] defaultProperties = propertySet.getDefaultPropertyValues2();
			int i = 0;
			for (Iterator iter = constructorProperties.iterator(); iter.hasNext(); ) {
				PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
				if (propertyAccessor.isList()) {
					constructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = new SimpleListManager(sessionManager);
				} else {
					constructorParameters[propertyAccessor.getIndexIntoConstructorParameters()] = defaultProperties[i++];
				}
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
			// We set propertyAccessor to be the property accessor
			// for the property whose value is contained in this
			// element.  This property may be a scalar or a list
			// property.
			
			// If the property is not found then we currently drop
			// the value.
			// TODO: Keep values even for unknown properties in
			// case plug-ins are installed later that can use the data.
		
			// All elements are expected to be in a namespace beginning
			// "http://jmoney.sf.net".  If the element is a property in an
			// extension property set then the id of the extension property
			// set will be appended.
			
			String namespace = null;
			if (uri.length() == 20) {
				namespace = null;
			} else {
				namespace = uri.substring(21);
			}
			
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
					propertyAccessor = propertySet.getPropertyAccessorGivenLocalNameAndExcludingExtensions(localName);
				} else {
					// Get the property based on the fully qualified name.
					// TODO this is not very efficient.  We combine the propertySetId
					// and the local name, but the first thing the following method
					// does is to split them apart again!
					propertyAccessor = PropertySet.getPropertyAccessor(namespace + "." + localName);
				}
			} catch (PropertyNotFoundException e) {
					// The property no longer exists.
					// TODO: Log this.  When changing the properties,
					// one is supposed to provide upgrader properties
					// for all obsoleted properties.
					// We drop the value.
					// Ignore content
					currentSAXEventProcessor = new IgnoreElementProcessor(sessionManager, this, null);
					return;
			}
			
			Class propertyClass = propertyAccessor.getValueClass();

			map = null;
			id = null;
			
			// See if the 'idref' attribute is specified.
			String idref = atts.getValue("idref");
			if (idref != null) {
				Object value;
				
				if (propertyClass == Currency.class) {
					value = idToCommodityMap.get(idref);
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
				
				currentSAXEventProcessor = new IgnoreElementProcessor(sessionManager, this, value);
			} else {
				if (!propertyClass.isPrimitive() 
						&& propertyClass != String.class
						&& propertyClass != Long.class
						&& propertyClass != Date.class) {
					String propertySetId = atts.getValue("propertySet");
					if (propertySetId == null) {
						// TODO: following call is overkill, because
						// the class should be an exact match, not derived
						// or anything.
						propertySetId = PropertySet.getPropertySet(propertyClass).getId();
					}
					
					// Save the id and appropriate map for this object so that the 
					// object can be added to the map later when the object is created.
					if (Commodity.class.isAssignableFrom(propertyClass)) {
						map = idToCommodityMap;
						id = atts.getValue("id");
					} else if (Account.class.isAssignableFrom(propertyClass)) {
						map = idToAccountMap;
						id = atts.getValue("id");
					}
					
					currentSAXEventProcessor = new ObjectProcessor(sessionManager, this, propertySetId);
				} else {
					// Property class is primative or primative class
					currentSAXEventProcessor = new PropertyProcessor(sessionManager, this, propertyClass);
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
			
			// We can now create the object.
			ExtendableObject extendableObject = (ExtendableObject)propertySet.constructImplementationObject(constructorParameters);
			
			objectKey.setObject(extendableObject);
			
			extendableObject.registerWithIndexes();
			
			// Pass the value back up to the outer element processor.
			if (parent != null) {
				parent.elementCompleted(extendableObject);
			}
			
			// Save the value so that getValue can return it.
			// TODO: Change this method so it returns the value,
			// and replace the getValue method with a getParent method.
			// That would be a little cleaner.
			value = extendableObject;
			
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
			
			if (propertyAccessor == null) {
				throw new RuntimeException("internal error");
			}
			
			// Set the value in our object.  If the property
			// is a list property then the object is added to
			// the list.
			if (!propertyAccessor.getPropertySet().isExtension()) {
				int index = propertyAccessor.getIndexIntoConstructorParameters(); 
				if (index != -1) {
					if (propertyAccessor.isScalar()) {
						if (propertyAccessor.getValueClass().isPrimitive()
								|| propertyAccessor.getValueClass() == String.class
								|| propertyAccessor.getValueClass() == Long.class
								|| propertyAccessor.getValueClass() == Date.class) {
							constructorParameters[index] = value;
						} else {
							constructorParameters[index] = ((ExtendableObject)value).getObjectKey();
							//extendableObject.setPropertyValue(propertyAccessor, value);
						}
					} else {
						// Must be an element in an array.
						//extendableObject.addPropertyValue(propertyAccessor, value);
						((Collection)constructorParameters[index]).add(value);
						
						// For Currency and Account objects, we also add to a map so that
						// references can be resolved.
						if (map != null) {
							map.put(id, value);
						}
					}
				}
			} else {
				// Property is in an extension.
				PropertySet extensionPropertySet = propertyAccessor.getPropertySet();
				Object[] extensionConstructorParameters = (Object[])extensionMap.get(extensionPropertySet);
				if (extensionConstructorParameters == null) {
					extensionConstructorParameters = new Object[extensionPropertySet.getConstructorProperties().size()];
					extensionMap.put(extensionPropertySet, extensionConstructorParameters);
				}

				int index = propertyAccessor.getIndexIntoConstructorParameters(); 
				if (index != -1) {
					if (propertyAccessor.isScalar()) {
						if (propertyAccessor.getValueClass().isPrimitive()
								|| propertyAccessor.getValueClass() == String.class
								|| propertyAccessor.getValueClass() == Long.class
								|| propertyAccessor.getValueClass() == Date.class) {
							extensionConstructorParameters[index] = value;
						} else {
							extensionConstructorParameters[index] = ((ExtendableObject)value).getObjectKey();
						}
					} else {
						// Must be an element in an array.
						((Collection)extensionConstructorParameters[index]).add(value);
					}
				}
			}
		}

		/* (non-Javadoc)
		 * @see net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin.SAXEventProcessor#getValue()
		 */
		public Object getValue() {
			return value;
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
		PropertyProcessor(SessionManager sessionManager, SAXEventProcessor parent, Class propertyClass) {
			super(sessionManager, parent);
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
			} else if (propertyClass.equals(boolean.class)) {
				String s = new String(ch, start, length);
				value = new Boolean(s);
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

		/* (non-Javadoc)
		 * @see net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin.SAXEventProcessor#getValue()
		 */
		public Object getValue() {
			return value;
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
		IgnoreElementProcessor(SessionManager sessionManager, SAXEventProcessor parent, Object value) {
			super(sessionManager, parent);
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
			currentSAXEventProcessor = new IgnoreElementProcessor(sessionManager, this, null);
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

		/* (non-Javadoc)
		 * @see net.sf.jmoney.serializeddatastore.SerializedDatastorePlugin.SAXEventProcessor#getValue()
		 */
		public Object getValue() {
			return value;
		}
	}
	


	// Used for writing
	private Map namespaceMap;  // PropertySet to String (namespace prefix)
	private int accountId;
	private Map accountIdMap;
	
	// Used for reading
	private Map idToCommodityMap;
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
	public void writeSession(final SessionManager sessionManager, final File sessionFile, IWorkbenchWindow window)  {
		// If there is any modified data in the controls in any of the
		// views, then commit these to the database now.
		// TODO: How do we do this?  Should framework call first
		// commitRemainingUserChanges();
		
		try {
			if (/*session.getTransactionCount() < 1000*/ false) {
				// If the session has less than 1000 transactions then it is
				// not worthwhile using a progress monitor.
				// The monitor would flash up so quickly that the
				// user could not read it.
				writeSessionQuietly(sessionManager, sessionFile, null);
			} else {
				IRunnableWithProgress writeSessionRunnable = new IRunnableWithProgress() {
					
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						// Set the number of work units in the monitor where
						// one work unit is writing 500 transactions
						//int workUnits = (int)(session.getTransactionCount()/500);
						int workUnits =  IProgressMonitor.UNKNOWN;
						
						monitor.beginTask(
								SerializedDatastorePlugin.getResourceString("MainFrame.SavingFile") + " " + sessionFile, 
								workUnits);   
						
						try {
							writeSessionQuietly(sessionManager, sessionFile, monitor);
						} catch (Exception ex) {
							throw new InvocationTargetException(ex);
						} finally {
							monitor.done();
						}
					}
					
				};
				
				ProgressMonitorJobsDialog progressDialog = new ProgressMonitorJobsDialog(window.getShell());
				
				try {
					progressDialog.run(true, false, writeSessionRunnable);
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
				
				EventLoopProgressMonitor monitor = new EventLoopProgressMonitor(new NullProgressMonitor());
			}
		} catch (InterruptedException e) {
			// If the user inturrupted the write then we do nothing.
			// Currently this cannot happen because the cancel button is not
			// enabled in the progress dialog, but if the cancel button is enabled
			// then a message should perhaps be displayed here indicating that the
			// file is unusable.
		} catch (Throwable ex) {
			JMoneyPlugin.log(ex);
			fileWriteError(sessionFile, window);
		}
	}
	

	/**
	 * Write session to file.
	 *
	 * @param monitor Monitor into which this method will call
	 * 			the beginTask method and update the progress.
	 * 			This parameter may be null in which this method
	 * 			will write the session without feedback on the progress.
	 */
	// TODO: update the monitor, perhaps by counting the transactions. 
	public void writeSessionQuietly(SessionManager sessionManager, File sessionFile, IProgressMonitor monitor)  
	throws IOException, SAXException, TransformerConfigurationException {

		FileOutputStream fout = new FileOutputStream(sessionFile);
		
		// If the extension is 'xml' then no compression is used.
		// If the extension is 'jmx' then compression is used.
		BufferedOutputStream bout;
		if (sessionFile.getName().endsWith(".xml")) {
			bout = new BufferedOutputStream(fout);
		} else {
			GZIPOutputStream gout = new GZIPOutputStream(fout);
			bout = new BufferedOutputStream(gout);
		}
		
		namespaceMap = new HashMap();
		accountId = 1;
		accountIdMap = new HashMap();
		
		StreamResult streamResult = new StreamResult(bout);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		// SAX2.0 ContentHandler.
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
		serializer.setOutputProperty(OutputKeys.INDENT,"no");
		hd.setResult(streamResult);
		hd.startDocument();
		writeObject(hd, sessionManager.getSession(), "session", Session.class);
		hd.endDocument();
		
		bout.close();
		fout.close();
	}
	

	/**
	 * 
	 * @param hd
	 * @param object
	 * @param elementName
	 * @param propertyType The typed class of the property.  The property may be an object
	 * 			of a class that is derived from this typed class.  If the property is a
	 * 			scalar property then the property type is determined by inspecting the
	 * 			getter and setter methods.  If the property is a list property then the
	 * 			type is determined by inspecting the adder and remover methods. 
	 * @throws SAXException
	 */
	void writeObject(TransformerHandler hd, ExtendableObject object, String elementName, Class propertyType) throws SAXException {
		// Find the property set information for this object.
		PropertySet propertySet = PropertySet.getPropertySet(object.getClass());

		AttributesImpl atts = new AttributesImpl();
		
		// Generate and declare the namespace prefixes.
		// All extension property sets have namespace prefixes.
		// Properties in base and derived property sets must be
		// unique within each object, so are all put in the
		// default namespace.
		atts.clear();
		if (propertyType == Session.class) {
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

		if (propertySet.getImplementationClass() != propertyType) {
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
		for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			if (propertyAccessor.isList()) {
				PropertySet propertySet2 = propertyAccessor.getPropertySet(); 
				if (!propertySet2.isExtension()
						|| object.getExtension(propertySet2) != null) {
					String name = propertyAccessor.getLocalName();
					
					for (Iterator elementIter = object.getPropertyIterator(propertyAccessor); elementIter.hasNext(); ) {
						ExtendableObject listElement = (ExtendableObject)elementIter.next();
						writeObject(hd, listElement, propertyAccessor.getLocalName(), propertyAccessor.getValueClass());
					}
				}
			}
		}
		
		for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
			PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
			if (propertyAccessor.isScalar()) {
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
								text = dateFormat.format(date);
								
								String text2 = new Integer(date.getYear() + 1900).toString() + "."
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
		}
		
		hd.endElement("", "", elementName);
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
		
		// Add the currencies
		JMoneyPlugin.initSystemCurrency(newSession);
		
		// Add the income and expense accounts
		net.sf.jmoney.model.CategoryNode root = oldFormatSession.getCategories().getRootNode();
		for (Enumeration e = root.children(); e.hasMoreElements();) {
			net.sf.jmoney.model.CategoryNode node = (net.sf.jmoney.model.CategoryNode) e.nextElement();
			Object obj = node.getUserObject();
			if (obj instanceof net.sf.jmoney.model.SimpleCategory) {
				net.sf.jmoney.model.SimpleCategory oldCategory = (net.sf.jmoney.model.SimpleCategory) obj;
				IncomeExpenseAccount newCategory = (IncomeExpenseAccount)newSession.createAccount(IncomeExpenseAccountInfo.getPropertySet());
				copyCategoryProperties(oldCategory, newCategory, accountMap);
			}
		}
		
		// Add the capital accounts
		Vector oldAccounts = oldFormatSession.getAccounts();
		for (Iterator iter = oldAccounts.iterator(); iter.hasNext(); ) {
			net.sf.jmoney.model.Account oldAccount = (net.sf.jmoney.model.Account)iter.next();
			
            BankAccount newAccount = (BankAccount)newSession.createAccount(BankAccountInfo.getPropertySet());
			newAccount.setName(oldAccount.getName());
			newAccount.setAbbreviation(oldAccount.getAbbrevation());
			newAccount.setAccountNumber(oldAccount.getAccountNumber());
			newAccount.setBank(oldAccount.getBank());
			newAccount.setComment(oldAccount.getComment());
			newAccount.setCurrency(JMoneyPlugin.getIsoCurrency(newSession, oldAccount.getCurrencyCode()));
			newAccount.setMinBalance(oldAccount.getMinBalance());
			newAccount.setStartBalance(oldAccount.getStartBalance());

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
						Transaction trans = newSession.createTransaction();
						trans.setDate(de.getDate());
						Entry entry1 = trans.createEntry();
						Entry entry2 = trans.createEntry();
						entry1.setAmount(de.getAmount());
						entry2.setAmount(-de.getAmount());
						entry1.setAccount((Account)accountMap.get(de.getOther().getCategory()));
						entry2.setAccount((Account)accountMap.get(de.getCategory()));
						
						copyEntryProperties(de, entry1, statusProperty);
						copyEntryProperties(de.getOther(), entry2, statusProperty);
					} else {
						doubleEntriesPreviouslyFound.add(de);
					}
				} else if (oldEntry instanceof net.sf.jmoney.model.SplittedEntry) {
					net.sf.jmoney.model.SplittedEntry se = (net.sf.jmoney.model.SplittedEntry)oldEntry;
					
					Transaction trans = newSession.createTransaction();
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
				} else {
					Transaction trans = newSession.createTransaction();
					trans.setDate(oldEntry.getDate());
					Entry entry1 = trans.createEntry();
					Entry entry2 = trans.createEntry();
					entry1.setAmount(oldEntry.getAmount());
					entry2.setAmount(-oldEntry.getAmount());
					entry1.setAccount(newAccount);
					if (oldEntry.getCategory() != null) {
					    entry2.setAccount((Account)accountMap.get(oldEntry.getCategory()));
					}
					
					// Put the check, memo, valuta, and status into the account entry only.
					// Assume the creation and description apply to both account and
					// category.
					copyEntryProperties(oldEntry, entry1, statusProperty);
					
					entry2.setCreation(oldEntry.getCreation());
					entry2.setDescription(oldEntry.getDescription());
				}
			}
		}
	}

    /**
	 * Copies category properties across from old to new.  Sub-categories are also
	 * copied across.
	 * 
	 * @param accountMap this and all sub-categories are added to this map, mapping
	 * 			old categories to the new categories
	 */
	private void copyCategoryProperties(net.sf.jmoney.model.SimpleCategory oldCategory, IncomeExpenseAccount newCategory, Map accountMap) {
		accountMap.put(oldCategory, newCategory);
		
		newCategory.setName(oldCategory.getCategoryName());
		
		for (Enumeration e2 = oldCategory.getCategoryNode().children(); e2.hasMoreElements();) {
			net.sf.jmoney.model.CategoryNode subNode = (net.sf.jmoney.model.CategoryNode) e2.nextElement();
			Object obj2 = subNode.getUserObject();
			if (obj2 instanceof net.sf.jmoney.model.SimpleCategory) {
				net.sf.jmoney.model.SimpleCategory oldSubCategory = (net.sf.jmoney.model.SimpleCategory) obj2;
				IncomeExpenseAccount newSubCategory = (IncomeExpenseAccount)newCategory.createSubAccount();
				copyCategoryProperties(oldSubCategory, newSubCategory, accountMap);

				accountMap.put(oldSubCategory, newSubCategory);
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