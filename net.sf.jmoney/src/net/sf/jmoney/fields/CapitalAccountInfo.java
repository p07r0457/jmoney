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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccountImpl;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IExtensionPropertySetInfo;
import net.sf.jmoney.model2.IPropertyRegistrar;
import net.sf.jmoney.model2.PropertyAccessor;

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
public class CapitalAccountInfo implements IExtensionPropertySetInfo {

	private static PropertyAccessor currencyAccessor = null;
	private static PropertyAccessor bankAccessor = null;
	private static PropertyAccessor accountNumberAccessor = null;
	private static PropertyAccessor startBalanceAccessor = null;
	private static PropertyAccessor minBalanceAccessor = null;
	private static PropertyAccessor abbreviationAccessor = null;
	private static PropertyAccessor commentAccessor = null;

    public CapitalAccountInfo() {
    }

	public Class getImplementationClass() {
		return CapitalAccountImpl.class;
	}

    public Class getInterfaceClass() {
        return CapitalAccount.class;
    }

    public void registerProperties(IPropertyRegistrar propertyRegistrar) {
		// TODO: figure out editors.
		// One Currency, two longs, others are Strings
		
		IPropertyControlFactory textControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					return new TextEditor(parent, 0, propertyAccessor);
				}
		};

		IPropertyControlFactory commentControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					IPropertyControl commentControl = new TextEditor(parent, SWT.MULTI | SWT.WRAP, propertyAccessor);
					GridData gridData = new GridData();
					gridData.verticalAlignment = GridData.FILL;
					gridData.grabExcessVerticalSpace = true;
					gridData.horizontalAlignment = GridData.FILL;
					gridData.grabExcessHorizontalSpace = true;
					commentControl.getControl().setLayoutData(gridData);
					return commentControl;
				}
		};
		
		IPropertyControlFactory amountControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					return new AmountEditor(parent, propertyAccessor);
				}
		};
		
		IPropertyControlFactory currencyControlFactory =
			new IPropertyControlFactory() {
				public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
					return new CurrencyEditor(parent);
				}
		};
		
		propertyRegistrar.addPropertyList("subAccount", JMoneyPlugin.getResourceString("<not used???>"), CapitalAccount.class, null);

		currencyAccessor = propertyRegistrar.addProperty("currency", JMoneyPlugin.getResourceString("AccountPropertiesPanel.currency"), 15.0, currencyControlFactory, null, null);
		bankAccessor = propertyRegistrar.addProperty("bank", JMoneyPlugin.getResourceString("AccountPropertiesPanel.bank"), 30.0, textControlFactory, null, null);
		accountNumberAccessor = propertyRegistrar.addProperty("accountNumber", JMoneyPlugin.getResourceString("AccountPropertiesPanel.accountNumber"), 15.0, textControlFactory, null, null);
		startBalanceAccessor = propertyRegistrar.addProperty("startBalance", JMoneyPlugin.getResourceString("AccountPropertiesPanel.startBalance"), 15.0, amountControlFactory, null, null);
		minBalanceAccessor = propertyRegistrar.addProperty("minBalance", JMoneyPlugin.getResourceString("AccountPropertiesPanel.minBalance"), 15.0, amountControlFactory, null, null);
		abbreviationAccessor = propertyRegistrar.addProperty("abbreviation", JMoneyPlugin.getResourceString("AccountPropertiesPanel.abbrevation"), 30.0, textControlFactory, null, null);
		commentAccessor = propertyRegistrar.addProperty("comment", JMoneyPlugin.getResourceString("AccountPropertiesPanel.comment"), 30.0, commentControlFactory, null, null);
	}

	/**
	 * @return
	 */
	public static PropertyAccessor getCurrencyAccessor() {
		return currencyAccessor;
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
	public static PropertyAccessor getStartBalanceAccessor() {
		return startBalanceAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getMinBalanceAccessor() {
		return minBalanceAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getAbbreviationAccessor() {
		return abbreviationAccessor;
	}	

	/**
	 * @return
	 */
	public static PropertyAccessor getCommentAccessor() {
		return commentAccessor;
	}	
}
