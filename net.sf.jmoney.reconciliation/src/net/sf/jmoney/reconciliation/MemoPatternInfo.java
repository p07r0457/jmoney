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

package net.sf.jmoney.reconciliation;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.CurrencyControlFactory;
import net.sf.jmoney.fields.IntegerControlFactory;
import net.sf.jmoney.fields.TextControlFactory;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Pattern properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * 
 * @author Nigel Westbury
 */
public class MemoPatternInfo implements IPropertySetInfo {

	
	private static ExtendablePropertySet<MemoPattern> propertySet = PropertySet.addBaseFinalPropertySet(MemoPattern.class, "Account Import Entry Pattern", new IExtendableObjectConstructors<MemoPattern>() {

		public MemoPattern construct(IObjectKey objectKey, IObjectKey parentKey) {
			return new MemoPattern(objectKey, parentKey);
		}

		public MemoPattern construct(IObjectKey objectKey,
				IObjectKey parentKey, IValues values) {
			return new MemoPattern(
					objectKey, 
					parentKey, 
					values.getScalarValue(MemoPatternInfo.getOrderingIndexAccessor()),
					values.getScalarValue(MemoPatternInfo.getPatternAccessor()),
					values.getScalarValue(MemoPatternInfo.getCheckAccessor()),
					values.getScalarValue(MemoPatternInfo.getDescriptionAccessor()),
					values.getReferencedObjectKey(MemoPatternInfo.getAccountAccessor()),
					values.getScalarValue(MemoPatternInfo.getMemoAccessor()),
					values.getReferencedObjectKey(MemoPatternInfo.getIncomeExpenseCurrencyAccessor()),
					values
			);
		}
	});
	
	private static ScalarPropertyAccessor<Integer> orderingIndexAccessor = null;
	private static ScalarPropertyAccessor<String> patternAccessor = null;
	private static ScalarPropertyAccessor<String> checkAccessor = null;
	private static ScalarPropertyAccessor<String> descriptionAccessor = null;
	private static ScalarPropertyAccessor<Account> accountAccessor = null;
	private static ScalarPropertyAccessor<String> memoAccessor = null;
	private static ScalarPropertyAccessor<Currency> incomeExpenseCurrencyAccessor = null;

	public PropertySet registerProperties() {
		IPropertyControlFactory<Integer> integerControlFactory = new IntegerControlFactory();
		IPropertyControlFactory<String> textControlFactory = new TextControlFactory();
        IPropertyControlFactory<Account> accountControlFactory = new AccountControlFactory<Account>();

        orderingIndexAccessor = propertySet.addProperty("orderingIndex", "Ordering Index",                                    Integer.class,1, 20,  integerControlFactory, null);
		patternAccessor       = propertySet.addProperty("pattern",       "Pattern",                                           String.class, 2, 50,  textControlFactory,    null);
		checkAccessor         = propertySet.addProperty("check",         JMoneyPlugin.getResourceString("Entry.check"),       String.class, 2, 50,  textControlFactory,    null);
		descriptionAccessor   = propertySet.addProperty("description",   JMoneyPlugin.getResourceString("Entry.description"), String.class, 5, 100, textControlFactory,    null);
		accountAccessor       = propertySet.addProperty("account",       JMoneyPlugin.getResourceString("Entry.category"),    Account.class,2, 70,  accountControlFactory, null);
		memoAccessor          = propertySet.addProperty("memo",          JMoneyPlugin.getResourceString("Entry.memo"),        String.class, 5, 100, textControlFactory,    null);
		incomeExpenseCurrencyAccessor = propertySet.addProperty("incomeExpenseCurrency",    JMoneyPlugin.getResourceString("Entry.currency"), Currency.class, 2, 70, new CurrencyControlFactory(), null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<MemoPattern> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Integer> getOrderingIndexAccessor() {
		return orderingIndexAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getPatternAccessor() {
		return patternAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getCheckAccessor() {
		return checkAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getDescriptionAccessor() {
		return descriptionAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Account> getAccountAccessor() {
		return accountAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<String> getMemoAccessor() {
		return memoAccessor;
	}	


	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Currency> getIncomeExpenseCurrencyAccessor() {
		return incomeExpenseCurrencyAccessor;
	}	
}
