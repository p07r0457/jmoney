/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.serializeddatastore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;

import net.sf.jmoney.model2.*;
import net.sf.jmoney.isocurrencies.IsoCurrenciesPlugin;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Holds the fields that will be saved in a file.
 */
public class SessionImpl extends ExtendableObjectHelperImpl implements ISessionManagement, Serializable {

    protected Currency defaultCurrency;
    
    protected File sessionFile = null;
    
    protected Vector accounts = new Vector();

    protected Vector categories = new Vector();

    protected Vector transactions = new Vector();

    protected static final ResourceBundle NAME =
    ResourceBundle.getBundle("net.sf.jmoney.isocurrencies.Currency");
    protected Hashtable currencies = new Hashtable();
    protected Object[] sortedCurrencies = null;
        
    protected transient boolean modified = false;

//    protected transient PropertyChangeSupport changeSupport =
//        new PropertyChangeSupport(this);
    protected transient Vector sessionChangeListeners = new Vector();
    protected transient Vector sessionChangeFirerListeners = new Vector();

    
    /**
     * Default constructor required by serialization.
     */
    public SessionImpl() {
//        this.serializerPlugin = serializerPlugin;
 //       this.userProperties = userProperties;

        initSystemCurrencies();
        
//      defaultCurrency = getCurrencyForCode(userProperties.getString("USD"));
        defaultCurrency = getCurrencyForCode("USD");
    }

    // TODO: decide how we implement saving of session properties.
    // This gives problems in JDO.
	protected boolean isMutable() {
		return false;
	}

	protected IExtendableObject getOriginalObject() {
		// This method should be called only if isMutable returns true,
		// which it never does.  However, we must provide an implementation.
		throw new RuntimeException("should never be called");
	}
	
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.session";
	}
	
    public Currency getDefaultCurrency() {
//      return getCurrencyForCode(userProperties.getDefaultCurrency());
        return defaultCurrency;
    }
    
    public void setDefaultCurrency(Currency defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
    
	/**
	 * @return the available currencies.
	 */
/*    
	public Object[] getAvailableCurrencies() {
		if (sortedCurrencies == null) {
			sortedCurrencies = currencies.values().toArray();
			Arrays.sort(sortedCurrencies);
		}
		return sortedCurrencies;
	}
*/
	/**
	 * @param code the currency code.
	 * @return the corresponding currency.
	 */
	public Currency getCurrencyForCode(String code) {
		if (currencies == null)
			initSystemCurrencies();
		return (Currency) currencies.get(code);
	}

	private void initSystemCurrencies() {
		InputStream in = IsoCurrenciesPlugin.class.getResourceAsStream("Currencies.txt");
		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
		try {
			String line = buffer.readLine();
			String c;
			byte d;
			while (line != null) {
				c = line.substring(0, 3);
				d = 2;
				try {
					d = Byte.parseByte(line.substring(4, 5));
				} catch (Exception ex) {
				}
                                
                                String name = NAME.getString(c);

				currencies.put(c, new CurrencyImpl(name, c, d));
				line = buffer.readLine();
			}
		} catch (IOException ioex) {
		}
	}

    // TODO: how do I stop these being serialized???
    // (For time being, 'A' added to cause property name mismatch).
    public File getFile() {
        return sessionFile;
    }
    
    public void setFileA(File file) {
        this.sessionFile = file;
        
        // The brief description of this session contains the file name, so we must
        // fire a change so views that show this session description are updated.
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.sessionPropertyChange("briefDescription", null, getBriefDescription());
        		}
       		});
    }
    
    public Iterator getCommodityIterator() {
/*    	
		if (sortedCurrencies == null) {
			sortedCurrencies = currencies.values().toArray();
			Arrays.sort(sortedCurrencies);
		}
*/		
		return currencies.values().iterator();
    }
   
    public Iterator getAccountIterator() {
        return new Iterator() {
        	Iterator iter = accounts.iterator();
        	boolean inAccounts = true;
        	
			public boolean hasNext() {
				if (iter.hasNext()) {
					return true;
				} else if (inAccounts) {
					inAccounts = false;
					iter = categories.iterator();
					return iter.hasNext();
				} else {
					return false; 
				}
			}
			public Object next() {
				return iter.next();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }
   
    public Iterator getCapitalAccountIterator() {
        return accounts.iterator();
    }
   
    public Iterator getIncomeExpenseAccountIterator() {
        return categories.iterator();
    }
   
    public Iterator getTransaxionIterator() {
        return transactions.iterator();
    }
   
    public Iterator getTransactionIterator() {
        return transactions.iterator();
    }
   
    // This is used when reading from the XML file only.
    // No other time.
    // TODO: why is addAccount different?
    // TODO: support non-currencies too.
    public void addCommodity(Commodity commodity) {
    	if (commodity instanceof Currency) {
    		currencies.put(((Currency)commodity).getCode(), commodity);
    	}
    }

    public void addAccount(Account account) {
        if (account instanceof CapitalAccount) {
            accounts.add(account);
        } else {
            categories.add(account);
        }
        
        // Fire the event.
        final AccountAddedEvent event = new AccountAddedEvent(this, account);
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountAdded(event);
        		}
       		});
    }
