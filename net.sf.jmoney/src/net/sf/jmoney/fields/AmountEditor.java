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
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeListener;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * Editor class for account properties that
 * are amounts (an amount of a commodity).  These amounts
 * are formatted according to the type of commodity (usually
 * a currency) held in the account.  The format may therefore
 * change if the user selects a different commodity for the
 * account.  This is thus an example of how one property can
 * be affected by another.  We listen for changes to the
 * currency and re-format as necessary.
 * <P>
 * Note that this class has neither get/set methods for the value being edited
 * and no support for property change listeners.  This is
 * because objects of this class are tied to an CapitalAccount object.  
 * Changes to this
 * object are reflected by this object in the CapitalAccount class objects.  
 * Consumers who are interested in changes to the CapitalAccount class objects should
 * add themselves as listeners to the Session object.
 * 
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class AmountEditor implements IPropertyControl {

    private ExtendableObject fObject;

    private Commodity fCommodity;
    
    private ScalarPropertyAccessor<Long> amountPropertyAccessor;
    
    private AmountControlFactory factory;
    
    private Text propertyControl;

    /**
     * Create a new amount editor.
     */
    public AmountEditor(Composite parent, ScalarPropertyAccessor<Long> propertyAccessor, AmountControlFactory factory) {
    	propertyControl = new Text(parent, 0);
    	this.amountPropertyAccessor = propertyAccessor;
    	this.factory = factory;
    }
    
    /**
     * Load the control with the value from the given account.
     */
    public void load(ExtendableObject object) {
        fObject = object;
        
    	if (object == null) {
            propertyControl.setText("");
    	} else {
            fCommodity = factory.getCommodity(fObject);
    		
    		// Some amounts may be of type Long, not long, so that 
    		// they can be null, so we must get the property
    		// value as a Long.
    		Long amount = (Long) fObject.getPropertyValue(amountPropertyAccessor);
    		if (amount == null) {
    			propertyControl.setText("");
    		} else {
    			propertyControl.setText(fCommodity.format(amount.longValue()));
    		}
    	}
    	propertyControl.setEnabled(object != null);
    }
    
    /**
     * Set a listener that listens for changes to properties
     * that affect the display of the amount.
     * <P>
     * The format of amounts depends on the currency being
     * represented by the amount.  If it is possible that the
     * commodity may change while this control exists then a
     * listener must be set using this method.  The listener
     * must call the updateCommodity method in this amount editor
     * whenever the commodity changes.
     * <P>
     * This method must not be called more than once.
     * This class takes responsibility for adding and
     * removing the listener to/from the session.
     *  
     * @param commodityChangeListener
     */
    public void setListener(final SessionChangeListener commodityChangeListener) {
		// We must listen for changes to the currency so that
		// we can change the format of the amount.

    	// Note that we cannot get the session from fObject because fObject
		// may be null.
		JMoneyPlugin.getDefault().getSessionManager().addSessionChangeListener(commodityChangeListener, propertyControl);
    }
    
    public void updateCommodity(Commodity newCommodity) {
    	// Get the current text from the control and try to re-format
    	// it for the new currency.
    	// However, if the property can take null values and the control
    	// contains the empty string then set the amount to null.
    	// (The currency amount parser returns a zero amount for the
    	// empty string).
    	// Amounts can be represented by 'Long' or by 'long'.
    	// 'Long' values can be null, 'long' values cannot be null.
    	// If the text in the control now translates to a different long
    	// value as a result of the new currency, update the new long value
    	// in the datastore.
    	
    	// It is probably not necessary for us to set the control text here,
    	// because this will be done by our listener if we are changing
    	// the amount.  However, to protect against a future possibility
    	// that a currency change may change the format without changing the amount,
    	// we set the control text ourselves first.
    	
    	String amountString = propertyControl.getText();
    	if (!amountString.equals("")) {
    		long amount = newCommodity.parse(amountString);
    		propertyControl.setText(newCommodity.format(amount));
    		fObject.setPropertyValue(amountPropertyAccessor, amount);
    	}
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
        if (amountString.length() == 0 && amountPropertyAccessor.getClassOfValueObject() == Long.class) {
            // The text box is empty and the property is Long
            // (not long) thus allowing nulls.  Therefore
            // we set the property value to be null.
            fObject.setPropertyValue(amountPropertyAccessor, null);
        } else {
            long amount = fCommodity.parse(amountString);
            fObject.setPropertyValue(amountPropertyAccessor, amount);
        }
    }

	/**
	 * @return The object containing the property being edited.
	 */
    public ExtendableObject getObject() {
		return fObject;
	}
    
	/**
	 * @return The underlying SWT widget.
	 */
	public Control getControl() {
		return propertyControl;
	}
}
