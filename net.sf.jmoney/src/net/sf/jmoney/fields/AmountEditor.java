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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Composite;

/**
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an CapitalAccount object.  
 * Changes to this
 * object are reflected by this object in the CapitalAccount class objects.  
 * Consumers who are interested in changes to the CapitalAccount class objects should
 * add themselves as listeners to the appropriate PropertyAccessor object.
 * <P>
 * This class is the editor class for account properties that
 * are amounts.  (An amount of a commodity).  These amounts
 * are formatted according to the type of commodity (usually
 * a currency) held in the account.  The format may therefore
 * change if the user selects a different commodity for the
 * account.  This is thus an example of how one property can
 * be affected by another.  We listen for changes to the
 * currency and re-format as necessary.
 * 
 * @author  Nigel
 */
public class AmountEditor implements IPropertyControl {
    
    private CapitalAccount account = null;
    
    private PropertyAccessor propertyAccessor;
    
    private Text propertyControl;

    /** Creates new CurrencyEditor */
    public AmountEditor(Composite parent, PropertyAccessor propertyAccessor) {
    	propertyControl = new Text(parent, 0);
    	this.propertyAccessor = propertyAccessor;
    }
    
    /**
     * Load the control with the value from the given account.
     */
    public void load(Object object) {
    	account = (CapitalAccount)object;
    	
		Currency currency = account.getCurrency();
		
		// Some amounts may be of type Long, not long, so that 
		// they can be null, so we must get the property
		// value as a Long.
		Long amount = (Long)account.getPropertyValue(propertyAccessor);
		if (amount == null) {
			propertyControl.setText("");
		} else {
			propertyControl.setText(currency.format(amount.longValue()));
		}

		// We must listen for changes to the currency so that
		// we can change the format of the amount.
		account.addPropertyChangeListener(
			new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getPropertyName().equals("currency")) {
						// Get the current text from the control and try to re-format
						// it for the new currency.
						// However, if the property can take null values and the control
						// contains the empty string then set the amount to null.
						// (The currency amount parser returns a zero amount for the
						// empty string).
						// Amounts can be represented by 'Long' or by 'long'.
						// 'Long' values can be null, 'long' values cannot be null.
						
						String amountString = propertyControl.getText();
						if (amountString.equals("") && propertyAccessor.getValueClass() == long.class) {
							account.setPropertyValue(propertyAccessor, null);
						} else {
							Currency currency = account.getCurrency();
							long amount = currency.parse(amountString);
							account.setLongPropertyValue(propertyAccessor, amount);
							propertyControl.setText(currency.format(amount));
						}
					}
				}
			});

		propertyControl.setEnabled(true);
    }
    
    /**
     * Load the control with the value from the given account.
     */
    public void loadDisabled(Object object) {
    	CapitalAccount nonMutableAccount = (CapitalAccount)object;
    	account = null;
    	
		Currency currency = nonMutableAccount.getCurrency();
		long amount = nonMutableAccount.getLongPropertyValue(propertyAccessor);
		propertyControl.setText(currency.format(amount));
		
		propertyControl.setEnabled(false);
    }
    
    /**
     * Save the value from the control back into the account object.
     *
     * Editors may update the property on a regular basis, not just when
     * the framework calls the <code>save</code> method.  However, the only time
     * that editors must update the property is when the framework calls this method.
     *
     * The framework should never call this method when no account is selected
     * so we can assume that <code>account</code> is not null.
     */
    public void save() {
    	String amountString = propertyControl.getText();
    	if (amountString.length() == 0
    			&& propertyAccessor.getValueClass() == Long.class) {
    		// The text box is empty and the property is Long
    		// (not long) thus allowing nulls.  Therefore
    		// we set the property value to be null.
    		account.setPropertyValue(propertyAccessor, null);
    	} else {
    		Currency currency = account.getCurrency();
    		long amount = currency.parse(amountString);
    		account.setLongPropertyValue(propertyAccessor, amount);
    	}
    }

	/* (non-Javadoc)
	 * @see net.sf.jmoney.model2.IPropertyControl#getControl()
	 */
	public Control getControl() {
		return propertyControl;
	}
}
