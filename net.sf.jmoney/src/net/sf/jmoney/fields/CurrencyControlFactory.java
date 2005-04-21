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

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyControlFactory;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.Session;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * A control factory to select a currency.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CurrencyControlFactory implements IPropertyControlFactory {

    private Vector usedCurrencies;
    
    public IPropertyControl createPropertyControl(Composite parent, PropertyAccessor propertyAccessor) {
        return new CurrencyEditor(parent, propertyAccessor);
    }

	public CellEditor createCellEditor(Table table) {
        Session session = JMoneyPlugin.getDefault().getSession();

        // Load all the currencies the user uses
        usedCurrencies = new Vector();
        for (Iterator iter = session.getCommodityCollection().iterator(); iter.hasNext();) {
            Commodity commodity = (Commodity) iter.next();
            if (commodity instanceof Currency) {
                usedCurrencies.add(commodity);
            }
        }
        
        // Extract their names
        String currencyNames[] = new String[usedCurrencies.size()];
        for (int i = 0; i<usedCurrencies.size(); i++)
            currencyNames[i] = ((Currency) usedCurrencies.get(i)).getName();

		return new ComboBoxCellEditor(table, currencyNames);
	}

	/*
	 * @author Faucheux
	 */
	public Object getValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        
	    Currency currency = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        if (currency == null) return new Integer(0); 
        
        // Search the index of the choosen entry
        int index = 0;
        while (index<usedCurrencies.size() && usedCurrencies.get(index) != currency  ) 
            index++;
        
		return new Integer(index);
	}

	/*
	 * @author Faucheux
	 */
	public void setValueTypedForCellEditor(ExtendableObject extendableObject, PropertyAccessor propertyAccessor, Object value) {
		int index = ((Integer)value).intValue();
		if (index > usedCurrencies.size()) 
			if (JMoneyPlugin.DEBUG) System.out.println("Can't find the " + index + " currency of " + usedCurrencies.size());
		else { 
		    Currency currency = (Currency) usedCurrencies.get(index);
        	extendableObject.setPropertyValue(propertyAccessor, currency);
		}
	}

    public String formatValueForMessage(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Currency value = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "none" : "'" + value.getName() + "'";
    }

    public String formatValueForTable(ExtendableObject extendableObject, PropertyAccessor propertyAccessor) {
        Currency value = (Currency) extendableObject.getPropertyValue(propertyAccessor);
        return value == null ? "" : value.getCode();
    }

	public boolean isEditable() {
		return true;
	}
}