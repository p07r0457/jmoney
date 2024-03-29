/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2010 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.pricehistory;

import java.util.Date;

import net.sf.jmoney.fields.AmountControlFactory;
import net.sf.jmoney.fields.AmountEditor;
import net.sf.jmoney.fields.DateControlFactory;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.IExtendableObjectConstructors;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.IPropertySetInfo;
import net.sf.jmoney.model2.IValues;
import net.sf.jmoney.model2.ListKey;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;

import org.eclipse.swt.widgets.Composite;

/**
 * This class is a listener class to the net.sf.jmoney.fields
 * extension point.  It implements an extension.
 * <P>
 * This extension registers the Price properties.  By registering
 * the properties, every one can know how to display, edit, and store
 * the properties.
 * 
 * @author Nigel Westbury
 */
public class PriceInfo implements IPropertySetInfo {

	
	private static ExtendablePropertySet<Price> propertySet = PropertySet.addBaseFinalPropertySet(Price.class, "Commodity Historical Price", new IExtendableObjectConstructors<Price>() {

		public Price construct(IObjectKey objectKey, ListKey<? super Price> parentKey) {
			return new Price(objectKey, parentKey);
		}

		public Price construct(IObjectKey objectKey,
				ListKey<? super Price> parentKey, IValues values) {
			return new Price(
					objectKey, 
					parentKey, 
					values.getScalarValue(PriceInfo.getDateAccessor()),
					values.getScalarValue(PriceInfo.getPriceAccessor()),
					values
			);
		}
	});
	
	private static ScalarPropertyAccessor<Date> dateAccessor = null;
	private static ScalarPropertyAccessor<Long> priceAccessor = null;

	public PropertySet<Price> registerProperties() {
        IPropertyControlFactory<Date> dateControlFactory = new DateControlFactory();

        IPropertyControlFactory<Long> amountControlFactory = new AmountControlFactory() {
		    @Override	
			protected Commodity getCommodity(ExtendableObject object) {
				// If not enough information has yet been set to determine
				// the currency of the amount in this entry, return
				// the default currency.
	    	    Price price = (Price)object;
	    	    Commodity commodity = (Commodity)price.getParentKey().getObject();
	    	    Currency currency = commodity.getPropertyValue(CommodityPricingInfo.getCurrencyAccessor());
	    	    if (currency == null) {
	    	    	currency = ((Entry) object).getSession().getDefaultCurrency();
	    	    }
	    	    return currency;
			}

			public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<Long> propertyAccessor) {
		    	final AmountEditor editor = new AmountEditor(parent, propertyAccessor, this);
		        
				/*
				 * The format of the amount will change if the currency set in
				 * the commodity object changes.
				 */
		        editor.setListener(new SessionChangeAdapter() {
		        		@Override	
		        		public void objectChanged(ExtendableObject changedObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
		        			Price entry = (Price)editor.getObject();
		        			if (entry == null) {
		        			    return;
		        			}
		        			
		        			Commodity commodity = (Commodity)entry.getParentKey().getObject();
	        				Currency currency = commodity.getPropertyValue(CommodityPricingInfo.getCurrencyAccessor());
	        				// TODO what if the currency is changed to be null?
		        			
		        			// Has the currency property of the parent commodity changed?
		        			if (changedObject == commodity && changedProperty == CommodityPricingInfo.getCurrencyAccessor()) {
								editor.updateCommodity(currency);	
		        			}
		        			// If any property in the currency object changed then
		        			// the format of the amount might also change.
		        			if (changedObject == currency) {
		        				editor.updateCommodity(currency);	
		        			}
		        		}
		        	});   	
		        
		        return editor;
			}};


		dateAccessor  = propertySet.addProperty("date",  "Date",  Date.class,1, 20,  dateControlFactory,   null);
		priceAccessor = propertySet.addProperty("price", "Price", Long.class, 2, 50, amountControlFactory, null);
		
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ExtendablePropertySet<Price> getPropertySet() {
		return propertySet;
	}

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Date> getDateAccessor() {
		return dateAccessor;
	}	

	/**
	 * @return
	 */
	public static ScalarPropertyAccessor<Long> getPriceAccessor() {
		return priceAccessor;
	}	
}
