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

import java.util.Map;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.BankAccountInfo;

/**
 * The data model for an bank account.
 */
public class BankAccount extends CurrencyAccount {

	protected String bank = null;

	protected String accountNumber = null;

	protected Long minBalance = null;

	/**
	 * The full constructor for a BankAccount object.  This constructor is called
	 * only be the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public BankAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			String name,
			IListManager<CapitalAccount> subAccounts,
			String abbreviation,
			String comment,
			IObjectKey currencyKey,
			long startBalance,
			String bank,
			String accountNumber,
			Long minBalance) {
		super(objectKey, extensions, parent, name, subAccounts, abbreviation, comment, currencyKey, startBalance);
		
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.minBalance = minBalance;
	}

	/**
	 * The default constructor for a BankAccount object.  This constructor is called
	 * when a new BankAccount object is created.  The properties are set to default
	 * values.  The list properties are set to empty lists.  The parameter list for this
	 * constructor is the same as the full constructor except that there are no parameters
	 * for the scalar properties.
	 */
	public BankAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			IListManager<CapitalAccount> subAccounts) {
		super(objectKey, extensions, parent, subAccounts);
		
		// Overwrite the default name with our own default name.
		this.name = JMoneyPlugin.getResourceString("Account.newAccount");
		
        this.bank = null;
        this.accountNumber = null;
        this.minBalance = null;
	}

	// TODO: remove this.  If we could get the property set, typed
	// with the correct type as the generic parameter, then that would
	// be great.  Otherwise this method is no use because we can get
	// the property set from the map.
	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.bankAccount";
	}
	
	/**
	 * @return the bank name of this account.
	 */
	public String getBank() {
		return bank;
	};

	/**
	 * @return the account number of this account.
	 */
	public String getAccountNumber() {
		return accountNumber;
	};

	/**
	 * @return the minimal balance of this account.
	 */
	public Long getMinBalance() {
		return minBalance;
	};

	/**
	 * @param aBank the name of this account.
	 */
	public void setBank(String aBank) {
        String oldBank = this.bank;
		this.bank = aBank;

		// Notify the change manager.
		processPropertyChange(BankAccountInfo.getBankAccessor(), oldBank, aBank);
	}

	/**
	 * Sets the account number of this account.
	 * @param anAccountNumber the account number
	 */
	public void setAccountNumber(String anAccountNumber) {
        String oldAccountNumber = this.accountNumber;
        this.accountNumber = anAccountNumber;

		// Notify the change manager.
		processPropertyChange(BankAccountInfo.getAccountNumberAccessor(), oldAccountNumber, anAccountNumber);
	}

	/**
	 * @param m the minimal balance which may be null.
	 */
	public void setMinBalance(Long m) {
        Long oldMinBalance = this.minBalance;
		this.minBalance = m;

		// Notify the change manager.
		processPropertyChange(BankAccountInfo.getMinBalanceAccessor(), oldMinBalance, m);
	}
}
