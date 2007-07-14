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
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.ListKey;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
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

	private static ExtendablePropertySet<Session> propertySet = PropertySet.addBaseFinalPropertySet(Session.class, "JMoney Session", new IExtendableObjectConstructors<Session>() {

		public Session construct(IObjectKey objectKey, ListKey parentKey) {
			return new Session(objectKey, parentKey);
		}

		public Session construct(IObjectKey objectKey,
				ListKey parentKey, IValues values) {
			return new Session(
					objectKey, 
					parentKey, 
					values.getListManager(objectKey, SessionInfo.getCommoditiesAccessor()),
					values.getListManager(objectKey, SessionInfo.getAccountsAccessor()),
					values.getListManager(objectKey, SessionInfo.getTransactionsAccessor()),
					values.getReferencedObjectKey(SessionInfo.getDefaultCurrencyAccessor()),
					values
			);
		}
	});

	
	private static ListPropertyAccessor<Commodity> commoditiesAccessor = null;
	private static ListPropertyAccessor<Account> accountsAccessor = null;
	private static ListPropertyAccessor<Transaction> transactionsAccessor = null;
	private static ScalarPropertyAccessor<Currency> defaultCurrencyAccessor = null;

	public PropertySet registerProperties() {
		IListGetter<Session, Commodity> commodityGetter = new IListGetter<Session, Commodity>() {
			public ObjectCollection<Commodity> getList(Session parentObject) {
				return parentObject.getCommodityCollection();
			}
		};
		
		IListGetter<Session, Account> accountGetter = new IListGetter<Session, Account>() {
			public ObjectCollection<Account> getList(Session parentObject) {
				return parentObject.getAccountCollection();
			}
		};
		
		IListGetter<Session, Transaction> transactionGetter = new IListGetter<Session, Transaction>() {
			public ObjectCollection<Transaction> getList(Session parentObject) {
				return parentObject.getTransactionCollection();
			}
		};
		
		IPropertyControlFactory<Currency> currencyControlFactory = new CurrencyControlFactory();

		commoditiesAccessor = propertySet.addPropertyList("commodity", JMoneyPlugin.getResourceString("<not used???>"), CommodityInfo.getPropertySet(), commodityGetter);
		accountsAccessor = propertySet.addPropertyList("account", JMoneyPlugin.getResourceString("<not used???>"), AccountInfo.getPropertySet(), accountGetter);
		transactionsAccessor = propertySet.addPropertyList("transaction", JMoneyPlugin.getResourceString("<not used???>"), TransactionInfo.getPropertySet(), transactionGetter);
		
		defaultCurrencyAccessor = propertySet.addProperty("defaultCurrency", JMoneyPlugin.getResourceString("Session.defaultCurrency"), Currency.class, 2, 20, currencyControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Session> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Commodity> getCommoditiesAccessor() {
		return commoditiesAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Account> getAccountsAccessor() {
		return accountsAccessor;
	}	

	/**
	 * @return
	 */
	public static ListPropertyAccessor<Transaction> getTransactionsAccessor() {
		return transactionsAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Currency> getDefaultCurrencyAccessor() {
		return defaultCurrencyAccessor;
	}	
}
