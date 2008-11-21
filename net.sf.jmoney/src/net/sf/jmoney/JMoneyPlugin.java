/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyInfo;
import net.sf.jmoney.model2.CurrentSessionChangeListener;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ISessionChangeFirer;
import net.sf.jmoney.model2.ISessionFactory;
import net.sf.jmoney.model2.Propagator;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeFirerListener;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.views.TreeNode;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class JMoneyPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "net.sf.jmoney"; //$NON-NLS-1$

    public static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("net.sf.jmoney/debug")); //$NON-NLS-1$ //$NON-NLS-2$

	//The shared instance.
	private static JMoneyPlugin plugin;
	//Resource bundle.
	@Deprecated
	private ResourceBundle resourceBundle;
	
    private DatastoreManager sessionManager = null;

    private Vector<CurrentSessionChangeListener> sessionChangeListeners = new Vector<CurrentSessionChangeListener>();

    /**
	 * A context that is active if a session is open and inactive if no session
	 * is open. This context is used to set the enabled state of menu and
	 * toolbar items.
	 */
	private IContextActivation sessionOpenContext = null;
	  
    // Create a listener that listens for changes to the new session.
    private SessionChangeFirerListener sessionChangeFirerListener =
    	new SessionChangeFirerListener() {
    		public void sessionChanged(ISessionChangeFirer firer) {
    	        if (!sessionChangeListeners.isEmpty()) {
    	        	// Take a copy of the listener list.  By doing this we
    	        	// allow listeners to safely add or remove listeners.
    	        	SessionChangeListener listenerArray[] = new SessionChangeListener[sessionChangeListeners.size()];
    	        	sessionChangeListeners.copyInto(listenerArray);
    	        	for (SessionChangeListener listener: listenerArray) {
    	        		firer.fire(listener);
    	        	}
    	        }
    			
    		}
    	};

    /**
	 * The constructor.
	 */
	public JMoneyPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle   = ResourceBundle.getBundle("net.sf.jmoney.resources.Language"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		PropertySet.init();
		Propagator.init();
		TreeNode.init();
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static JMoneyPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 * @deprecated Use Messages in package net.sf.jmoney.resources.
	 */
	@Deprecated
	public static String getResourceString(String key) {
		ResourceBundle bundle = JMoneyPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public static Image createImage(String name) {
//		String iconPath = "icons/";
		String iconPath = ""; //$NON-NLS-1$
		try {
			URL installURL = getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
			URL url = new URL(installURL, iconPath + name);
			return ImageDescriptor.createFromURL(url).createImage();
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor().createImage();
		}
	}

	public static ImageDescriptor createImageDescriptor(String name) {
		// Make above call this, or remove above
//		String iconPath = "icons/";
		String iconPath = ""; //$NON-NLS-1$
		try {
			URL installURL = getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
			URL url = new URL(installURL, iconPath + name);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	/**
	 * Log status to log the of this plug-in.
	 */	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Log exception to the log of this plug-in.
	 * 
	 * @param e Exception to log
	 */
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, JMoneyPlugin.PLUGIN_ID, IStatus.ERROR, "Internal errror", e)); //$NON-NLS-1$
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	@Deprecated
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
    public DatastoreManager getSessionManager() {
        return sessionManager;
    }
   
    /**
	 * Saves the old session.
	 * Returns false if canceled by user or the save fails.
	 */
	public boolean saveOldSession(IWorkbenchWindow window) {
		if (sessionManager == null) {
			return true;
		} else {
			return sessionManager.canClose(window);
		}
	}
	
	// Helper method
    // TODO: see if we really need this method.
    public Session getSession() {
        return sessionManager == null 
			? null 
			: sessionManager.getSession();
    }
    
    /**
     * Sets the Session object.  The session object contains the accounting
     * data so this method will replace the accounting data in the framework
     * with a new set of accounting data.  This method is normally called
     * only by plug-ins that implement a datastore when accounting data
     * is loaded.
     *
     * To avoid doing too much work and user input before setting the new 
     * session, only to find that the
     * user does not want to close the previous session, plug-in actions
     * that expect to set a new session should call canClose on the previous
     * session before preparing the new session.  It is the caller's
     * responsibility to ensure that
     * both canClose() and close() are called on the previous session.
     * This method will not close any previously set session.
     */
    public void setSessionManager(DatastoreManager newSessionManager) {
        // It is up to the caller to ensure that the previous session
        // has been closed.

        if (sessionManager == newSessionManager)
            return;
        DatastoreManager oldSessionManager = sessionManager;
        sessionManager = newSessionManager;
        
    	/*
		 * JMoney depends on having at least one currency, which must also be
		 * set as the default currency. If there is no default currency then
		 * this must be a new datastore and we must set a default currency.
		 */
        if (newSessionManager != null) {
        	if (getSession().getDefaultCurrency() == null) {
        		initSystemCurrency(getSession());
        	}
        }

        // It is possible, tho I can't think why, that a listener who
        // we tell of a change in the current session will modify either
        // the old or the new session.
        // The correct way of handling this is:
        // - if a change is made to the old session then only those
        //   listeners that have not been told of the change of session
        //   should be told.
        // - if a change is made to the new session then only those
        //	 listeners that have already been told of the change of session
        //   (including the listener that made the change) should be told
        //   of the change.
        // This code handles this correctly.
        
        // We do not support the scenario where a listener replaces the
        // session itself while being notified of a change in the session.
        // Any attempt to do this will cause an exception to be thrown.
        // TODO: Throw this exception.
        
        // If a listener adds a further listener then the correct
        // way of handling this is for the new listener to start
        // recieving change notifications immediately.  This includes
        // changes made to the session by the listener that had added
        // the new listener and also changes made by other listeners that
        // had not, at the time the new listener had been created,
        // been notified of the change in the current session.

        // TODO: Implement the above or decide on a design and what
        // restrictions we impose.
        
        if (!sessionChangeListeners.isEmpty()) {
        	// Take a copy of the listener list.  By doing this we
        	// allow listeners to safely add or remove listeners.
        	CurrentSessionChangeListener listenerArray[] = new CurrentSessionChangeListener[sessionChangeListeners.size()];
        	sessionChangeListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		listenerArray[i].sessionReplaced(
        				oldSessionManager == null ? null : oldSessionManager.getSession(), 
        				newSessionManager == null ? null : newSessionManager.getSession()
        		);
        	}
        }
        
        // Stop listening to the old session and start listening to the
        // new session for changes within the session.
		
        IContextService contextService = (IContextService) PlatformUI.getWorkbench().getService(IContextService.class);

		if (oldSessionManager != null) {
        	oldSessionManager.removeSessionChangeFirerListener(sessionChangeFirerListener);
        	contextService.deactivateContext(sessionOpenContext);
        	sessionOpenContext = null;
        }
        if (newSessionManager != null) {
        	newSessionManager.addSessionChangeFirerListener(sessionChangeFirerListener);
        	sessionOpenContext = contextService.activateContext("net.sf.jmoney.sessionOpen"); //$NON-NLS-1$
        }
	}

    /**
     * Get the corresponding ISO currency for "code". If "session" already
     * contains such a currency this currency is returned. Otherwise, we
     * check our list of ISO 4217 currencies and we create a new currency
     * instance for "session".
     * 
     * @param session Session object which will contain the currency
     * @param code ISO currency code
     * @return Currency for "code"
     */
    public static Currency getIsoCurrency(Session session, String code) {
        // Check if the currency already exists for this session.
        Currency result = session.getCurrencyForCode(code);
        if (result != null) return result;

        // Find the currency in our list of ISO 4217 currencies
        ResourceBundle res = ResourceBundle.getBundle("net.sf.jmoney.resources.Currency"); //$NON-NLS-1$
        byte decimals = 2;
        String name = null;
        try{
        	name = res.getString(code);
        }catch (Exception e){
        	//Problem when retrieve the name return null
        	return null;
        }
        try {
            InputStream in = JMoneyPlugin.class.getResourceAsStream("Currencies.txt"); //$NON-NLS-1$
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
            for (String line = buffer.readLine(); line != null; line = buffer.readLine()) {
                if (line.substring(0, 3).equals(code)) {
                	// The Currencies.txt file does not contain the number of decimals
                	// for every currency.  If no number is in the file then a StringIndexOutOfBoundsException
                	// will be thrown and we assume two decimal places.
                	try {
                		decimals = Byte.parseByte(line.substring(4, 5));
                	} catch (StringIndexOutOfBoundsException e) {
                		decimals = 2;
                	}
                }
            }
        } catch (IOException ioex) {
            log(ioex);
        } catch (NumberFormatException nfex) {
            log(nfex);
        }

        result = session.createCommodity(CurrencyInfo.getPropertySet());
        result.setCode(code);
        result.setName(name);
        result.setDecimals(decimals);

        return result;
    }

    /**
     * Whenever a new session is created, JMoney will set a single initial
     * currency.  The currency is taken from our list of ISO 4217
     * currencies and chosen using information from the default locale.
     * This currency is also set as the default currency.
     * <P>
     * By doing this, we minimize the number of steps that a new JMoney
     * user must take to get started.  If a user only ever uses a single
     * currency then the user may never have to worry about currencies
     * and may never see a currency selection control.
     * 
     * @param session
     */
	public static void initSystemCurrency(Session session) {
        Locale defaultLocale = Locale.getDefault();
        NumberFormat format = NumberFormat.getCurrencyInstance(defaultLocale);
        String code = format.getCurrency().getCurrencyCode();
        Currency currency = getIsoCurrency(session, code);
        if (currency == null) {
        	// JMoney depends on a default currency
        	currency = getIsoCurrency(session, "USD"); //$NON-NLS-1$
        }
        
        /*
         * Note that although we are modifying the datastore,
         * we do not make this an undoable operation.  The user
         * did not set this currency and the user should not be
         * able to undo it.
         */
        session.setDefaultCurrency(currency);
    }

	/**
	 * Adds a change listener.
	 * <P>
	 * The listener is active only for as long as the given control exists.  When the
	 * given control is disposed, the listener is removed and will receive no more
	 * notifications.
	 * <P>
	 * This method is generally used when a listener is used to update contents in a
	 * control.  Typically multiple controls are updated by a listener and the parent
	 * composite control is passed to this method.
	 * 
	 * @param listener
	 * @param control
	 */
	public void addSessionChangeListener(final CurrentSessionChangeListener listener, Control control) {
        sessionChangeListeners.add(listener);
        
		// Remove the listener when the given control is disposed.
		control.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				sessionChangeListeners.remove(listener);
			}
		});
    }

    // Preferences
    
    /**
     * Get the format to be used for dates.  This format is
     * compatible with the VerySimpleDateFormat class.
     * The format is read from the preference store.
     */
    public String getDateFormat() {
    	/*
		 * The following line cannot return a null value, even if the user did
		 * not set a value, because a default value is set. The default value is
		 * set by by JMoneyPreferenceInitializer (an extension to the
		 * org.eclipse.core.runtime.preferences extension point).
		 */
    	return getPreferenceStore().getString("dateFormat"); //$NON-NLS-1$
    }

	/**
	 * Given a memento containing the data needed to open a session,
	 * return the session.  If the session is already open
	 * then return the session, otherwise the session is opened
	 * by this method and returned.
	 *  
	 * @param memento
	 * @return
	 */
	public static Session openSession(IMemento memento) {
		if (memento != null) {
			// This is a kludge.  Only one session can be open at a time,
			// therefore all views that need a session will save the same
			// data in the session memento.  Therefore, if a session is open,
			// just return that.  We know it is the right session.
			if (getDefault().getSession() != null) {
				return getDefault().getSession();
			}
			
			String factoryId = memento.getString("currentSessionFactoryId");  //$NON-NLS-1$
			if (factoryId != null && factoryId.length() != 0) {
				// Search for the factory.
				IExtensionRegistry registry = Platform.getExtensionRegistry();
				for (IConfigurationElement element: registry.getConfigurationElementsFor("org.eclipse.ui.elementFactories")) { //$NON-NLS-1$
					if (element.getName().equals("factory")) { //$NON-NLS-1$
						if (element.getAttribute("id").equals(factoryId)) { //$NON-NLS-1$
							try {
								ISessionFactory listener = (ISessionFactory)element.createExecutableExtension("class"); //$NON-NLS-1$

								// Create and initialize the session object from 
								// the data stored in the memento.
								listener.openSession(memento.getChild("currentSession")); //$NON-NLS-1$
								return getDefault().getSession();
							} catch (CoreException e) {
								// Could not create the factory given by the 'class' attribute
								// Log the error and start JMoney with no open session.
								e.printStackTrace();
							}
							break;
						}
					}
				}
			}
		}
		
    	return null;
	}

	/**
	 * Helper method to compare two objects.  Either or both
	 * the objects may be null.  If both objects are null,
	 * they are considered equal.
	 * 
	 * @param object
	 * @param object2
	 * @return
	 */
	public static boolean areEqual(Object object1, Object object2) {
		return (object1 == null)
			? (object2 == null)
					: object1.equals(object2);
	}
}