/* moved to MT940 code    
    public CapitalAccount getAccountByNumber(String accountNumber) {
        for (int i = 0; i < accounts.size(); i++) {
            CapitalAccount account = (CapitalAccount) accounts.get(i);
            if (account.getAccountNumber() != null
                && account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }
*/
/*
    public CapitalAccount getNewAccount(String name) {
        Currency defaultCurrency = Currency.getCurrencyForCode(
                userProperties.getDefaultCurrencyCode());
        CapitalAccount account = new CapitalAccountImpl(name, defaultCurrency);
        accounts.addElement(account);
        modified();
        return account;
    }
*/
    // TODO: Ensure no mutable interface on this object already.
    public MutableIncomeExpenseAccount createNewIncomeExpenseAccount() {
        return new MutableIncomeExpenseAccountImpl(this);
    }
        
    // TODO: Ensure no mutable interface on this object already.
    public MutableCapitalAccount createNewCapitalAccount() {
        return new MutableCapitalAccountImpl(this);
    }
     
    public MutableTransaxion createNewTransaxion() {
        return new MutableTransaxionImpl(this);
    }

    // TODO: Ensure no mutable interface on this object already.
    public MutableTransaxion createMutableTransaxion(Transaxion transaction) throws ObjectLockedForEditException {
        if (transaction instanceof MutableTransaxion) {
            throw new ObjectLockedForEditException();
        }
        return new MutableTransaxionImpl(this, transaction);
    }
    
    // Used by MutableTransaxionImpl.
    // Also this is the implementation of the addXxx pattern
    // required for list properties.
    public void addTransaxion(Transaxion transaction) {
        transactions.add(transaction);
       
        // For efficiency, we keep a list of entries in each
        // account/category.  We must update this list now.
        for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
            Entry entry = (Entry)iter.next();
            // TODO: at some time, keep these lists for categories too
            Account category = entry.getAccount();
            if (category instanceof CapitalAccount) {
                ((CapitalAccountImpl)category).addEntry(entry);
            }
        }
        modified();
    }
    
    public void addTransaction(Transaxion transaction) {
    	addTransaxion(transaction);
    }
    	
   	public void removeTransaxion(Transaxion transaction) {
        transactions.remove(transaction);

        // For efficiency, we keep a list of entries in each
        // account/category.  We must update this list now.
        for (Iterator iter = transaction.getEntryIterator(); iter.hasNext(); ) {
            Entry entry = (Entry)iter.next();
            // TODO: at some time, keep these lists for categories too
            Account category = entry.getAccount();
            if (category instanceof CapitalAccount) {
                ((CapitalAccountImpl)category).removeEntry(entry);
            }
        }
        modified();
    }
    
    // This is atomic, so does not need a mutable object.
    public void removeAccount(Account account) {
        Account parent = account.getParent();
        if (parent == null) {
            if (account instanceof IncomeExpenseAccount) {
                categories.remove(account);
            } else {
                accounts.remove(account);
            }
        } else {
            ((AbstractAccountImpl)parent).removeSubAccount(account);
        }
        
        modified();
        
        // Fire the event.
        final AccountDeletedEvent event = new AccountDeletedEvent(this, account);
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountDeleted(event);
        		}
       		});
    }

    boolean isModified() {
        return modified;
    }

    // 'A' to stop it being a property
    public void setModifiedA(boolean m) {
        if (modified == m)
            return;
        modified = m;
//        changeSupport.firePropertyChange("modified", !m, m);
    }

    /**
     * Other class implementations in this package may call this method
     * when classes further down inside this session are modified.
     */
    // TODO: Change to package only access when mutable stuff moved to impl.
    public void modified() {
        setModifiedA(true);
    }

    /**
     * Adds a PropertyChangeListener.
     */
//    public void addPropertyChangeListener(PropertyChangeListener pcl) {
//        changeSupport.addPropertyChangeListener(pcl);
//    }

    /**
     * Removes a PropertyChangeListener.
     */
