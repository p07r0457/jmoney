package net.sf.jmoney.copier;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountImpl;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ISessionManager;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.IncomeExpenseAccountImpl;
import net.sf.jmoney.model2.ObjectLockedForEditException;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

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
    public void populateSession(Session newSession, Session oldSession)
    throws ObjectLockedForEditException {    
        Map categoryMap = new Hashtable();
        
        // Add the accounts
        for (Iterator iter = oldSession.getAccountIterator(); iter.hasNext(); ) {
            Account oldAccount = (Account) iter.next();
            
            if (oldAccount instanceof CapitalAccount) {
            	CapitalAccountImpl newAccount = (CapitalAccountImpl)newSession.createAccount(JMoneyPlugin.getCapitalAccountPropertySet());
            	populateAndCommitAccount(newSession, (CapitalAccount)oldAccount, newAccount, categoryMap);
            } else {
                IncomeExpenseAccountImpl newCategory = (IncomeExpenseAccountImpl)newSession.createAccount(JMoneyPlugin.getIncomeExpenseAccountPropertySet());
                populateAndCommitSimpleCategory(newSession, (IncomeExpenseAccount)oldAccount, newCategory, categoryMap);
            }            	
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
                newEntry.setAccount((Account)categoryMap.get(oldEntry.getAccount()));
                newEntry.setCheck(oldEntry.getCheck());
                newEntry.setCreation(oldEntry.getCreation());
                newEntry.setDescription(oldEntry.getDescription());
                newEntry.setMemo(oldEntry.getMemo());
                newEntry.setValuta(oldEntry.getValuta());
                
                // Copy the extensions.
/* TODO: sort out how best to do this.                
                for (Iterator pluginIter = JMoneyPlugin.getEntryPropertySet().BeanContainer.getPluginIterator(); pluginIter.hasNext(); ) { 
                    PluginWrapper plugin = (PluginWrapper)pluginIter.next();
                    
                    AbstractEntryExtension oldExtension = oldEntry.getExtension(plugin);
                    if (oldExtension != null) {
                        AbstractEntryExtension newExtension = newEntry.getExtension(plugin);
                        // Copy the properties from one to the other.
                        plugin.copyEntryProperties(oldExtension, newExtension);
                    }
                }
*/                
            }
        }
    }
    
    private void populateAndCommitSimpleCategory(Session newSession, IncomeExpenseAccount oldCategory, IncomeExpenseAccountImpl newCategory, Map categoryMap)
    throws ObjectLockedForEditException {
        newCategory.setName(oldCategory.getName());
        
        categoryMap.put(oldCategory, newCategory);
        
        for (Iterator iter = oldCategory.getSubAccountIterator(); iter.hasNext(); ) {
        	IncomeExpenseAccount oldSubCategory = (IncomeExpenseAccount)iter.next();
            
            IncomeExpenseAccountImpl newSubCategory = (IncomeExpenseAccountImpl)newCategory.createSubAccount();
            populateAndCommitSimpleCategory(newSession, oldSubCategory, newSubCategory, categoryMap);
        }
    }

    private void populateAndCommitAccount(Session newSession, CapitalAccount oldAccount, CapitalAccountImpl newAccount, Map categoryMap)
    throws ObjectLockedForEditException {
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
            
            CapitalAccountImpl newSubAccount = (CapitalAccountImpl)newAccount.createSubAccount();
            populateAndCommitAccount(newSession, oldSubAccount, newSubAccount, categoryMap);
        }
    }
}
