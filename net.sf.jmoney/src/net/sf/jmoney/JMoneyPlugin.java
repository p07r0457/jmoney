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

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import net.sf.jmoney.isocurrencies.IsoCurrenciesPlugin;
import net.sf.jmoney.model2.*;
import net.sf.jmoney.model2.Currency;

/**
 * The main plugin class to be used in the desktop.
 */
public class JMoneyPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static JMoneyPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
    private ISessionManager sessionManager = null;

    private Vector sessionChangeListeners = new Vector();
    
    // Create a listener that listens for changes to the new session.
    private SessionChangeFirerListener sessionChangeFirerListener =
    	new SessionChangeFirerListener() {
    		public void sessionChanged(ISessionChangeFirer firer) {
    	        if (!sessionChangeListeners.isEmpty()) {
    	        	// Take a copy of the listener list.  By doing this we
    	        	// allow listeners to safely add or remove listeners.
    	        	SessionChangeListener listenerArray[] = new SessionChangeListener[sessionChangeListeners.size()];
    	        	sessionChangeListeners.copyInto(listenerArray);
    	        	for (int i = 0; i < listenerArray.length; i++) {
    	        		firer.fire(listenerArray[i]);
    	        	}
    	        }
    			
    		}
    	};

    private static PropertySet commodityPropertySet = null;
    private static PropertySet currencyPropertySet = null;	
	private static PropertySet accountPropertySet = null;
	private static PropertySet capitalAccountPropertySet = null;
	private static PropertySet incomeExpenseAccountPropertySet = null;
	private static PropertySet transactionPropertySet = null;
	private static PropertySet entryPropertySet = null;
    
    /**
	 * The constructor.
	 */
	public JMoneyPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle   = ResourceBundle.getBundle("net.sf.jmoney.resources.Language");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		
		PropertySet.init();
		Propagator.init();
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// Load the session that was last loaded, if any.
		// Get the plug-in id, if any, that was saved in the preferences when JMoney
		// was last closed, search the 'datastor.es' extensions for this plug-in and
		// create an appropriate session object, then set this session as the current
		// session.
