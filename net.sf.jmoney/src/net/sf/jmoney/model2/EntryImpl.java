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

import net.sf.jmoney.JMoneyPlugin;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * The data model for an entry.
 */
public class EntryImpl extends ExtendableObjectHelperImpl implements Entry {
	
    protected MutableEntryImpl originalEntry = null;
	
	protected IObjectKey transactionKey = null;
	
	protected long creation = Calendar.getInstance().getTime().getTime();
	
	protected String check = null;
	
	protected Date valuta = null;
	
	protected String description = null;
	
	protected Account account = null;
	
	protected long amount = 0;
	
	protected String memo = null;
	
    /**
     * Constructor used by datastore plug-ins to create
     * an entry object.
     * 
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public EntryImpl(
				IObjectKey objectKey,
	    		Map        extensions,
				IObjectKey parent,
	    		String     check,
	    		String     description,
	    		IObjectKey accountKey,
	    		Date       valuta,
	    		String     memo,
	    		long       amount,
	    		long       creation) {
		super(objectKey, extensions);

		/*

		System.out.println("Creating an Entry with" 
		        + "\nobjectKey: " + objectKey
		        + "\nparent:" + parent
		        + "\naccountKey:" + accountKey);
		*/
		
		this.creation = creation;
		this.check = check;
		this.valuta = valuta;
		this.description = description;
		this.account = accountKey.getObject() instanceof Account 
			? (Account)accountKey.getObject()
			: null;
		this.amount = amount;
		this.memo = memo;
		
        this.transactionKey = parent;
	}
	
	// Called only by datastore after object originally constructed.
	public void registerWithIndexes() {
		if (account instanceof CapitalAccountImpl) {
			CapitalAccountImpl capitalAccount = (CapitalAccountImpl) account;
			capitalAccount.addEntry(this);
		}
	}
	
	public boolean isMutable() {
		return false;
	}

	// TODO: see how we can remove this method from here,
	// as it is only required for mutable objects.
	protected IExtendableObject getOriginalObject() {
		return null;
	}

	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.entry";
	}
	
	/**
	 * Sets the property values in an entry to the values taken
	 * from a mutable entry.
	 *
	 * This method is used when a new entry or changes to an existing
	 * entry are to be committed to the database.
	 */
	void copyProperties(Entry sourceEntry) {
		creation = sourceEntry.getCreation();
		check = sourceEntry.getCheck();
		valuta = sourceEntry.getValuta();
		description = sourceEntry.getDescription();
		account = sourceEntry.getAccount();
		amount = sourceEntry.getAmount();
		memo = sourceEntry.getMemo();
		
		copyExtensions(sourceEntry.getExtensionsAsIs());
	}
	
	/*
	 * Used by the above to get extension info from mutable entry.
	 */
	public Map getExtensionsAsIs() {
		// Entry interface, but called only from mutable
		// entries.
		// TODO: can we tidy this up???
		throw new RuntimeException("internal error");
	}
	
	/**
	 * Returns the transaction.
	 */
	public Transaction getTransaxion() {
		return (Transaction)transactionKey.getObject();
	}

	/**
	 * Returns the creation.
	 */
	public long getCreation() {
		return creation;
	}
	
	/**
	 * Returns the check.
	 */
	public String getCheck() {
		return check;
	}
	
	/**
	 * Returns the valuta.
	 */
	public Date getValuta() {
		return valuta;
	}
	
	/**
	 * Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Returns the account.
	 */
	public Account getAccount() {
		return account;
	}
	
	public String getFullAccountName() {
		if (getTransaxion().hasTwoEntries()) {
			Account category = getTransaxion().getOther(this).getAccount();
			if (category == null) {
				return null;
			} else {
				return category.getFullAccountName();
			}
		} else if (getTransaxion().hasMoreThanTwoEntries()) {
			// TODO: get rid of this message from here,
			// and move text from jmoney to jmoney.accountentriespanel
			return JMoneyPlugin.getResourceString("SplitCategory.name");
		} else {
			return null;
		}                    
	}
	
	/**
	 * Returns the amount.
	 */
	public long getAmount() {
		return amount;
	}
	
	/**
	 * Returns the memo.
	 */
	public String getMemo() {
		return memo;
	}
	
	/**
	 * Sets the creation.
	 */
	public void setCreation(long aCreation) {
		creation = aCreation;
	}
	
	/**
	 * Sets the check.
	 */
	public void setCheck(String aCheck) {
		String oldCheck = this.check;
		check = (aCheck != null && aCheck.length() == 0) ? null : aCheck;
		firePropertyChange("check", oldCheck, check);
	}
	
	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date aValuta) {
		Date oldValuta = this.valuta;
		valuta = aValuta;
		firePropertyChange("valuta", oldValuta, valuta);
	}
	
	/**
	 * Sets the description.
	 */
	public void setDescription(String aDescription) {
		String oldDescription = this.description;
		description = (aDescription != null && aDescription.length() == 0) ? null : aDescription;
		firePropertyChange("description", oldDescription, description);
	}
	
	/**
	 * Sets the account.
	 */
	public void setAccount(Account newAccount) {
		Account oldAccount = this.account;
		this.account = newAccount;
		firePropertyChange("account", oldAccount, newAccount);
	}
	
	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		long oldAmount = this.amount;
		amount = anAmount;
		firePropertyChange("amount", new Double(oldAmount), new Double(amount));
	}
	
	/**
	 * Sets the memo.
	 */
	public void setMemo(String aMemo) {
		String oldMemo = this.memo;
		this.memo = (aMemo != null && aMemo.length() == 0) ? null : aMemo;
		firePropertyChange("memo", oldMemo, memo);
	}
	
	// Methods for firing property changes
	
	/**
	 * Fires changes to properties in the extendable object.
	 * (This method is not called for changes to properties
	 * in extension property sets).
	 */
	protected void firePropertyChange(String propertyLocalName, Object oldValue, Object newValue) {
		if (newValue != null && !newValue.equals(oldValue)
				|| newValue == null && oldValue != null) {
			// TODO tidy this up.
			// Should remove getExtendablePropertySet method
			// altogether and should not get by name but this
			// should be set directly and stored in a static
			// in the class.
			try {
				PropertySet.getPropertySet("net.sf.jmoney.entry").getProperty(propertyLocalName).firePropertyChange(
					this, oldValue, newValue);
			} catch (PropertySetNotFoundException e) {
				throw new RuntimeException("internal error");
			} catch (PropertyNotFoundException e) {
				throw new RuntimeException("internal error");
			}
		}
	}
	
	protected void firePropertyChange(String propertyLocalName, int oldValue, int newValue) {
		firePropertyChange(propertyLocalName, new Integer(oldValue), new Integer(newValue));
	}
	
	protected void firePropertyChange(String propertyLocalName, char oldValue, char newValue) {
		firePropertyChange(propertyLocalName, new Character(oldValue), new Character(newValue));
	}
}
