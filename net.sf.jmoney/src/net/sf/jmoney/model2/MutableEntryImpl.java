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
public class MutableEntryImpl extends ExtendableObjectHelperImpl implements Entry {
	
    protected EntryImpl originalEntry = null;
	
	protected Transaction transaction = null;
	
	protected long creation = Calendar.getInstance().getTime().getTime();
	
	protected String check = null;
	
	protected Date valuta = null;
	
	protected String description = null;
	
	protected Account account = null;
	
	protected long amount = 0;
	
	protected String memo = null;
	
	/**
	 * Creates a new entry in a transaction.
	 * This constructor should be called from MutableTransaction.createEntry() only.
	 */

	MutableEntryImpl(MutableTransactionImpl transaction) {
		super(null, null);
		this.transaction = transaction;
	}
	
	/**
	 * Creates a mutable entry that can be used to modify an existing entry.
	 * This constructor should be called from MutableTransaction constructor only.
	 */
	MutableEntryImpl(MutableTransactionImpl transaction, EntryImpl originalEntry) {
		super(null, null);  // TODO: I don't think this is correct.

		this.transaction = transaction;
		this.originalEntry = originalEntry;
		
		creation = originalEntry.getCreation();
		check = originalEntry.getCheck();
		valuta = originalEntry.getValuta();
		description = originalEntry.getDescription();
		account = (AbstractAccountImpl)originalEntry.getAccount();
		amount = originalEntry.getAmount();
		memo = originalEntry.getMemo();
	}

	public boolean isMutable() {
		return true;
	}

	protected IExtendableObject getOriginalObject() {
		return originalEntry;
	}
	
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.entry";
	}
	
	/*
	 * Used by the above to get extension info from mutable entry.
	 */
	public Map getExtensionsAsIs() {
		return extensions;
	}
	
	/**
	 * Returns the creation.
	 */
	public Transaction getTransaxion() {
		return transaction;
	}
	
	/**
	 * If this entry is a mutable entry then return the original
	 * entry which is being edited, or, if this mutable entry is
	 * a new entry that has never been committed to the datastore
	 * then return null.
	 *
	 * @exception RuntimeException This entry is not mutable.
	 */
	public EntryImpl getOriginalEntry() {
		return originalEntry;
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
//		firePropertyChange("check", oldCheck, check);
	}
	
	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date aValuta) {
		Date oldValuta = this.valuta;
		valuta = aValuta;
//		firePropertyChange("valuta", oldValuta, valuta);
	}
	
	/**
	 * Sets the description.
	 */
	public void setDescription(String aDescription) {
		String oldDescription = this.description;
		description = (aDescription != null && aDescription.length() == 0) ? null : aDescription;
//		firePropertyChange("description", oldDescription, description);
	}
	
	/**
	 * Sets the account.
	 */
	public void setAccount(Account newAccount) {
		Account oldAccount = this.account;
		this.account = newAccount;
//		firePropertyChange("account", oldAccount, newAccount);
	}
	
	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		long oldAmount = this.amount;
		amount = anAmount;
//		firePropertyChange("amount", new Double(oldAmount), new Double(amount));
	}
	
	/**
	 * Sets the memo.
	 */
	public void setMemo(String aMemo) {
		String oldMemo = this.memo;
		this.memo = (aMemo != null && aMemo.length() == 0) ? null : aMemo;
//		firePropertyChange("memo", oldMemo, memo);
	}
	
	// Methods for firing property changes
	
	/**
	 * Fires changes to properties in the extendable object.
	 * (This method is not called for changes to properties
	 * in extension property sets).
	 */
/*	
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
*/	
}
