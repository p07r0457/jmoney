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

package net.sf.jmoney.model2;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;

import net.sf.jmoney.isocurrencies.IsoCurrenciesPlugin;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Holds the fields that will be saved in a file.
 */
public class SessionImpl extends ExtendableObjectHelperImpl implements Session, Serializable {

    protected Currency defaultCurrency;
    
    protected IListManager commodities;
    
    protected IListManager accounts;

    protected IListManager transactions;

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
     * Constructor used by datastore plug-ins to create
     * a session object.
     */
    public SessionImpl(
    		IObjectKey objectKey,
    		Map extensions,
    		IListManager commodities,
			IListManager accounts,
			IListManager transactions,
			IObjectKey defaultCurrency) {
    	super(objectKey, extensions);

    	this.commodities = commodities;
    	this.accounts = accounts;
    	this.transactions = transactions;
    	
    	// If the list of commodities is empty then load
    	// the full list of ISO currencies.
    	if (commodities.isEmpty()) {
    		initSystemCurrencies();
    	}

        // Set up a hash table that maps currency codes to
        // the currency object.
    	this.currencies = new Hashtable();
    	for (Iterator iter = commodities.iterator(); iter.hasNext(); ) {
    		Commodity commodity = (Commodity)iter.next();
    		if (commodity instanceof Currency) {
    			Currency currency = (Currency)commodity;
    			this.currencies.put(currency.getCode(), currency);
    		}
    	}
    	
        // If the default currency is null then we look for
        // a currency that has a code of "USD".  If no such
        // currency exists then no default currency is set.
        if (defaultCurrency != null) {
        	this.defaultCurrency = (Currency)defaultCurrency.getObject();
        } else {
        	Currency dollarCurrency = getCurrencyForCode("USD");
        	if (dollarCurrency != null) {
        		this.defaultCurrency = dollarCurrency;
        	} else {
        		this.defaultCurrency = null;
        	}
        }
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
		return (Currency) currencies.get(code);
	}