/*		
		String pluginId2 = getPreferenceStore().getString("currentSessionPlugin");
		String pluginId = getPluginPreferences().getString("currentSessionPlugin");
		IPersistableElement;
		if (pluginId.length() != 0) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.datastores");
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				if (extensions[i].getNamespace().equals(pluginId)) {
					IConfigurationElement[] elements =
						extensions[i].getConfigurationElements();
					for (int j = 0; j < elements.length; j++) {
						if (elements[j].getName().equals("session")) {
							try {
								ISessionManager listener = (ISessionManager)elements[j].createExecutableExtension("class");
								
						    	listener.restore(getWorkbench().getActiveWorkbenchWindow());

						    	// The session object will have initialized itself from 
								// data stored in the preferences.  We need only now
								// set this session as the current session.
								JMoneyPlugin.getDefault().setSession(listener);
								break;
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
*/		
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
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
	 */
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
		String iconPath = "";
		try {
			URL installURL = getDefault().getBundle().getEntry("/");
			URL url = new URL(installURL, iconPath + name);
			return ImageDescriptor.createFromURL(url).createImage();
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor().createImage();
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
    public ISessionManager getSessionManager() {
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
    public void setSessionManager(ISessionManager newSessionManager) {
        // It is up to the caller to ensure that the previous session
        // has been closed.

        if (sessionManager == newSessionManager)
            return;
        ISessionManager oldSessionManager = sessionManager;
        sessionManager = newSessionManager;
        
    	// If the list of commodities is empty then load
    	// the full list of ISO currencies.
        if (newSessionManager != null) {
        	if (!getSession().getCommodityIterator().hasNext()) {
        		initSystemCurrencies(getSession());
        		getSession().registerUndoableChange("add ISO currencies");
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
        	SessionChangeListener listenerArray[] = new SessionChangeListener[sessionChangeListeners.size()];
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
        if (oldSessionManager != null) {
        	oldSessionManager.getSession().removeSessionChangeFirerListener(sessionChangeFirerListener);
        }
        if (newSessionManager != null) {
        	newSessionManager.getSession().addSessionChangeFirerListener(sessionChangeFirerListener);
        }
	}

	public static void initSystemCurrencies(Session session) {
	    ResourceBundle NAME =
	    	ResourceBundle.getBundle("net.sf.jmoney.isocurrencies.Currency");

		InputStream in = IsoCurrenciesPlugin.class.getResourceAsStream("Currencies.txt");
		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
		try {
			String line = buffer.readLine();
			while (line != null) {
				String code = line.substring(0, 3);
				byte d;
				try {
					d = Byte.parseByte(line.substring(4, 5));
				} catch (Exception ex) {
					d = 2;
				}
				byte decimals = d;
				String name = NAME.getString(code);
				
				Currency newCurrency = (Currency)session.createCommodity(
						JMoneyPlugin.getCurrencyPropertySet());
				
				newCurrency.setName(name);
				newCurrency.setCode(code);
				newCurrency.setDecimals(decimals);

		        // Set the default currency to "USD".
				if (code.equals("USD")) {
					session.setDefaultCurrency(newCurrency);
				}
				
				line = buffer.readLine();
			}
			
		} catch (IOException ioex) {
		}
	}

	public void addSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.add(l);
    }
    
    public void removeSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.remove(l);
    }

    // Preferences
    
	/** 
	 * Initializes a preference store with default preference values 
	 * for this plug-in.
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault("dateFormat", "yyyy-MM-dd");
	}
	
    public String getDateFormat() {
    	// The following line cannot return a null value because if
    	// no value is set then the default value set in
    	// the above initializeDefaultPreferences method will be returned.
    	return getPreferenceStore().getString("dateFormat");
    }

	/**
	 * @return
	 */
	public static PropertySet getCommodityPropertySet() {
		if (commodityPropertySet == null) {
			try {
				commodityPropertySet = PropertySet.getPropertySet("net.sf.jmoney.commodity");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return commodityPropertySet;
	}

	/**
	 * @return
	 */
	public static PropertySet getCurrencyPropertySet() {
		if (currencyPropertySet == null) {
			try {
				currencyPropertySet = PropertySet.getPropertySet("net.sf.jmoney.currency");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return currencyPropertySet;
	}
	
	/**
	 * @return
	 */
	public static PropertySet getAccountPropertySet() {
		if (accountPropertySet == null) {
			try {
				accountPropertySet = PropertySet.getPropertySet("net.sf.jmoney.account");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return accountPropertySet;
	}

	/**
	 * @return
	 */
	public static PropertySet getCapitalAccountPropertySet() {
		if (capitalAccountPropertySet == null) {
			try {
				capitalAccountPropertySet = PropertySet.getPropertySet("net.sf.jmoney.capitalAccount");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return capitalAccountPropertySet;
	}
	
	/**
	 * @return
	 */
	public static PropertySet getIncomeExpenseAccountPropertySet() {
		if (incomeExpenseAccountPropertySet == null) {
			try {
				incomeExpenseAccountPropertySet = PropertySet.getPropertySet("net.sf.jmoney.categoryAccount");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return incomeExpenseAccountPropertySet;
	}
	
	/**
	 * @return
	 */
	public static PropertySet getTransactionPropertySet() {
		if (transactionPropertySet == null) {
			try {
				transactionPropertySet = PropertySet.getPropertySet("net.sf.jmoney.transaction");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return transactionPropertySet;
	}	

	/**
	 * @return
	 */
	public static PropertySet getEntryPropertySet() {
		if (entryPropertySet == null) {
			try {
				entryPropertySet = PropertySet.getPropertySet("net.sf.jmoney.entry");
			} catch (PropertySetNotFoundException e) {
				e.printStackTrace();
				throw new RuntimeException("internal error");
			}
		}
		
		return entryPropertySet;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
/*    
	public String getFactoryId() {
		if (session != null) {
			return session.getFactoryId();
		}
		return null;
	}
*/
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#saveState(org.eclipse.ui.IMemento)
	 */
/*    
	public void saveState(IMemento memento) {
		if (session != null) {
			IPersistableElement pe = (IPersistableElement)session.getAdapter(IPersistableElement.class);
			memento.putString("currentSessionFactoryId", pe.getFactoryId());
			pe.saveState(memento.createChild("currentSession"));
		}
	}
		*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#saveState(org.eclipse.ui.IMemento)
	 */
/*
    public void init(IMemento memento) {
		if (memento != null) {
			String factoryId = memento.getString("currentSessionFactoryId"); 
			if (factoryId != null && factoryId.length() != 0) {
				// Search for the factory.
				IExtensionRegistry registry = Platform.getExtensionRegistry();
				IExtensionPoint extensionPoint = registry.getExtensionPoint("org.eclipse.ui.elementFactories");
				IExtension[] extensions = extensionPoint.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IConfigurationElement[] elements =
						extensions[i].getConfigurationElements();
					for (int j = 0; j < elements.length; j++) {
						if (elements[j].getName().equals("factory")) {
							if (elements[j].getAttribute("id").equals(factoryId)) {
								try {
									IElementFactory listener = (IElementFactory)elements[j].createExecutableExtension("class");
									
									ISessionManager session = (ISessionManager)listener.createElement(memento.getChild("currentSession"));
									// The session object has been created and initialized from 
									// the data stored in the memento.  We need only now
									// set this session as the current session.
									// TODO set this directly???
									JMoneyPlugin.getDefault().setSession(session);
									break;
								} catch (CoreException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
	}
		*/

    /**
     * If there is any modified data in the controls in any of the
     * views, then commit these to the database now.
     */
//    void commitRemainingUserChanges();
}
