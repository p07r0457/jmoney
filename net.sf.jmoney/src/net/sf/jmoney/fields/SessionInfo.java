/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.fields;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Entry properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class SessionInfo implements IPropertySetInfo {

	private static PropertySet<Session> propertySet = null;
	private static ListPropertyAccessor<Commodity> commoditiesAccessor = null;
	private static ListPropertyAccessor<Account> accountsAccessor = null;
	private static ListPropertyAccessor<Transaction> transactionsAccessor = null;
	private static ScalarPropertyAccessor<Currency> defaultCurrencyAccessor = null;

	public SessionInfo() {
    }

	public Class getImplementationClass() {
		return Session.class;
	}
	
	public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		SessionInfo.propertySet = propertyRegistrar.addPropertySet(Session.class);

		IPropertyControlFactory<Currency> currencyControlFactory = new CurrencyControlFactory();

		commoditiesAccessor = propertyRegistrar.addPropertyList("commodity", JMoneyPlugin.getResourceString("<not used???>"), Commodity.class, null);
		accountsAccessor = propertyRegistrar.addPropertyList("account", JMoneyPlugin.getResourceString("<not used???>"), Account.class, null);
		transactionsAccessor = propertyRegistrar.addPropertyList("transaction", JMoneyPlugin.getResourceString("<not used???>"), Transaction.class, null);
		
		defaultCurrencyAccessor = propertyRegistrar.addProperty("defaultCurrency", JMoneyPlugin.getResourceString("Session.defaultCurrency"), Currency.class, 2, 20, currencyControlFactory, null);
		
		propertyRegistrar.setObjectDescription("JMoney Session");
	}

	/**
	 * @return
	 */
	public static PropertySet getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor getCommoditiesAccessor() {
		return commoditiesAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor getAccountsAccessor() {
		return accountsAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor getTransactionsAccessor() {
		return transactionsAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor getDefaultCurrencyAccessor() {
		return defaultCurrencyAccessor;
	}	
}