	private void initSystemCurrencies() {
		InputStream in = IsoCurrenciesPlugin.class.getResourceAsStream("Currencies.txt");
		BufferedReader buffer = new BufferedReader(new InputStreamReader(in));
		try {
			String line = buffer.readLine();
			while (line != null) {
				final String code = line.substring(0, 3);
				byte d;
				try {
					d = Byte.parseByte(line.substring(4, 5));
				} catch (Exception ex) {
					d = 2;
				}
				final byte decimals = d;
				final String name = NAME.getString(code);
				
				PropertySet currencyPropertySet;
				try {
					currencyPropertySet = PropertySet.getPropertySet("net.sf.jmoney.currency");
				} catch (PropertySetNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("internal error");
				}
				// TODO: Sort out what should really be in the Currency interface and remove
				// most of the methods in the following implementation.
				Currency newCurrency = (Currency)commodities.createNewElement(
						currencyPropertySet, 
						new Currency() {

							public String getCode() {
								return code;
							}

							public void setCode(String code) {
								// TODO Auto-generated method stub
								
							}

							public int getDecimals() {
								return decimals;
							}

							public void setDecimals(int decimals) {
								// TODO Auto-generated method stub
								
							}

							public String getName() {
								return name;
							}

							public void setName(String name) {
								// TODO Auto-generated method stub
								
							}

							public long parse(String amountString) {
								// TODO Auto-generated method stub
								return 0;
							}

							public String format(long amount) {
								// TODO Auto-generated method stub
								return null;
							}

							public short getScaleFactor() {
								// TODO Auto-generated method stub
								return 0;
							}

							public ExtensionObject getExtension(PropertySet propertySetKey) {
								// No extension properties are to be set.
								return null;
							}

							public Object getPropertyValue(PropertyAccessor propertyAccessor) {
								// TODO Auto-generated method stub
								return null;
							}

							public int getIntegerPropertyValue(PropertyAccessor propertyAccessor) {
								// TODO Auto-generated method stub
								return 0;
							}

							public long getLongPropertyValue(PropertyAccessor propertyAccessor) {
								// TODO Auto-generated method stub
								return 0;
							}

							public String getStringPropertyValue(PropertyAccessor propertyAccessor) {
								// TODO Auto-generated method stub
								return null;
							}

							public Iterator getPropertyIterator(PropertyAccessor propertyAccessor) {
								// TODO Auto-generated method stub
								return null;
							}

							public String getPropertyValueAsString(PropertyAccessor propertyAccessor) {
								// TODO Auto-generated method stub
								return null;
							}

							public IObjectKey getObjectKey() {
								// TODO Auto-generated method stub
								return null;
							}
							
						});

				line = buffer.readLine();
			}
		} catch (IOException ioex) {
		}
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
/*   
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
  */ 
    public Iterator getAccountIterator() {
        return accounts.iterator();
    }
   
    public Iterator getCapitalAccountIterator() {
        return new Iterator() {
        	Iterator iter = accounts.iterator();
        	Object element;
        	
			public boolean hasNext() {
				while (iter.hasNext()) {
					element = iter.next();
					if (element instanceof CapitalAccount) {
						return true; 
					}
				}
				return false;
			}
			public Object next() {
				return element;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }
   
    public Iterator getIncomeExpenseAccountIterator() {
        return new Iterator() {
        	Iterator iter = accounts.iterator();
        	Object element;
        	
			public boolean hasNext() {
				while (iter.hasNext()) {
					element = iter.next();
					if (element instanceof IncomeExpenseAccount) {
						return true; 
					}
				}
				return false;
			}
			public Object next() {
				return element;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
        };
    }
   
    public Iterator getTransactionIterator() {
        return transactions.iterator();
    }
/*   
    // This is used when reading from the XML file only.
    // No other time.
    // TODO: why is addAccount different?
    // TODO: support non-currencies too.
    public void addCommodity(Commodity commodity) {
    	if (commodity instanceof Currency) {
    		currencies.put(((Currency)commodity).getCode(), commodity);
    	}
    }
*/
/*    
    // Used by MutableCapitalAccountImpl and
    // MutableIncomeExpenseAccountImpl.
    // TODO: Remove this method and use instead the
    // new method that gets the datastore to create
    // new objects.
    public void addAccount(Account account) {
        accounts.add(account);
              
        // Fire the event.
        final AccountAddedEvent event = new AccountAddedEvent(this, account);
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountAdded(event);
        		}
       		});
    }
*/
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
     
    public MutableTransaction createNewTransaction() {
        return new MutableTransactionImpl(this);
    }

    // TODO: Ensure no mutable interface on this object already.
    public MutableTransaction createMutableTransaction(Transaction transaction) throws ObjectLockedForEditException {
        if (transaction instanceof MutableTransaction) {
            throw new ObjectLockedForEditException();
        }
        return new MutableTransactionImpl(this, transaction);
    }
    
    // Used by MutableTransactionImpl.
    // Also this is the implementation of the addXxx pattern
    // required for list properties.
/*    
    public void addTransaxion(Transaction transaction) {
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
    
    public void addTransaction(Transaction transaction) {
    	addTransaxion(transaction);
    }
*/    	
   	public void removeTransaction(Transaction transaction) {
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
/*
        	if (account instanceof IncomeExpenseAccount) {
                categories.remove(account);
            } else {
                accounts.remove(account);
            }
*/            
            accounts.remove(account);
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
    
    // The following methods are here only so that the object can be
    // serialized as a bean.
/*    
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
*/
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
    // TODO: Change to package protection when interface is removed.
    public void fireEvent(ISessionChangeFirer firer) {
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

	/**
	 * Create a new account.  Accounts are abstract, so
	 * a property set derived from the account property
	 * set must be passed to this method.  When an account
	 * is created, various objects, such as the objects that manage collections within
	 * the account, must be created.  The implementation of
	 * these objects depends on the datastore and must be passed
	 * to the constructor, so the actual construction of the object
	 * is delegated to the collection object that will hold this
	 * new account.  (The implementation of the collection object
	 * is provided by the datastore plug-in, so this object knows
	 * how to create an object in a way that is appropriate for
	 * the datastore).
	 * 
	 * The collection object will get the properties for the new
	 * object from the given interface.  Scalar properties are
	 * simply set.  References to other objects are likewise set.
	 * This means any referenced object must have been fetched by
	 * the datastore.
	 * 
	 * @param accountPropertySet
	 * @param account
	 * @return
	 */
	public Account createNewAccount(PropertySet propertySet, Account account) {
		AbstractAccountImpl newAccount = (AbstractAccountImpl)accounts.createNewElement(propertySet, account);

		newAccount.setParent(null);  // null indicates a top-level account

		// Fire the event.
        final AccountAddedEvent event = new AccountAddedEvent(this, newAccount);
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountAdded(event);
        		}
       		});
        
        return newAccount;
	}

	/**
	 * Create a new commodity.  Commodities are abstract, so
	 * a property set derived from the commodity property
	 * set must be passed to this method.  When an commodity
	 * is created, various objects, such as the objects that manage collections within
	 * the commodity, must be created.  The implementation of
	 * these objects depends on the datastore and must be passed
	 * to the constructor, so the actual construction of the object
	 * is delegated to the collection object that will hold this
	 * new commodity.  (The implementation of the collection object
	 * is provided by the datastore plug-in, so this object knows
	 * how to create an object in a way that is appropriate for
	 * the datastore).
	 * 
	 * The collection object will get the properties for the new
	 * object from the given interface.  Scalar properties are
	 * simply set.  References to other objects are likewise set.
	 * This means any referenced object must have been fetched by
	 * the datastore.
	 * 
	 * @param commodityPropertySet
	 * @param commodity
	 * @return
	 */
	public Commodity createNewCommodity(PropertySet propertySet, Commodity commodity) {
		Commodity newCommodity = (Commodity)commodities.createNewElement(propertySet, commodity);

		if (newCommodity instanceof Currency) {
			Currency newCurrency = (Currency)newCommodity;
			currencies.put(newCurrency.getCode(), newCurrency);
		}
/* TODO: implement this		
		// Fire the event.
        final CommodityAddedEvent event = new CommodityAddedEvent(this, newCommodity);
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.commodityAdded(event);
        		}
       		});
*/        
        return newCommodity;
	}

	public Transaction createNewTransaction(PropertySet propertySet, Transaction transaction) {
		TransactionImpl newTransaction = (TransactionImpl)transactions.createNewElement(propertySet, transaction);

		// Fire the event.
/* Do we need notification of new transactions, or are the notifications
 * for addition and removal of each entry good enough?
 		
        final AccountAddedEvent event = new AccountAddedEvent(this, newAccount);
        fireEvent(
        	new ISessionChangeFirer() {
        		public void fire(SessionChangeListener listener) {
        			listener.accountAdded(event);
        		}
       		});
*/        
        return newTransaction;
	}

/*
    public TransactionImpl[] getTransaxions() {
        result = new TransactionImpl[transa
        return accounts;
    }
    
    public void setAccounts(Vector accounts) {
        this.accounts = accounts;
    }
*/
}
