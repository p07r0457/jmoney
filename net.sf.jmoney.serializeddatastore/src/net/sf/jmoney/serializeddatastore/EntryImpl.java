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

import net.sf.jmoney.model2.*;
import net.sf.jmoney.JMoneyPlugin;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;

/**
 * The data model for an entry.
 */
public class EntryImpl extends ExtendableObjectHelperImpl implements Entry, Serializable {
	
    protected Entry originalEntry = null;
	
	protected Transaxion transaction = null;
	
	protected long creation = Calendar.getInstance().getTime().getTime();
	
	protected String check = null;
	
	protected Date valuta = null;
	
	protected String description = null;
	
	protected Account account = null;
	
	protected long amount = 0;
	
	protected String memo = null;
	
	/**
	 * Default constructor used for de-serialization
	 */
	public EntryImpl() {
	}
	
	/**
	 * Creates a new entry in a transaction.
	 * This constructor should be called from MutableTransaxion.createEntry() only.
	 */
	EntryImpl(Transaxion transaction) {
		this.transaction = transaction;
	}
	
	/**
	 * Creates a mutable entry that can be used to modify an existing entry.
	 * This constructor should be called from MutableTransaxion constructor only.
	 */
	EntryImpl(MutableTransaxionImpl transaction, EntryImpl originalEntry) {
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
		return getTransaxion() instanceof MutableTransaxion;
	}

	protected IExtendableObject getOriginalObject() {
		return originalEntry;
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
	void copyProperties(EntryImpl sourceEntry) {
		creation = sourceEntry.getCreation();
		check = sourceEntry.getCheck();
		valuta = sourceEntry.getValuta();
		description = sourceEntry.getDescription();
		account = (AbstractAccountImpl)sourceEntry.getAccount();
		amount = sourceEntry.getAmount();
		memo = sourceEntry.getMemo();
		
		copyExtensions(sourceEntry.getExtensionsAsIs());
	}
	
	/*
	 * Used by the above to get extension info from mutable entry.
	 */
	Map getExtensionsAsIs() {
		return extensions;
	}
	
	/**
	 * Returns the creation.
	 */
	public Transaxion getTransaxion() {
		return transaction;
	}
	
	public Entry getOriginalEntry() {
		return originalEntry;
	}
	
	/**
	 * Called only when a new entry is being committed to the database.
	 * This allows consumers to determine the committed entry for
	 * the given mutable entry.
	 */
	void setOriginalEntry(EntryImpl originalEntry) {
		if (!(transaction instanceof MutableTransaxion)) { 
			throw new RuntimeException("internal error");
		}
		if (this.originalEntry != null) { 
			throw new RuntimeException("internal error");
		}
		this.originalEntry = originalEntry;
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
		if (transaction.hasTwoEntries()) {
			Account category = transaction.getOther(this).getAccount();
			if (category == null) {
				return null;
			} else {
				return category.getFullAccountName();
			}
		} else if (transaction.hasMoreThanTwoEntries()) {
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
	
	
	
	private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
	
	/**
	 * This method is used when setting the references that are not
	 * serialized to file during serialization.
	 */
	void setTransaxion(Transaxion transaction) {
		this.transaction = transaction;
	}
	
	// These methods are used by the XMLEncoder to serialize the extensions.
	// One of the characteristics of the data model is that all data is maintained
	// even if the data was added by a plug-in (so only the plug-in knows how to
	// interpret the data) and the plug-in is not installed.  In this case the data
	// must be maintained, though it cannot be edited.
	
	// This is done by serializing the id of the property set and a serialization of
	// the extension.  If, on de-serialization, the plug-in is not found then
	// the data is added to the map by adding a string containing the data.
	// If the plug-in is later installed then the appropriate extension object
	// is built from the string.
	
	public SerializableExtension[] getExtensions() {
		SerializableExtension result[] = new SerializableExtension[extensions.size()];
		int i = 0;
		
		for (Iterator iter = extensions.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry mapEntry = (Map.Entry)iter.next();
			
			PropertySet propertySetKey = (PropertySet)mapEntry.getKey();
			String propertySetId = propertySetKey.getId();
			
			IExtendableObject extension = (IExtendableObject)mapEntry.getValue();
			
			// All extensions are beans and thus must be fully re-constructable through
			// the properties that have both setters and getters.
			result[i] = new SerializableExtension();
			result[i].setPluginId(propertySetId);
			result[i].setData(extensionToString(extension));
			
			i++;
		}
		
		return result;
	}
	
	public void setExtensions(SerializableExtension[] input) {
		extensions.clear();  // not sure if really necessary
		for (int i = 0; i < input.length; i++) {
			importExtensionString(input[i].getPluginId(), input[i].getData());
		}
	}

	protected void postLoad() {
	}
	
}
