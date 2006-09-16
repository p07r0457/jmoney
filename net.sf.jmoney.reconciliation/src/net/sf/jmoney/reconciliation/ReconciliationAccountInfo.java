/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

import net.sf.jmoney.fields.AccountControlFactory;
import net.sf.jmoney.fields.CheckBoxControlFactory;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.PropertySet;

/**
 * Provides the metadata for the extra properties added to each
 * currency account by this plug-in.
 * 
 * @author Nigel Westbury
 */
public class ReconciliationAccountInfo implements IPropertySetInfo {

	private static PropertySet<ReconciliationAccount> propertySet = null;
	private static ScalarPropertyAccessor<Boolean> reconcilableAccessor = null;
	private static ScalarPropertyAccessor<IncomeExpenseAccount> defaultCategoryAccessor = null;
	
	public Class getImplementationClass() {
		return ReconciliationAccount.class;
	}
	
	public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		ReconciliationAccountInfo.propertySet = propertyRegistrar.addPropertySet(ReconciliationAccount.class);

		AccountControlFactory<IncomeExpenseAccount> accountControlFactory = new AccountControlFactory<IncomeExpenseAccount>();
		
		reconcilableAccessor = propertyRegistrar.addProperty("reconcilable", ReconciliationPlugin.getResourceString("Account.isReconcilable"), Boolean.class, 1, 5, new CheckBoxControlFactory(), null);
		defaultCategoryAccessor = propertyRegistrar.addProperty("defaultCategory", ReconciliationPlugin.getResourceString("Account.defaultCategory"), IncomeExpenseAccount.class, 1, 20, accountControlFactory, null);
	}

	/**
	 * @return
	 */
	public static PropertySet<ReconciliationAccount> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Boolean> getReconcilableAccessor() {
		return reconcilableAccessor;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<IncomeExpenseAccount> getDefaultCategoryAccessor() {
		return defaultCategoryAccessor;
	}	
}
