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
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

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
public class CurrencyAccountInfo implements IPropertySetInfo {

	private static PropertySet<CurrencyAccount> propertySet = null;
	private static ScalarPropertyAccessor<Currency> currencyAccessor = null;
	private static ScalarPropertyAccessor<Long> startBalanceAccessor = null;

    public CurrencyAccountInfo() {
    }

	public Class getImplementationClass() {
		return CurrencyAccount.class;
	}

    public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		propertySet = propertyRegistrar.addPropertySet(CurrencyAccount.class);
		
		IPropertyControlFactory<Long> amountControlFactory = new AmountInCurrencyAccountControlFactory();
		IPropertyControlFactory<Currency> currencyControlFactory = new CurrencyControlFactory();
		
		currencyAccessor = propertyRegistrar.addProperty("currency", JMoneyPlugin.getResourceString("AccountPropertiesPanel.currency"), Currency.class, 3, 30, currencyControlFactory, null);
		startBalanceAccessor = propertyRegistrar.addProperty("startBalance", JMoneyPlugin.getResourceString("AccountPropertiesPanel.startBalance"), Long.class, 2, 40, amountControlFactory, null);

		propertyRegistrar.setDerivableInfo();
	}

	/**
	 * @return
	 */
	public static PropertySet<CurrencyAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Currency> getCurrencyAccessor() {
		return currencyAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long> getStartBalanceAccessor() {
		return startBalanceAccessor;
	}	
}
