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

import org.eclipse.swt.widgets.Composite;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;

/**
 * @author Nigel
 *
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
 */
public class SessionInfo implements IPropertySetInfo {

	private static PropertySet propertySet = null;
	private static PropertyAccessor commoditiesAccessor = null;
	private static PropertyAccessor accountsAccessor = null;
	private static PropertyAccessor transactionsAccessor = null;

	public SessionInfo() {
    }

	public Class getImplementationClass() {
		return Session.class;
	}
	
	public void registerProperties(PropertySet propertySet, IPropertyRegistrar propertyRegistrar) {
		SessionInfo.propertySet = propertySet;

		IPropertyControlFactory currencyControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					return new CurrencyEditor(parent, propertyAccessor);
				}

				public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
					return extendableObject.getPropertyValue(propertyAccessor).toString();
				}

				public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
					return ((Currency)extendableObject.getPropertyValue(propertyAccessor)).getName();
				}
		};

		commoditiesAccessor = propertyRegistrar.addPropertyList("commodity", JMoneyPlugin.getResourceString("<not used???>"), Commodity.class, null);
		accountsAccessor = propertyRegistrar.addPropertyList("account", JMoneyPlugin.getResourceString("<not used???>"), Account.class, null);
		transactionsAccessor = propertyRegistrar.addPropertyList("transaction", JMoneyPlugin.getResourceString("<not used???>"), Transaction.class, null);
		
		propertyRegistrar.addProperty("defaultCurrency", JMoneyPlugin.getResourceString("Session.defaultCurrency"), 15.0, currencyControlFactory, null, null);
		
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
	public static PropertyAccessor getCommoditiesAccessor() {
		return commoditiesAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getAccountsAccessor() {
		return accountsAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getTransactionsAccessor() {
		return transactionsAccessor;
	}	
}
