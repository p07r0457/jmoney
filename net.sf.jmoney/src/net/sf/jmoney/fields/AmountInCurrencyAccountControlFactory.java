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

import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.CurrencyAccountInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;

import org.eclipse.swt.widgets.Composite;

/**
 * A control factory to edit an amount of a commodity.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class AmountInCurrencyAccountControlFactory extends AmountControlFactory {

    public IPropertyControl createPropertyControl(Composite parent, ScalarPropertyAccessor<Long> propertyAccessor) {
    	final AmountEditor editor = new AmountEditor(parent, propertyAccessor, this);
        
        editor.setListener(new SessionChangeAdapter() {
		    @Override	
        	public void objectChanged(ExtendableObject extendableObject, ScalarPropertyAccessor changedProperty, Object oldValue, Object newValue) {
        			if (extendableObject.equals(editor.getObject()) && changedProperty == CurrencyAccountInfo.getCurrencyAccessor()) {
        				editor.updateCommodity((Commodity)newValue);	
        			}
        		}
        	});   	
        
        return editor;
    }

    @Override	
    protected Commodity getCommodity(ExtendableObject object) {
    	return ((CurrencyAccount) object).getCurrency();
    }

    @Override	
	public boolean isEditable() {
		return true;
	}
}