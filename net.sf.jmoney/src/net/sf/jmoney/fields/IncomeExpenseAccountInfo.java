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
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IListGetter;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the IncomeExpenseAccount properties.  By registering
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
 */
public class IncomeExpenseAccountInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<IncomeExpenseAccount> propertySet = PropertySet.addDerivedFinalPropertySet(IncomeExpenseAccount.class, "Income or Expense Category", AccountInfo.getPropertySet(), new IExtendableObjectConstructors<IncomeExpenseAccount>() {

		public IncomeExpenseAccount construct(IObjectKey objectKey,
				IObjectKey parentKey) {
			return new IncomeExpenseAccount(objectKey, parentKey);
		}

		public IncomeExpenseAccount construct(IObjectKey objectKey,
				IObjectKey parentKey, IValues values) {
			return new IncomeExpenseAccount(
					objectKey, 
					parentKey, 
					values.getScalarValue(AccountInfo.getNameAccessor()),
					values.getListManager(objectKey, IncomeExpenseAccountInfo.getSubAccountAccessor()),
					values.getScalarValue(IncomeExpenseAccountInfo.getMultiCurrencyAccessor()),
					values.getReferencedObjectKey(IncomeExpenseAccountInfo.getCurrencyAccessor()),
					values 
			);
		}
	});


	private static ListPropertyAccessor<IncomeExpenseAccount> subAccountAccessor = null;
	private static ScalarPropertyAccessor<Boolean> multiCurrencyAccessor = null;
	private static ScalarPropertyAccessor<Currency> currencyAccessor = null;
	
    public PropertySet registerProperties() {
		IListGetter<IncomeExpenseAccount, IncomeExpenseAccount> accountGetter = new IListGetter<IncomeExpenseAccount, IncomeExpenseAccount>() {
			public ObjectCollection<IncomeExpenseAccount> getList(IncomeExpenseAccount parentObject) {
				return parentObject.getSubAccountCollection();
			}
		};
		
		subAccountAccessor = propertySet.addPropertyList("subAccount", JMoneyPlugin.getResourceString("<not used???>"), IncomeExpenseAccountInfo.getPropertySet(), accountGetter, null);

		multiCurrencyAccessor = propertySet.addProperty("multiCurrency", JMoneyPlugin.getResourceString("AccountPropertiesPanel.multiCurrency"), Boolean.class, 0, 10, new CheckBoxControlFactory(), null); 
		currencyAccessor = propertySet.addProperty("currency", JMoneyPlugin.getResourceString("AccountPropertiesPanel.currency"), Currency.class, 2, 20, new CurrencyControlFactory(), multiCurrencyAccessor.getFalseValueDependency());
		
		// We should define something for the implied enumerated value
		// that is controlled by the derived class type.  This has not
		// been designed yet, so for time being we have nothing to do.
		
		propertySet.setIcon("icons/category.gif");
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<IncomeExpenseAccount> getPropertySet() {
		return propertySet;
	}

    /**
	 * @return
	 */
	public static ListPropertyAccessor<IncomeExpenseAccount> getSubAccountAccessor() {
		return subAccountAccessor;
	}	

    /**
	 * @return
	 */
	public static ScalarPropertyAccessor<Boolean> getMultiCurrencyAccessor() {
		return multiCurrencyAccessor;
	}	

    /**
	 * @return
	 */
	public static ScalarPropertyAccessor<Currency> getCurrencyAccessor() {
		return currencyAccessor;
	}	
}
