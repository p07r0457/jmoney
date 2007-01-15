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

import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;

import org.eclipse.swt.widgets.Composite;

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
public class EntryInfo implements IPropertySetInfo {

	private static ExtendablePropertySet<Entry> propertySet = PropertySet.addBasePropertySet(Entry.class, "Accounting Entry");
	
	private static ScalarPropertyAccessor<String> checkAccessor = null;
	private static ScalarPropertyAccessor<String> descriptionAccessor = null;
	private static ScalarPropertyAccessor<Account> accountAccessor = null;
	private static ScalarPropertyAccessor<Date> valutaAccessor = null;
	private static ScalarPropertyAccessor<String> memoAccessor = null;
	private static ScalarPropertyAccessor<Long> amountAccessor = null;
	private static ScalarPropertyAccessor<Long> creationAccessor = null;
	private static ScalarPropertyAccessor<Currency> incomeExpenseCurrencyAccessor = null;

	public PropertySet registerProperties() {
		IPropertyControlFactory<String> textControlFactory = new TextControlFactory();
        IPropertyControlFactory<Date> dateControlFactory = new DateControlFactory();
        IPropertyControlFactory<Account> accountControlFactory = new AccountControlFactory<Account>();

        IPropertyControlFactory<Long> amountControlFactory = new AmountControlFactory() {

			protected Commodity getCommodity(ExtendableObject object) {
	    	    return ((Entry) object).getCommodity();
			}

			public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<Long> propertyAccessor, Session session) {
		    	final AmountEditor editor = new AmountEditor(parent, propertyAccessor, this);
		        
		    	// The format of the amount will change if either
		    	// the account property of the entry changes or if
		    	// the commodity property of the account changes.
		        editor.setListener(new SessionChangeAdapter() {
		        		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
		        			Entry entry = (Entry)editor.getObject();
		        			if (entry == null) {
		        			    return;
		        			}
		        			// Has the account property changed?
		        			if (changedObject.equals(entry) && changedProperty == EntryInfo.getAccountAccessor()) {
		        				// FIXME I don't know how to get the commodity for an
		        				// income and expense account.
		        				//editor.updateCommodity(entry.getCommodity());	
		        			}
		        			// Has the commodity property of the account changed?
		        			if (changedObject ==  entry.getAccount() && changedProperty == CurrencyAccountInfo.getCurrencyAccessor()) {
		        				editor.updateCommodity(entry.getCommodity());	
		        			}
		        			// If any property in the commodity object changed then
		        			// the format of the amount might also change.

	        				// FIXME I don't know how to get the commodity for an
	        				// income and expense account.
		        			//if (changedObject ==  entry.getCommodity()) {
		        			//	  editor.updateCommodity(entry.getCommodity());	
		        			//}
		        			
		        			// TODO: All the above tests are still not complete.
		        			// If the account for the entry can contain multiple
		        			// commodities then the commodity may depend on properties
		        			// in the entry object.  We really need a special listener
		        			// that listens for any changes that would affect the
		        			// Entry.getCommodity() value.
		        		}
		        	});   	
		        
		        return editor;
			}};

		IPropertyControlFactory<Long> creationControlFactory = new PropertyControlFactory<Long>() {

			public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<Long> propertyAccessor, Session session) {
				// This property is not editable
		    	return null;
			}

			public Long getDefaultValue() {
				return 0L;
			}

			public boolean isEditable() {
				return false;
			}
		};

		checkAccessor       = propertySet.addProperty("check",       JMoneyPlugin.getResourceString("Entry.check"),       String.class, 2, 50,  textControlFactory, null);
		descriptionAccessor = propertySet.addProperty("description", JMoneyPlugin.getResourceString("Entry.description"), String.class, 5, 100, textControlFactory, null);
		accountAccessor     = propertySet.addProperty("account",     JMoneyPlugin.getResourceString("Entry.category"),    Account.class, 2, 70,  accountControlFactory, null);
		valutaAccessor      = propertySet.addProperty("valuta",      JMoneyPlugin.getResourceString("Entry.valuta"),      Date.class, 0, 76,  dateControlFactory, null);
		memoAccessor        = propertySet.addProperty("memo",        JMoneyPlugin.getResourceString("Entry.memo"),        String.class, 5, 100, textControlFactory, null);
		amountAccessor      = propertySet.addProperty("amount",      JMoneyPlugin.getResourceString("Entry.amount"),      Long.class, 2, 70,  amountControlFactory, null);
		creationAccessor    = propertySet.addProperty("creation",    JMoneyPlugin.getResourceString("Entry.creation"),    Long.class, 0, 70,  creationControlFactory, null);
		incomeExpenseCurrencyAccessor = propertySet.addProperty("incomeExpenseCurrency",    JMoneyPlugin.getResourceString("Entry.currency"),    Currency.class, 2, 70, new CurrencyControlFactory(), null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Entry> getPropertySet() {
		return propertySet;
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
	public static ScalarPropertyAccessor<Date> getValutaAccessor() {
		return valutaAccessor;
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
	public static ScalarPropertyAccessor<Long> getAmountAccessor() {
		return amountAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long> getCreationAccessor() {
		return creationAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Currency> getIncomeExpenseCurrencyAccessor() {
		return incomeExpenseCurrencyAccessor;
	}	
}
