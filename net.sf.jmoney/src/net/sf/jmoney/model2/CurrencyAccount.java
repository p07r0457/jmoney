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
import net.sf.jmoney.fields.CurrencyAccountInfo;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;

/**
 * The data model for an account.
 */
public class CurrencyAccount extends CapitalAccount {

	/**
	 * The entries are ordered by their creation.
	 */
	public static final int CREATION_ORDER = 0;

	/**
	 * The entries are ordered by their date field.
	 */
	public static final int DATE_ORDER = 1;

	/**
	 * The entries are ordered by their check field.
	 */
	public static final int CHECK_ORDER = 2;

	/**
	 * The entries are ordered by their valuta field.
	 */
	public static final int VALUTA_ORDER = 3;

	protected static String[] entryOrderNames;

	protected Currency currency;

	protected long startBalance = 0;

	protected PropertyChangeSupport changeSupport =
		new PropertyChangeSupport(this);

	/**
	 * The full constructor for a CurrencyAccount object.  This constructor is called
	 * only be the datastore when loading data from the datastore.  The properties
	 * passed to this constructor must be valid because datastores should only pass back
	 * values that were previously saved from a CapitalAccount object.  So, for example,
	 * we can be sure that a non-null name and currency are passed to this constructor.
	 * 
	 * @param name the name of the account
	 */
	public CurrencyAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			String name,
			IListManager subAccounts,
			String abbreviation,
			String comment,
			IObjectKey currencyKey,
			long startBalance) {
		super(objectKey, extensions, parent, name, subAccounts, abbreviation, comment);
		
		// This account is being loaded from the datastore and therefore a currency
		// must be set.  We store internally the Currency object itself, not the 
		// key used to fetch the Currency object.
		if (currencyKey == null) {
			this.currency = objectKey.getSession().getDefaultCurrency();
		} else {
			this.currency = (Currency)currencyKey.getObject();
		}
		
        this.startBalance = startBalance;
	}

	/**
	 * The default constructor for a CapitalAccount object.  This constructor is called
	 * when a new CapitalAccount object is created.  The properties are set to default
	 * values.  The list properties are set to empty lists.  The parameter list for this
	 * constructor is the same as the full constructor except that there are no parameters
	 * for the scalar properties.
	 */
	public CurrencyAccount(
			IObjectKey objectKey, 
			Map extensions, 
			IObjectKey parent,
			IListManager subAccounts) {
		super(objectKey, extensions, parent, subAccounts);
		
		// Overwrite the default name with our own default name.
		this.name = JMoneyPlugin.getResourceString("Account.newAccount");
		
		// Set the currency to the session default currency.
		this.currency = objectKey.getSession().getDefaultCurrency();
		
        this.startBalance = 0;
	}

	protected String getExtendablePropertySetId() {
		return "net.sf.jmoney.currencyAccount";
	}
	
	/**
	 * @return the locale of this account.
	 */
	public String getCurrencyCode() {
		return currency.getCode();
	}

	public Currency getCurrency() {
            return currency;
	}

	/**
	 * @return the initial balance of this account.
	 */
	public long getStartBalance() {
		return startBalance;
	};

	public void setCurrency(Currency aCurrency) {
        Currency oldCurrency = currency;
		currency = aCurrency;

		// Notify the change manager.
		processPropertyChange(CurrencyAccountInfo.getCurrencyAccessor(), oldCurrency, aCurrency);

		if (this.currency == aCurrency ||
				(this.currency != null && this.currency.equals(aCurrency)))
				return;
		
		changeSupport.firePropertyChange("currency", oldCurrency, currency);
	}

	/**
	 * Sets the initial balance of this account.
	 * @param s the start balance
	 */
	public void setStartBalance(long s) {
        long oldStartBalance = this.startBalance;
		this.startBalance = s;

		// Notify the change manager.
		processPropertyChange(CurrencyAccountInfo.getStartBalanceAccessor(), new Long(oldStartBalance), new Long(s));
	}

	/**
	 * Adds a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.addPropertyChangeListener(pcl);
	}

	/**
	 * Removes a PropertyChangeListener.
	 * @param pcl a property change listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		changeSupport.removePropertyChangeListener(pcl);
	}

	/**
	 * Sort the entries.
	 */
        // TODO: Sorting the entries affects the view of the data.
        // This sort should be done in the view, not the model, otherwise
        // one view might mess up another view of the data.
        // This must be reviewed if we support SQL databases in any case.
/*	
	public void sortEntries(Comparator c) {
		Collections.sort(entries, c);
	}
*/
	public String toString() {
		return name;
	}

	public String getFullAccountName() {
	    if (getParent() == null) {
		       return name;
		    } else {
		        return getParent().getFullAccountName() + "." + this.name;
		    }
	}

	public int compareTo(Object o) {
		CurrencyAccount a = (CurrencyAccount) o;
		return getName().compareTo(a.getName());
	}

	/**
	 * Required by JMoney.
	 */
	// TODO: remove all of these and instead specify any 'unusual'
	// default values (if required at all) in the property registration.
	static public Object [] getDefaultProperties() {
		return new Object [] { "new account", null, null, null, new Long(0), null, null, null };
	}
	
	/**
	 * Get the balance at a given date
	 * 
	 * @param date
	 * @return the balance
	 * @author Faucheux
	 */
	public long getBalance(Session session, Date fromDate, Date toDate) {
		System.out.println("Calculing the Balance for >" + name + "< (without sub-accounts) between " + fromDate + " and " + toDate);
		
		long bal = getStartBalance();
		Iterator eIt = null;
		
		// Sum each entry the entry between the two dates 
		eIt = entries.iterator();
		while (eIt.hasNext()) {
			Entry e = (Entry) eIt.next();
			if ((e.getTransaction().getDate().compareTo(fromDate) >= 0)
					&& e.getTransaction().getDate().compareTo(toDate) <= 0){
				bal += e.getAmount();
				
			}
		}
		
		return bal;
	}
	
	/**
	 * Get the balance between two dates , inclusive sub-accounts
	 * 
	 * @param date
	 * @return the balance
	 * @author Faucheux
	 */
	public long getBalanceWithSubAccounts(Session session, Date fromDate, Date toDate) {
		System.out.println("Calculing the Balance for >" + name + "< (with sub-accounts) between " + fromDate + " and " + toDate);
		long bal = getBalance(session, fromDate, toDate);
		
		Iterator aIt = getSubAccountIterator();
		
		while (aIt.hasNext()) {
			bal += ((CurrencyAccount) aIt.next()).getBalanceWithSubAccounts(session, fromDate, toDate);
		}
		return bal;
	}
}