//    public void removePropertyChangeListener(PropertyChangeListener pcl) {
//        changeSupport.removePropertyChangeListener(pcl);
//    }

    public void addSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.add(l);
    }
    
    public void removeSessionChangeListener(SessionChangeListener l) {
        sessionChangeListeners.remove(l);
    }
    
    public void addSessionChangeFirerListener(SessionChangeFirerListener l) {
        sessionChangeFirerListeners.add(l);
    }
    
    public void removeSessionChangeFirerListener(SessionChangeFirerListener l) {
        sessionChangeFirerListeners.remove(l);
    }
    
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        modified = false;
//        changeSupport = new PropertyChangeSupport(this);
    }

    public boolean canClose(IWorkbenchWindow window) {
        if (isModified()) {
            return SerializedDatastorePlugin.getDefault().requestSave(this, window);
        } else {
            return true;
        }
    }

    public void close() {
        // There is nothing to do here.  No files, connections or other resources
        // are kept open so there is nothing to close.
    }
    
    public String getBriefDescription() {
        if (sessionFile == null) {
            return null;
        } else {
            return sessionFile.getName();
        }
    }

    // The following methods are here only so that the object can be
    // serialized as a bean.
    
    public Vector getCommodities() {
        Vector result = new Vector();
    	for (Iterator iter = currencies.values().iterator(); iter.hasNext(); ) {
    		// All happen to be currencies
    		Currency commodity = (Currency)iter.next();
    		result.add(commodity);
    	}
    	return result;
    }
    
    public void setCommodities(Vector commodities) {
    	this.currencies = new Hashtable();
    	for (Iterator iter = commodities.iterator(); iter.hasNext(); ) {
    		// All happen to be currencies
    		Currency commodity = (Currency)iter.next();
    		this.currencies.put(commodity.getCode(), commodity);
    	}
    }

    public Vector getAccounts() {
        return accounts;
    }
    
    public void setAccounts(Vector accounts) {
        this.accounts = accounts;
    }

    public Vector getCategories() {
        return categories;
    }
    
    public void setCategories(Vector categories) {
        this.categories = categories;
    }

    public Vector getTransactions() {
        return transactions;
    }
    
    public void setTransactions(Vector transactions) {
        this.transactions = transactions;
    }

    /**
     * Passes an event on to all listeners who are listening for changes
     * to this session.
     * Called by other classes committing changes.
     */
/*    
    void fire(PropertyChangeEvent evt) {
        changeSupport.firePropertyChange(evt);
    }
*/
    /**
     *  In practice it is likely that the only listener will be the
     * JMoneyPlugin object.  Views should all listen to the JMoneyPlugin
     * class for changes to the model.  The JMoneyPlugin object will pass
     * on events from this session object.
     * <P>
     * Listeners may register directly with a session object.  However
     * if they do so then they must re-register whenever the session
     * object changes.  If a viewer wants to listen for changes to a
     * session even if that session is not the session currently shown
     * in the workbench then it should register with the session object,
     * but if the viewer wants to be told about changes to the current
     * workbench window then it should register with the JMoneyPlugin
     * object.
     */
    void fireEvent(ISessionChangeFirer firer) {
    	// Notify listeners who are listening to us using the
    	// SessionChangeListener interface.
        if (!sessionChangeFirerListeners.isEmpty()) {
        	// Take a copy of the listener list.  By doing this we
        	// allow listeners to safely add or remove listeners.
        	SessionChangeFirerListener listenerArray[] = new SessionChangeFirerListener[sessionChangeFirerListeners.size()];
        	sessionChangeFirerListeners.copyInto(listenerArray);
        	for (int i = 0; i < listenerArray.length; i++) {
        		listenerArray[i].sessionChanged(firer);
        	}
        }
    	
    	// Notify listeners who are listening to us using the
    	// SessionChangeListener interface.
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

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.ISessionManagement#getFactoryId()
	 */
	public String getFactoryId() {
		return "net.sf.jmoney.serializeddatastore.factoryid";
	}

	private IPersistableElement persistableElement 
	= new IPersistableElement() {
		public String getFactoryId() {
			return "net.sf.jmoney.serializeddatastore.factoryid";
		}
		public void saveState(IMemento memento) {
			// The session must have been saved by now, because
			// JMoney will not closed until the Session object says
			// it is ok to close, and the Session object will not
			// say it is ok to close unless it has available a file
			// name to which the session can be saved.  (It will ask
			// the user if the session was created using the New menu).
			memento.putString("fileName", sessionFile.getPath());
		}
	};
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IPersistableElement.class) {
			return persistableElement;
		}
		return null;
	}

/*
    public TransaxionImpl[] getTransaxions() {
        result = new TransaxionImpl[transa
        return accounts;
    }
    
    public void setAccounts(Vector accounts) {
        this.accounts = accounts;
    }
*/
}
