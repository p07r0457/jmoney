package net.sf.jmoney.copier;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtensionObject;
import net.sf.jmoney.model2.ISessionManager;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.Entry;

import org.eclipse.ui.plugin.*;
import org.osgi.framework.BundleContext;
import java.util.*;

/**
 * The main plugin class to be used in the desktop.
 */
public class CopierPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static CopierPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	private static ISessionManager savedSessionManager = null;
	
	/**
	 * The constructor.
	 */
	public CopierPlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("net.sf.jmoney.copier.CopierPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
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
	public static CopierPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = CopierPlugin.getDefault().getResourceBundle();
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
	 * @param sessionManager
	 */
	public static void setSessionManager(ISessionManager sessionManager) {
		savedSessionManager = sessionManager;
	}

	/**
	 * @return
	 */
	public static ISessionManager getSessionManager() {
		return savedSessionManager;
	}
	
    /**
     * Copy the contents of one session to another.
     *
     * The two sessions may be interfaces into different implementations.
     * This is therefore more than just a deep copy.  It is a conversion.
     */
    public void populateSession(Session newSession, Session oldSession) {
        Map objectMap = new Hashtable();
        
        PropertySet propertySet = PropertySet.getPropertySet(oldSession.getClass());
    	populateObject(propertySet, oldSession, newSession, objectMap);
/*
    	// Add the accounts
        for (Iterator iter = oldSession.getAccountIterator(); iter.hasNext(); ) {
            Account oldAccount = (Account) iter.next();
            
/ *            
            if (oldAccount instanceof CapitalAccount) {
            	CapitalAccount newAccount = (CapitalAccount)newSession.createAccount(JMoneyPlugin.getCapitalAccountPropertySet());
            	populateCapitalAccount(newSession, (CapitalAccount)oldAccount, newAccount, accountMap);
            } else {
                IncomeExpenseAccount newCategory = (IncomeExpenseAccount)newSession.createAccount(JMoneyPlugin.getIncomeExpenseAccountPropertySet());
                populateIncomeExpenseAccount(newSession, (IncomeExpenseAccount)oldAccount, newCategory, accountMap);
            }
* /
            PropertySet propertySet = PropertySet.getPropertySet(oldAccount.getClass());
        	Account newAccount = newSession.createAccount(propertySet);
        	populateObject(propertySet, oldAccount, newAccount, objectMap);

        }

        // Add the transactions and entries
        for (Iterator iter = oldSession.getTransactionIterator(); iter.hasNext(); ) {
            Transaction oldTransaction = (Transaction)iter.next();
            
            Transaction trans = newSession.createTransaction();
            trans.setDate(oldTransaction.getDate());
            
            for (Iterator entryIter = oldTransaction.getEntryIterator(); entryIter.hasNext(); ) {
                Entry oldEntry = (Entry)entryIter.next();
                
                Entry newEntry = trans.createEntry();
                newEntry.setAmount(oldEntry.getAmount());
                newEntry.setAccount((Account)objectMap.get(oldEntry.getAccount()));
                newEntry.setCheck(oldEntry.getCheck());
                newEntry.setCreation(oldEntry.getCreation());
                newEntry.setDescription(oldEntry.getDescription());
                newEntry.setMemo(oldEntry.getMemo());
                newEntry.setValuta(oldEntry.getValuta());
            }
        }
*/        
    }
 /*   
    private void populateIncomeExpenseAccount(Session newSession, IncomeExpenseAccount oldCategory, IncomeExpenseAccount newCategory, Map categoryMap) {
        newCategory.setName(oldCategory.getName());
        
        categoryMap.put(oldCategory, newCategory);
        
        for (Iterator iter = oldCategory.getSubAccountIterator(); iter.hasNext(); ) {
        	IncomeExpenseAccount oldSubCategory = (IncomeExpenseAccount)iter.next();
            
            IncomeExpenseAccount newSubCategory = (IncomeExpenseAccount)newCategory.createSubAccount();
            populateIncomeExpenseAccount(newSession, oldSubCategory, newSubCategory, categoryMap);
        }
    }

    private void populateCapitalAccount(Session newSession, CapitalAccount oldAccount, CapitalAccount newAccount, Map categoryMap) {
        newAccount.setName(oldAccount.getName());
        newAccount.setAbbreviation(oldAccount.getAbbreviation());
        newAccount.setAccountNumber(oldAccount.getAccountNumber());
        newAccount.setBank(oldAccount.getBank());
        newAccount.setComment(oldAccount.getComment());
        newAccount.setCurrency(newSession.getCurrencyForCode(oldAccount.getCurrency().getCode()));
        newAccount.setMinBalance(oldAccount.getMinBalance());
        newAccount.setStartBalance(oldAccount.getStartBalance());
        
        categoryMap.put(oldAccount, newAccount);
        
        for (Iterator iter = oldAccount.getSubAccountIterator(); iter.hasNext(); ) {
            CapitalAccount oldSubAccount = (CapitalAccount)iter.next();
            
            CapitalAccount newSubAccount = (CapitalAccount)newAccount.createSubAccount();
            populateCapitalAccount(newSession, oldSubAccount, newSubAccount, categoryMap);
        }
    }
*/    
    private void populateObject(PropertySet propertySet, ExtendableObject oldObject, ExtendableObject newObject, Map objectMap) {
    	
    	// For all non-extension properties (including properties
    	// in base classes), read the property value from the
    	// old object and write it to the new object.
    	for (Iterator iter = propertySet.getPropertyIterator3(); iter.hasNext(); ) {
    		PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
    		if (!propertyAccessor.getPropertySet().isExtension()) {
    			copyProperty(propertyAccessor, oldObject, newObject, objectMap);
    		}
    	}
    	
    	// Now copy the extensions.  This is done by looping through the extensions
    	// in the old object and, for every extension that exists in the old object,
    	// copy the properties to the new object.
    	for (Iterator extensionIter = oldObject.getExtensionIterator(); extensionIter.hasNext(); ) {
    		Map.Entry mapEntry = (Map.Entry)extensionIter.next();
    		PropertySet extensionPropertySet = (PropertySet)mapEntry.getKey();
    		ExtensionObject extension = (ExtensionObject)mapEntry.getValue();
    		for (Iterator propertyIter = extensionPropertySet.getPropertyIterator1(); propertyIter.hasNext(); ) {
    			PropertyAccessor propertyAccessor = (PropertyAccessor)propertyIter.next();
    			copyProperty(propertyAccessor, oldObject, newObject, objectMap);
    		}
    	}
    
    	// TODO: This code works because
    	// Commodity and Account objects are the
    	// only objects referenced by other objects.
    	// Plug-ins could change this, thus breaking this code.
    	if (oldObject instanceof Commodity
    			|| oldObject instanceof Account) {
    		objectMap.put(oldObject, newObject);
    	}
    }

    private void copyProperty(PropertyAccessor propertyAccessor, ExtendableObject oldAccount, ExtendableObject newAccount, Map objectMap) {
    	if (propertyAccessor.isScalar()) {
    		newAccount.setPropertyValue(
    				propertyAccessor,
					oldAccount.getPropertyValue(propertyAccessor));
    	} else {
    		// Property is a list property.
    		for (Iterator listIter = oldAccount.getPropertyIterator(propertyAccessor); listIter.hasNext(); ) {
    			ExtendableObject oldSubObject = (ExtendableObject)listIter.next();
    			PropertySet listElementPropertySet = PropertySet.getPropertySet(oldSubObject.getClass());
    			ExtendableObject newSubObject = newAccount.createObject(propertyAccessor, listElementPropertySet);
    			populateObject(listElementPropertySet, oldSubObject, newSubObject, objectMap);
    		}
    	}
    }
}
