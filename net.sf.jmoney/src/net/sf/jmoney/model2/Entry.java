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

import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.EntryInfo;

/**
 * The data model for an entry.
 */
public final class Entry extends ExtendableObject {
	
	protected long creation = Calendar.getInstance().getTime().getTime();
	
	protected String check = null;
	
	protected Date valuta = null;
	
	protected String description = null;
	
	/**
	 * Element: Account
	 */
	protected IObjectKey accountKey = null;
	
	protected long amount = 0;
	
	protected String memo = null;
	
	/**
	 * Applicable only if the account is an IncomeExpenseAccount
	 * and the multi-currency property in the account is set.
	 * <P>
	 * Element: Currency
	 */
	protected IObjectKey incomeExpenseCurrencyKey = null; 
	
    /**
     * Constructor used by datastore plug-ins to create
     * an entry object.
     *
     * Note that the entry constructed by this constructor
     * may be invalid.  For example, it is possible that a
     * null account is set.  It is the callers responsibility
     * to ensure that an account is set before it relinquishes
     * control to other plug-ins.
     *  
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public Entry(
			IObjectKey objectKey,
			IObjectKey parentKey,
    		String     check,
    		String     description,
    		IObjectKey accountKey,
    		Date       valuta,
    		String     memo,
    		long       amount,
    		long       creation,
    		IObjectKey incomeExpenseCurrencyKey,
    		IValues extensionValues) {
		super(objectKey, parentKey, extensionValues);
		
		if (creation == 0) {
			this.creation = Calendar.getInstance().getTime().getTime();
		} else {
			this.creation = creation;
		}
		this.check = check;
		this.valuta = valuta;
		this.description = description;
		this.accountKey = accountKey;
		this.amount = amount;
		this.memo = memo;
		this.incomeExpenseCurrencyKey = incomeExpenseCurrencyKey;
	}
	
    /**
     * Constructor used by datastore plug-ins to create
     * an entry object.
     *
     * Note that the entry constructed by this constructor
     * may be invalid.  For example, it is possible that a
     * null account is set.  It is the callers responsibility
     * to ensure that an account is set before it relinquishes
     * control to other plug-ins.
     *  
     * @param parent The key to a Transaction object.
     * 		This parameter must be non-null.
     * 		The getObject method must not be called on this
     * 		key from within this constructor because the
     * 		key may not yet be in a state in which it is
     * 		capable of materializing an object.   
     */
	public Entry(
			IObjectKey objectKey,
			IObjectKey parentKey) {
		super(objectKey, parentKey);
		
		this.creation = Calendar.getInstance().getTime().getTime();
		this.check = null;
		this.valuta = null;
		this.description = null;
		this.accountKey = null;
		this.amount = 0;
		this.memo = null;
		this.incomeExpenseCurrencyKey = null;
	}

	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.entry";
	}
	
	/**
	 * Returns the transaction.
	 */
	public Transaction getTransaction() {
		return (Transaction)parentKey.getObject();
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
		if (accountKey == null) {
			return null;
		} else {
			return (Account)accountKey.getObject();
		}
	}
	
	/**
	 * Returns the currency in which the amount in this entry is denominated.
	 * This property is applicable if and only if the account for this entry
	 * is an IncomeExpenseAccount and the multi-currency property in the account
	 * is set.
	 */
	public Currency getIncomeExpenseCurrency() {
		if (incomeExpenseCurrencyKey == null) {
			return null;
		} else {
			return (Currency)incomeExpenseCurrencyKey.getObject();
		}
	}

	/**
	 * returns the other account of the transaction associated with this
	 * entry. If the transaction is a splitted one, there are several "other 
	 * accounts", and the returned value is "null".
	 */
	public Account getOtherAccount () {
		if (getTransaction().hasTwoEntries()) {
			return getTransaction().getOther(this).getAccount();
		} else {
			return null;
		}                    
	}

	public String getFullAccountName() {
		if (getTransaction().hasTwoEntries()) {
			Account category = getTransaction().getOther(this).getAccount();
			if (category == null) {
				return null;
			} else {
				return category.getFullAccountName();
			}
		} else if (getTransaction().hasMoreThanTwoEntries()) {
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
	 * @return The commodity for this entry, or null if not enough
	 * 			information has been set to determine the commodity.
	 */
	public Commodity getCommodity() {
		if (getAccount() == null) {
			return null;
		} else {
			return getAccount().getCommodity(this);
		}
	}
	
	/**
	 * Sets the creation.
	 */
	public void setCreation(long aCreation) {
		long oldCreation = this.creation;
		creation = aCreation;
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getCreationAccessor(), new Long(oldCreation), new Long(creation));
	}
	
	/**
	 * Sets the check.
	 */
	public void setCheck(String aCheck) {
		String oldCheck = this.check;
		check = (aCheck != null && aCheck.length() == 0) ? null : aCheck;
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getCheckAccessor(), oldCheck, check);
	}
	
	/**
	 * Sets the valuta.
	 */
	public void setValuta(Date aValuta) {
		Date oldValuta = this.valuta;
		valuta = aValuta;
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getValutaAccessor(), oldValuta, valuta);
	}
	
	/**
	 * Sets the description.
	 */
	public void setDescription(String aDescription) {
		String oldDescription = this.description;
		description = (aDescription != null && aDescription.length() == 0) ? null : aDescription;
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getDescriptionAccessor(), oldDescription, description);
	}
	
	/**
	 * Sets the account.
	 */
	public void setAccount(Account newAccount) {
		Account oldAccount =
			accountKey == null
			? null
					: (Account)accountKey.getObject();
		
		// TODO: This is not efficient.  Better would be to pass
		// an object key as the old value to the property change
		// method.  Then the object is materialized only if
		// necessary.
		// NOTE: Even though a null account is not valid, we support
		// the setting of it because code may potentially need to do this
		// in order to, say, delete the account before the new account
		// of the entry is known.
		accountKey = 
			newAccount == null
			? null
					: newAccount.getObjectKey();
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getAccountAccessor(), oldAccount, newAccount);
	}
	
	/**
	 * Sets the amount.
	 */
	public void setAmount(long anAmount) {
		long oldAmount = this.amount;
		amount = anAmount;
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getAmountAccessor(), oldAmount, amount);
	}
	
	/**
	 * Sets the memo.
	 */
	public void setMemo(String aMemo) {
		String oldMemo = this.memo;
		this.memo = (aMemo != null && aMemo.length() == 0) ? null : aMemo;
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getMemoAccessor(), oldMemo, memo);
	}
	
	/**
	 * Sets the currency in which the amount in this entry is denominated.
	 * This property is applicable if and only if the account for this entry
	 * is an IncomeExpenseAccount and the multi-currency property in the account
	 * is set.
	 */
	public void setIncomeExpenseCurrency(Currency incomeExpenseCurrency) {
		Currency oldIncomeExpenseCurrency =
			incomeExpenseCurrencyKey == null
			? null
					: (Currency)incomeExpenseCurrencyKey.getObject();
		
		// TODO: This is not efficient.  Better would be to pass
		// an object key as the old value to the property change
		// method.  Then the object is materialized only if
		// necessary.
		incomeExpenseCurrencyKey =
			incomeExpenseCurrency == null
			? null
					: incomeExpenseCurrency.getObjectKey();
		
		
		// Notify the change manager.
		processPropertyChange(EntryInfo.getIncomeExpenseCurrencyAccessor(), oldIncomeExpenseCurrency, incomeExpenseCurrency);
	}
}
