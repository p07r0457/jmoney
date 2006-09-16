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
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
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
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class BankAccountInfo implements IPropertySetInfo {

	private static PropertySet<BankAccount> propertySet = null;
	private static ScalarPropertyAccessor<String> bankAccessor = null;
	private static ScalarPropertyAccessor<String> accountNumberAccessor = null;
	private static ScalarPropertyAccessor<Long> minBalanceAccessor = null;

    public BankAccountInfo() {
    }

	public Class getImplementationClass() {
		return BankAccount.class;
	}

    public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		BankAccountInfo.propertySet = propertyRegistrar.addPropertySet(BankAccount.class);
		
		IPropertyControlFactory<String> textControlFactory = new TextControlFactory();
		IPropertyControlFactory<Long> amountControlFactory = new AmountInCurrencyAccountControlFactory();
		
		bankAccessor          = propertyRegistrar.addProperty("bank", JMoneyPlugin.getResourceString("AccountPropertiesPanel.bank"), String.class, 5, 100, textControlFactory, null); //$NON-NLS-1$ //$NON-NLS-2$
		accountNumberAccessor = propertyRegistrar.addProperty("accountNumber", JMoneyPlugin.getResourceString("AccountPropertiesPanel.accountNumber"), String.class, 2, 70, textControlFactory, null); //$NON-NLS-1$ //$NON-NLS-2$
		minBalanceAccessor    = propertyRegistrar.addProperty("minBalance", JMoneyPlugin.getResourceString("AccountPropertiesPanel.minBalance"), Long.class, 2, 40, amountControlFactory, null); //$NON-NLS-1$ //$NON-NLS-2$
		
		propertyRegistrar.setObjectDescription(JMoneyPlugin.getResourceString("AccountPropertiesPanel.ObjectDescription")); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	public static PropertySet<BankAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getBankAccessor() {
		return bankAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getAccountNumberAccessor() {
		return accountNumberAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long> getMinBalanceAccessor() {
		return minBalanceAccessor;
	}	
}
