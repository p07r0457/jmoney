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
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * @author Nigel
 *
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the CapitalAccount properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * <P>
 * These properties are supported in the JMoney base code, so everyone
 * including plug-ins will know about these properties.  However, to
 * follow the Eclipse paradigm (every one should be treated equal,
 * including oneself), these are registered through the same extension
 * point that plug-ins must also use to register their properties.
 */
public class BankAccountInfo implements IPropertySetInfo {

	private static PropertySet propertySet = null;
	private static PropertyAccessor bankAccessor = null;
	private static PropertyAccessor accountNumberAccessor = null;
	private static PropertyAccessor minBalanceAccessor = null;

    public BankAccountInfo() {
    }

	public Class getImplementationClass() {
		return BankAccount.class;
	}

    public void registerProperties(PropertySet propertySet, IPropertyRegistrar propertyRegistrar) {
		BankAccountInfo.propertySet = propertySet;
		
		IPropertyControlFactory textControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					return new TextEditor(parent, 0, propertyAccessor);
				}

				public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
					String value = extendableObject.getStringPropertyValue(propertyAccessor);
					if (value == null || value.length() == 0) {
						return "empty";
					} else {
						return "'" + value + "'";
					}
				}

				public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
					return extendableObject.getStringPropertyValue(propertyAccessor);
				}
		};

		IPropertyControlFactory amountControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					return new AmountEditor(parent, propertyAccessor);
				}

				public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
					Long amount = (Long)extendableObject.getPropertyValue(propertyAccessor);
					if (amount == null) {
						return "none"; 
					} else {
						Currency currency = ((BankAccount)extendableObject).getCurrency();
						return currency.format(amount.longValue());
					}
				}

				public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
					Long amount = (Long)extendableObject.getPropertyValue(propertyAccessor);
					if (amount == null) {
						return ""; 
					} else {
						Currency currency = ((BankAccount)extendableObject).getCurrency();
						return currency.format(amount.longValue());
					}
				}
		};
		
		bankAccessor = propertyRegistrar.addProperty("bank", JMoneyPlugin.getResourceString("AccountPropertiesPanel.bank"), 30.0, textControlFactory, null, null);
		accountNumberAccessor = propertyRegistrar.addProperty("accountNumber", JMoneyPlugin.getResourceString("AccountPropertiesPanel.accountNumber"), 15.0, textControlFactory, null, null);
		minBalanceAccessor = propertyRegistrar.addProperty("minBalance", JMoneyPlugin.getResourceString("AccountPropertiesPanel.minBalance"), 15.0, amountControlFactory, null, null);
		
		propertyRegistrar.setObjectDescription("Bank Account");
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
	public static PropertyAccessor getBankAccessor() {
		return bankAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getAccountNumberAccessor() {
		return accountNumberAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getMinBalanceAccessor() {
		return minBalanceAccessor;
	}	
}
